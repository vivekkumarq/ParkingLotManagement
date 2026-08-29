# syntax=docker/dockerfile:1
#
# Multi-stage build for the parkinglotmanagement-application module.
#
#   docker build -t parkinglotmanagement .
#   docker run --rm -p 8083:8083 --env-file .env parkinglotmanagement
#
# NOTE: Docker was not available in the environment this file was written in, so
# it has never been built or run. Treat it as reviewed-but-unverified.

# ---------------------------------------------------------------------------
# Stage 1 - build the reactor and produce the executable jar.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

# The wrapper and the POMs first, so the dependency layer is cached and only
# re-resolved when a POM actually changes.
COPY mvnw mvnw.cmd ./
COPY .mvn/ .mvn/
COPY pom.xml ./
COPY parkinglotmanagement-api/pom.xml           parkinglotmanagement-api/
COPY parkinglotmanagement-database/pom.xml      parkinglotmanagement-database/
COPY parkinglotmanagement-impl/pom.xml          parkinglotmanagement-impl/
COPY parkinglotmanagement-web/pom.xml           parkinglotmanagement-web/
COPY parkinglotmanagement-application/pom.xml   parkinglotmanagement-application/

RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline -DskipTests

COPY parkinglotmanagement-api/src           parkinglotmanagement-api/src
COPY parkinglotmanagement-database/src      parkinglotmanagement-database/src
COPY parkinglotmanagement-impl/src          parkinglotmanagement-impl/src
COPY parkinglotmanagement-web/src           parkinglotmanagement-web/src
COPY parkinglotmanagement-application/src   parkinglotmanagement-application/src

# jOOQ code generation runs here. It reads the JPA entity model, not a database,
# so the build needs no network service.
RUN ./mvnw -B -ntp clean package -DskipTests

# ---------------------------------------------------------------------------
# Stage 2 - runtime. JRE only, no Maven, no sources.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy AS runtime

# curl is used by the healthcheck below.
RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/*

# Run as an unprivileged user; the application never writes to disk.
RUN groupadd --system --gid 1001 parking \
    && useradd --system --uid 1001 --gid parking --no-create-home parking

WORKDIR /app
COPY --from=build --chown=parking:parking \
     /workspace/parkinglotmanagement-application/target/parkinglotmanagement.jar app.jar

USER parking

EXPOSE 8083

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport" \
    SERVER_PORT=8083

# Actuator's health endpoint is exposed by default in application.properties.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

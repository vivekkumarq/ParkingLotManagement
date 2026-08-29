package com.netcracker.parkinglotmanagement.config;

import org.jooq.conf.RenderNameCase;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * jOOQ rendering settings, picked up by Spring Boot's {@code JooqAutoConfiguration}.
 *
 * <p>The generated classes carry identifiers in upper case because they are derived
 * from the JPA model through H2, which folds unquoted names upwards. PostgreSQL folds
 * them <em>downwards</em>, so emitting {@code "ID"} quoted would look for a column
 * that does not exist. Rendering every name unquoted and in lower case is correct on
 * both: PostgreSQL matches directly and H2 folds back up.
 */
@Configuration
public class JooqSettingsConfiguration {

    @Bean
    public Settings jooqSettings() {
        return new Settings()
                .withRenderNameCase(RenderNameCase.LOWER)
                .withRenderQuotedNames(RenderQuotedNames.NEVER)
                .withReturnAllOnUpdatableRecord(true);
    }
}

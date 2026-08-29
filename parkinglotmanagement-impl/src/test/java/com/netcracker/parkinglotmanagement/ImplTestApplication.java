package com.netcracker.parkinglotmanagement;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * Boots just enough of Spring for the repository integration tests: a DataSource,
 * Flyway, a DSLContext and the beans in {@code com.netcracker.parkinglotmanagement}.
 *
 * <p>The impl module has no application class of its own - the runnable one lives
 * in parkinglotmanagement-application - so the tests supply this one. JPA is
 * excluded for the same reason it is excluded in production: nothing queries
 * through Hibernate, and letting it near the schema would undermine Flyway.
 */
@SpringBootApplication(
        scanBasePackages = "com.netcracker.parkinglotmanagement",
        exclude = HibernateJpaAutoConfiguration.class)
public class ImplTestApplication {
}

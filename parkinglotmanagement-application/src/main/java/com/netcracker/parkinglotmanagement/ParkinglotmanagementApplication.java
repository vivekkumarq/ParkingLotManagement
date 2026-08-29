package com.netcracker.parkinglotmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point.
 *
 * <p>{@link HibernateJpaAutoConfiguration} is excluded on purpose. The JPA entity
 * model in {@code parkinglotmanagement-api} exists to feed jOOQ code generation at
 * build time, not to be persisted through at run time - every query in this
 * application goes through jOOQ. Leaving Hibernate switched on would start a second
 * ORM that nobody calls and, with the {@code spring.jpa.hibernate.ddl-auto=update}
 * the project used to carry, would let it alter the schema behind Flyway's back.
 *
 * <p>With JPA out of the way Spring Boot supplies a plain
 * {@code DataSourceTransactionManager}, which is what the {@code @Transactional}
 * service methods and jOOQ share.
 */
@SpringBootApplication(exclude = HibernateJpaAutoConfiguration.class)
@EnableScheduling
public class ParkinglotmanagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParkinglotmanagementApplication.class, args);
    }
}

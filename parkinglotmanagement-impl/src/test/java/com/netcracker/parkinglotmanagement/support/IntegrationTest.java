package com.netcracker.parkinglotmanagement.support;

import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test that runs against the in-memory H2 database with the real Flyway
 * migrations applied.
 *
 * <p>Spring caches the context across every test carrying this annotation, so the
 * schema is migrated once per build rather than once per class. Isolation comes
 * from {@link DatabaseFixture#clean()} instead of from rebuilding the context,
 * which is both faster and closer to how the application really runs.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
public @interface IntegrationTest {
}

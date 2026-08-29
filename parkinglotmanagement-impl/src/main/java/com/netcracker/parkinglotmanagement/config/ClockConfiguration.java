package com.netcracker.parkinglotmanagement.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Supplies the clock the services read "now" from.
 *
 * <p>Injecting a {@link Clock} rather than calling {@code LocalDateTime.now()}
 * directly is what makes the fee, reservation-expiry and occupancy logic testable:
 * a test substitutes {@link Clock#fixed} and gets deterministic timestamps without
 * sleeping or mocking static methods.
 */
@Configuration
public class ClockConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}

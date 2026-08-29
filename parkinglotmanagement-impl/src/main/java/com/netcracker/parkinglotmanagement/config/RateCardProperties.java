package com.netcracker.parkinglotmanagement.config;

import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * The tariff, bound from {@code parking.rate-card.*}.
 *
 * <p>The defaults below make the application runnable with no configuration at all;
 * anything set in a properties file or an environment variable overrides them.
 *
 * <pre>
 * parking.rate-card.currency=INR
 * parking.rate-card.grace-period-minutes=15
 * parking.rate-card.overstay-penalty-per-hour=50.00
 * parking.rate-card.rates.CAR.first-hour=50.00
 * parking.rate-card.rates.CAR.per-additional-hour=30.00
 * parking.rate-card.rates.CAR.daily-cap=400.00
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "parking.rate-card")
@Data
public class RateCardProperties {

    /** ISO currency code, echoed in fee breakdowns and revenue reports. */
    private String currency = "INR";

    /** A stay no longer than this is free. */
    private int gracePeriodMinutes = 15;

    /** Charged per started hour a reservation-backed stay runs past its window. */
    private BigDecimal overstayPenaltyPerHour = new BigDecimal("50.00");

    private Map<VehicleType, Rate> rates = defaultRates();

    /**
     * @throws IllegalStateException when no tariff is configured for the vehicle type
     */
    public Rate rateFor(VehicleType vehicleType) {
        Rate rate = rates.get(vehicleType);
        if (rate == null) {
            throw new IllegalStateException(
                    "No rate card entry configured for vehicle type " + vehicleType
                            + "; set parking.rate-card.rates." + vehicleType + ".*");
        }
        return rate;
    }

    private static Map<VehicleType, Rate> defaultRates() {
        Map<VehicleType, Rate> defaults = new EnumMap<>(VehicleType.class);
        defaults.put(VehicleType.MOTORCYCLE,
                new Rate(new BigDecimal("20.00"), new BigDecimal("10.00"), new BigDecimal("100.00")));
        defaults.put(VehicleType.CAR,
                new Rate(new BigDecimal("50.00"), new BigDecimal("30.00"), new BigDecimal("400.00")));
        defaults.put(VehicleType.TRUCK,
                new Rate(new BigDecimal("100.00"), new BigDecimal("60.00"), new BigDecimal("800.00")));
        return defaults;
    }

    /** Tariff for a single vehicle type. */
    @Data
    public static class Rate {

        /** Charged as soon as the grace period is exceeded. */
        private BigDecimal firstHour;

        /** Charged for every started hour after the first. */
        private BigDecimal perAdditionalHour;

        /** Ceiling for any 24-hour period; a full day never costs more than this. */
        private BigDecimal dailyCap;

        public Rate() {
        }

        public Rate(BigDecimal firstHour, BigDecimal perAdditionalHour, BigDecimal dailyCap) {
            this.firstHour = firstHour;
            this.perAdditionalHour = perAdditionalHour;
            this.dailyCap = dailyCap;
        }
    }
}

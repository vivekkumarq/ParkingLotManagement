package com.netcracker.parkinglotmanagement.api.domain;

/**
 * The kinds of vehicle the car park accepts. A slot is cut for exactly one of
 * these, and the rate card is keyed by it.
 */
public enum VehicleType {

    MOTORCYCLE,
    CAR,
    TRUCK;

    /**
     * Parses a persisted or request-supplied value, tolerating case and surrounding
     * whitespace.
     *
     * @throws IllegalArgumentException if the value is not a known vehicle type
     */
    public static VehicleType from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("vehicleType must not be null");
        }
        return VehicleType.valueOf(value.trim().toUpperCase());
    }
}

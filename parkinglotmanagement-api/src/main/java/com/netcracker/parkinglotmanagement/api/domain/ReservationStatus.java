package com.netcracker.parkinglotmanagement.api.domain;

/** Lifecycle of a slot reservation. */
public enum ReservationStatus {

    /** Booked and holding its slot; the window has not been claimed yet. */
    BOOKED,

    /** The vehicle arrived and the booking was turned into a parking slip. */
    CLAIMED,

    /** Withdrawn by the customer before the window started. */
    CANCELLED,

    /** The window elapsed without the vehicle arriving; the slot was released. */
    EXPIRED;

    public static ReservationStatus from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        return ReservationStatus.valueOf(value.trim().toUpperCase());
    }
}

package com.netcracker.parkinglotmanagement.api.domain;

/** Lifecycle of a parking slip: open on entry, closed and priced on exit. */
public enum SlipStatus {

    /** Vehicle is inside; exit time and cost are not yet known. */
    ACTIVE,

    /** Vehicle has left; exit time, basic cost, penalty and total are final. */
    CLOSED;

    public static SlipStatus from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        return SlipStatus.valueOf(value.trim().toUpperCase());
    }
}

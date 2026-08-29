package com.netcracker.parkinglotmanagement.api.domain;

/**
 * Lifecycle of a single parking slot.
 *
 * <pre>
 *   FREE --reserve--> RESERVED --claim--> OCCUPIED --exit--> FREE
 *     \----------------check-in----------^
 * </pre>
 *
 * Every transition is performed as a guarded UPDATE that also matches on the
 * slot's current status and version, so concurrent allocations cannot collide.
 */
public enum SlotStatus {

    /** Available for allocation. */
    FREE,

    /** Held for a future reservation window; not allocatable to a walk-in. */
    RESERVED,

    /** A vehicle is parked in it and an open parking slip refers to it. */
    OCCUPIED,

    /** Withdrawn from service (maintenance); never allocated. */
    OUT_OF_SERVICE;

    public static SlotStatus from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        return SlotStatus.valueOf(value.trim().toUpperCase());
    }
}

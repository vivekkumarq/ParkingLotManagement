package com.netcracker.parkinglotmanagement.api.exception;

/**
 * The vehicle already has an open slip, so it cannot be checked in again.
 * Mapped to HTTP 409.
 */
public class DuplicateEntryException extends ParkingLotException {

    private static final long serialVersionUID = 1L;

    public DuplicateEntryException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return "DUPLICATE_ENTRY";
    }
}

package com.netcracker.parkinglotmanagement.api.exception;

/**
 * Base type for every error this domain raises deliberately. The web layer's
 * {@code GlobalExceptionHandler} maps each subtype to an HTTP status, so a
 * service never needs to know about HTTP.
 */
public abstract class ParkingLotException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected ParkingLotException(String message) {
        super(message);
    }

    protected ParkingLotException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Stable, machine-readable code echoed in the error payload. */
    public abstract String getErrorCode();
}

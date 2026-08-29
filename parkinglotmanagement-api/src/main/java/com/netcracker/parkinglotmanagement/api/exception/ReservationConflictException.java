package com.netcracker.parkinglotmanagement.api.exception;

/**
 * The requested reservation window overlaps a booking that already holds the
 * slot. Mapped to HTTP 409.
 */
public class ReservationConflictException extends ParkingLotException {

    private static final long serialVersionUID = 1L;

    public ReservationConflictException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return "RESERVATION_CONFLICT";
    }
}

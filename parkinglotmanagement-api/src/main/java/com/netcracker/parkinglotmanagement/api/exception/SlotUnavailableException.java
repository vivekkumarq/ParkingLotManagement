package com.netcracker.parkinglotmanagement.api.exception;

/**
 * A specific slot could not be transitioned because another transaction changed
 * it first, or because it is not in the state the operation requires. Mapped to
 * HTTP 409.
 */
public class SlotUnavailableException extends ParkingLotException {

    private static final long serialVersionUID = 1L;

    public SlotUnavailableException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return "SLOT_UNAVAILABLE";
    }
}

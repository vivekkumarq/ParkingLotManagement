package com.netcracker.parkinglotmanagement.api.exception;

/** The request is well-formed but semantically invalid. Mapped to HTTP 400. */
public class InvalidRequestException extends ParkingLotException {

    private static final long serialVersionUID = 1L;

    public InvalidRequestException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return "INVALID_REQUEST";
    }
}

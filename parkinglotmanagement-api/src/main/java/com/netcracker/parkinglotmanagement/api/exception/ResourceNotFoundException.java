package com.netcracker.parkinglotmanagement.api.exception;

/** A requested record does not exist. Mapped to HTTP 404. */
public class ResourceNotFoundException extends ParkingLotException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " " + id + " was not found");
    }

    @Override
    public String getErrorCode() {
        return "RESOURCE_NOT_FOUND";
    }
}

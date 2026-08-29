package com.netcracker.parkinglotmanagement.api.exception;

import com.netcracker.parkinglotmanagement.api.domain.VehicleType;

/**
 * The lot has no free slot for the requested vehicle type. Mapped to HTTP 409,
 * because retrying later may well succeed.
 */
public class NoSlotAvailableException extends ParkingLotException {

    private static final long serialVersionUID = 1L;

    public NoSlotAvailableException(String message) {
        super(message);
    }

    public NoSlotAvailableException(VehicleType vehicleType) {
        super("No free slot is available for vehicle type " + vehicleType);
    }

    @Override
    public String getErrorCode() {
        return "NO_SLOT_AVAILABLE";
    }
}

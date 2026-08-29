package com.netcracker.parkinglotmanagement.api.service;

import com.netcracker.parkinglotmanagement.api.dto.ParkingSlipDTO;
import com.netcracker.parkinglotmanagement.api.dto.VehicleEntryRequest;
import com.netcracker.parkinglotmanagement.api.dto.VehicleExitRequest;
import com.netcracker.parkinglotmanagement.api.dto.VehicleExitResponse;

import java.util.List;
import java.util.UUID;

/** Vehicle entry and exit: the slip lifecycle. */
public interface ParkingSlipService {

    /**
     * Allocates a slot and opens a slip.
     *
     * @throws com.netcracker.parkinglotmanagement.api.exception.DuplicateEntryException
     *         when the registration already has an open slip
     * @throws com.netcracker.parkinglotmanagement.api.exception.NoSlotAvailableException
     *         when the lot is full for this vehicle type
     */
    ParkingSlipDTO checkIn(VehicleEntryRequest request);

    /**
     * Closes a slip, prices the stay from the rate card and frees the slot.
     *
     * @throws com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException
     *         when the slip does not exist
     * @throws com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException
     *         when the slip is already closed, or the exit time precedes the entry time
     */
    VehicleExitResponse checkOut(UUID slipId, VehicleExitRequest request);

    /** Convenience for a gate that reads a registration rather than a slip id. */
    VehicleExitResponse checkOutByVehicleNumber(String vehicleNumber, VehicleExitRequest request);

    ParkingSlipDTO getSlipById(UUID id);

    /**
     * @throws com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException
     *         when the vehicle has no open slip
     */
    ParkingSlipDTO getActiveSlipByVehicleNumber(String vehicleNumber);

    /**
     * @param rsqlFilter optional RSQL expression over
     *                   {@code vehicleNumber, vehicleType, status, totalCost}
     */
    List<ParkingSlipDTO> getAllSlips(String rsqlFilter);
}

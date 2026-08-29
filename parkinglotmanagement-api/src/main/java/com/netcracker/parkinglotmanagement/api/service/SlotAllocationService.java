package com.netcracker.parkinglotmanagement.api.service;

import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlotDTO;

import java.util.UUID;

/**
 * Picks a slot for an arriving vehicle and moves it between states.
 *
 * <p>All three transitions are compare-and-set operations: the UPDATE matches on
 * the slot's id, its expected current status <em>and</em> its version, and the
 * call fails if it did not affect exactly one row. Two threads racing for the
 * last free slot therefore produce one winner and one
 * {@link com.netcracker.parkinglotmanagement.api.exception.NoSlotAvailableException},
 * never a double booking.
 */
public interface SlotAllocationService {

    /**
     * Finds the lowest free slot in the lot that fits the vehicle type and marks it
     * OCCUPIED. "Lowest" means ordered by floor number, then block code, then slot
     * number, so a lot fills from the ground up.
     *
     * <p>If another transaction claims the candidate first, the search retries with
     * the next candidate.
     *
     * @throws com.netcracker.parkinglotmanagement.api.exception.NoSlotAvailableException
     *         when the lot has no slot of that type left
     */
    ParkingSlotDTO allocate(UUID parkingLotId, VehicleType vehicleType);

    /**
     * Same search, but leaves the slot RESERVED rather than OCCUPIED.
     *
     * @throws com.netcracker.parkinglotmanagement.api.exception.NoSlotAvailableException
     *         when the lot has no slot of that type left
     */
    ParkingSlotDTO reserve(UUID parkingLotId, VehicleType vehicleType);

    /**
     * Moves a specific RESERVED slot to OCCUPIED when the booked vehicle arrives.
     *
     * @throws com.netcracker.parkinglotmanagement.api.exception.SlotUnavailableException
     *         when the slot is no longer RESERVED
     */
    ParkingSlotDTO claimReserved(UUID parkingSlotId);

    /**
     * Returns a slot to FREE, whatever state it was in.
     *
     * @throws com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException
     *         when no such slot exists
     */
    void release(UUID parkingSlotId);
}

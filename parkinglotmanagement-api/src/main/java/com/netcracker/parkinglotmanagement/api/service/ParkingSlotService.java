package com.netcracker.parkinglotmanagement.api.service;

import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.OccupancySummaryDTO;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlotDTO;
import com.netcracker.parkinglotmanagement.api.dto.SlotAvailabilityDTO;

import java.util.List;
import java.util.UUID;

/** CRUD over the physical slots plus the live availability views. */
public interface ParkingSlotService {

    ParkingSlotDTO createSlot(ParkingSlotDTO slot);

    ParkingSlotDTO getSlotById(UUID id);

    /**
     * @param rsqlFilter optional RSQL expression over
     *                   {@code slotNumber, charges, vehicleType, status, floorId}
     */
    List<ParkingSlotDTO> getAllSlots(String rsqlFilter);

    ParkingSlotDTO updateSlot(ParkingSlotDTO slot);

    void deleteSlot(UUID id);

    /** Takes a slot out of service, or puts it back. */
    ParkingSlotDTO setOutOfService(UUID id, boolean outOfService);

    /** Free/reserved/occupied counts for a lot, one row per floor. */
    List<SlotAvailabilityDTO> getAvailabilityByFloor(UUID parkingLotId);

    /** Free/reserved/occupied counts for a lot, one row per block. */
    List<SlotAvailabilityDTO> getAvailabilityByBlock(UUID parkingLotId);

    /** Free/reserved/occupied counts for a lot, one row per vehicle type. */
    List<SlotAvailabilityDTO> getAvailabilityByVehicleType(UUID parkingLotId);

    /** Slots that could be allocated right now for this vehicle type. */
    List<ParkingSlotDTO> getFreeSlots(UUID parkingLotId, VehicleType vehicleType);

    /** Headline occupancy figures for a lot. */
    OccupancySummaryDTO getOccupancy(UUID parkingLotId);
}

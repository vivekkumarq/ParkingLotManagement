package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.domain.SlotStatus;
import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.OccupancySummaryDTO;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlotDTO;
import com.netcracker.parkinglotmanagement.api.dto.SlotAvailabilityDTO;
import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException;
import com.netcracker.parkinglotmanagement.api.service.ParkingSlotService;
import com.netcracker.parkinglotmanagement.service.repository.ParkingSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ParkingSlotServiceImpl implements ParkingSlotService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final ParkingSlotRepository parkingSlotRepository;
    private final Clock clock;

    public ParkingSlotServiceImpl(ParkingSlotRepository parkingSlotRepository, Clock clock) {
        this.parkingSlotRepository = parkingSlotRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ParkingSlotDTO createSlot(ParkingSlotDTO slot) {
        return parkingSlotRepository.insert(slot);
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingSlotDTO getSlotById(UUID id) {
        return parkingSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking slot", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSlotDTO> getAllSlots(String rsqlFilter) {
        return parkingSlotRepository.findAll(rsqlFilter);
    }

    @Override
    @Transactional
    public ParkingSlotDTO updateSlot(ParkingSlotDTO slot) {
        if (slot.getId() == null) {
            throw new ResourceNotFoundException("Parking slot id is required for an update");
        }
        if (parkingSlotRepository.update(slot) == 0) {
            throw new ResourceNotFoundException("Parking slot", slot.getId());
        }
        return getSlotById(slot.getId());
    }

    @Override
    @Transactional
    public void deleteSlot(UUID id) {
        if (parkingSlotRepository.deleteById(id) == 0) {
            throw new ResourceNotFoundException("Parking slot", id);
        }
    }

    /**
     * A slot can only be withdrawn from service while it is FREE - taking away a bay
     * that a vehicle is standing in would leave the open slip pointing at a slot the
     * exit gate cannot release.
     */
    @Override
    @Transactional
    public ParkingSlotDTO setOutOfService(UUID id, boolean outOfService) {
        ParkingSlotDTO slot = getSlotById(id);
        SlotStatus from = outOfService ? SlotStatus.FREE : SlotStatus.OUT_OF_SERVICE;
        SlotStatus to = outOfService ? SlotStatus.OUT_OF_SERVICE : SlotStatus.FREE;

        if (slot.getStatus() == to) {
            return slot;
        }
        if (slot.getStatus() != from) {
            throw new InvalidRequestException(
                    "Slot " + id + " is " + slot.getStatus() + "; it must be " + from
                            + " before it can be moved to " + to);
        }
        if (!parkingSlotRepository.compareAndSetStatus(id, from, to, slot.getVersion())) {
            throw new InvalidRequestException("Slot " + id + " changed state; retry the request");
        }
        slot.setStatus(to);
        slot.setVersion(slot.getVersion() + 1);
        return slot;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlotAvailabilityDTO> getAvailabilityByFloor(UUID parkingLotId) {
        return parkingSlotRepository.availabilityByFloor(parkingLotId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlotAvailabilityDTO> getAvailabilityByBlock(UUID parkingLotId) {
        return parkingSlotRepository.availabilityByBlock(parkingLotId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlotAvailabilityDTO> getAvailabilityByVehicleType(UUID parkingLotId) {
        return parkingSlotRepository.availabilityByVehicleType(parkingLotId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSlotDTO> getFreeSlots(UUID parkingLotId, VehicleType vehicleType) {
        return parkingSlotRepository.findAllocatableSlots(parkingLotId, vehicleType, Integer.MAX_VALUE);
    }

    @Override
    @Transactional(readOnly = true)
    public OccupancySummaryDTO getOccupancy(UUID parkingLotId) {
        SlotAvailabilityDTO totals = parkingSlotRepository.totalsForLot(parkingLotId);
        int total = totals.getTotalSlots() != null ? totals.getTotalSlots() : 0;
        int occupied = totals.getOccupiedSlots() != null ? totals.getOccupiedSlots() : 0;

        return OccupancySummaryDTO.builder()
                .parkingLotId(parkingLotId)
                .asOf(LocalDateTime.now(clock))
                .totalSlots(total)
                .occupiedSlots(occupied)
                .reservedSlots(totals.getReservedSlots())
                .freeSlots(totals.getFreeSlots())
                .occupancyRate(percentage(occupied, total))
                .activeSlips(parkingSlotRepository.countActiveSlipsInLot(parkingLotId))
                .byVehicleType(parkingSlotRepository.availabilityByVehicleType(parkingLotId))
                .build();
    }

    /** @return {@code part / whole} as a percentage to 2 decimals; zero when the lot has no slots */
    static BigDecimal percentage(int part, int whole) {
        if (whole <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(part)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP);
    }
}

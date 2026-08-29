package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.domain.SlotStatus;
import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlotDTO;
import com.netcracker.parkinglotmanagement.api.exception.NoSlotAvailableException;
import com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException;
import com.netcracker.parkinglotmanagement.api.exception.SlotUnavailableException;
import com.netcracker.parkinglotmanagement.api.service.SlotAllocationService;
import com.netcracker.parkinglotmanagement.service.repository.ParkingSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Finds a slot and claims it without ever handing the same slot to two vehicles.
 *
 * <h2>Why this is not just "SELECT the first free slot, then UPDATE it"</h2>
 *
 * <p>Between the SELECT and the UPDATE another transaction can take the slot.
 * Locking the whole table would serialise every arrival at a busy car park, and
 * {@code SELECT ... FOR UPDATE} is not uniformly available across the databases
 * this project targets.
 *
 * <p>Instead the claim is a compare-and-set. The candidate's status and version
 * are read as part of the search, and the UPDATE that claims it repeats both in
 * its WHERE clause:
 *
 * <pre>
 * UPDATE parking_slot
 *    SET status = 'OCCUPIED', version = version + 1
 *  WHERE id = ? AND status = 'FREE' AND version = ?
 * </pre>
 *
 * <p>The database serialises the two UPDATEs on the row lock. The winner changes
 * one row; the loser changes zero, notices, and moves on to the next candidate.
 * No slot is double-allocated and no arrival waits on an unrelated one.
 *
 * <p>The search fetches a batch of candidates rather than one, so a burst of
 * simultaneous arrivals is absorbed without a second round trip to the database.
 * Only if every candidate in the batch is taken does the search re-query.
 */
@Service
public class SlotAllocationServiceImpl implements SlotAllocationService {

    private static final Logger LOG = LoggerFactory.getLogger(SlotAllocationServiceImpl.class);

    /** How many candidates to consider per database round trip. */
    static final int CANDIDATE_BATCH_SIZE = 16;

    /** How many batches to try before declaring the lot full. */
    static final int MAX_SEARCH_ROUNDS = 5;

    private final ParkingSlotRepository parkingSlotRepository;

    public SlotAllocationServiceImpl(ParkingSlotRepository parkingSlotRepository) {
        this.parkingSlotRepository = parkingSlotRepository;
    }

    @Override
    @Transactional
    public ParkingSlotDTO allocate(UUID parkingLotId, VehicleType vehicleType) {
        return claimFreeSlot(parkingLotId, vehicleType, SlotStatus.OCCUPIED);
    }

    @Override
    @Transactional
    public ParkingSlotDTO reserve(UUID parkingLotId, VehicleType vehicleType) {
        return claimFreeSlot(parkingLotId, vehicleType, SlotStatus.RESERVED);
    }

    @Override
    @Transactional
    public ParkingSlotDTO claimReserved(UUID parkingSlotId) {
        ParkingSlotDTO slot = parkingSlotRepository.findById(parkingSlotId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking slot", parkingSlotId));

        if (slot.getStatus() != SlotStatus.RESERVED) {
            throw new SlotUnavailableException(
                    "Slot " + parkingSlotId + " is " + slot.getStatus() + ", not RESERVED");
        }
        if (!parkingSlotRepository.compareAndSetStatus(
                parkingSlotId, SlotStatus.RESERVED, SlotStatus.OCCUPIED, slot.getVersion())) {
            throw new SlotUnavailableException(
                    "Slot " + parkingSlotId + " changed state while it was being claimed");
        }

        slot.setStatus(SlotStatus.OCCUPIED);
        slot.setVersion(slot.getVersion() + 1);
        return slot;
    }

    @Override
    @Transactional
    public void release(UUID parkingSlotId) {
        // Unconditional: a vehicle leaving must always free its slot, whatever state
        // bookkeeping elsewhere left it in.
        if (parkingSlotRepository.setStatus(parkingSlotId, SlotStatus.FREE) == 0) {
            throw new ResourceNotFoundException("Parking slot", parkingSlotId);
        }
    }

    private ParkingSlotDTO claimFreeSlot(UUID parkingLotId, VehicleType vehicleType, SlotStatus target) {
        if (parkingLotId == null) {
            throw new ResourceNotFoundException("A parking lot id is required to allocate a slot");
        }

        for (int round = 0; round < MAX_SEARCH_ROUNDS; round++) {
            List<ParkingSlotDTO> candidates =
                    parkingSlotRepository.findAllocatableSlots(parkingLotId, vehicleType, CANDIDATE_BATCH_SIZE);

            if (candidates.isEmpty()) {
                throw new NoSlotAvailableException(vehicleType);
            }

            for (ParkingSlotDTO candidate : candidates) {
                if (parkingSlotRepository.compareAndSetStatus(
                        candidate.getId(), SlotStatus.FREE, target, candidate.getVersion())) {
                    candidate.setStatus(target);
                    candidate.setVersion(candidate.getVersion() + 1);
                    return candidate;
                }
                LOG.debug("Slot {} was taken by another request; trying the next candidate", candidate.getId());
            }
        }

        // Every candidate in every round was claimed by someone else between the read
        // and the write. Under sustained contention that is indistinguishable from a
        // full lot from this caller's point of view.
        throw new NoSlotAvailableException(
                "No slot could be allocated for vehicle type " + vehicleType
                        + " after " + MAX_SEARCH_ROUNDS + " attempts; the lot is full or heavily contended");
    }
}

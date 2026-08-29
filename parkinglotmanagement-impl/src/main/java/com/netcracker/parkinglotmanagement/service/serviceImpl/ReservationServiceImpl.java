package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.domain.ReservationStatus;
import com.netcracker.parkinglotmanagement.api.domain.SlipStatus;
import com.netcracker.parkinglotmanagement.api.domain.SlotStatus;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlipDTO;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlotDTO;
import com.netcracker.parkinglotmanagement.api.dto.ReservationDTO;
import com.netcracker.parkinglotmanagement.api.dto.ReservationRequest;
import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import com.netcracker.parkinglotmanagement.api.exception.ReservationConflictException;
import com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException;
import com.netcracker.parkinglotmanagement.api.exception.SlotUnavailableException;
import com.netcracker.parkinglotmanagement.api.service.ReservationService;
import com.netcracker.parkinglotmanagement.api.service.SlotAllocationService;
import com.netcracker.parkinglotmanagement.service.repository.ParkingSlipRepository;
import com.netcracker.parkinglotmanagement.service.repository.ParkingSlotRepository;
import com.netcracker.parkinglotmanagement.service.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Booking a slot ahead of time.
 *
 * <p>A reservation holds its slot outright: the slot is moved to RESERVED for the
 * whole life of the booking, so a walk-in can never be given it. Overlap
 * detection on top of that guards against two bookings for the same slot in
 * intersecting windows - which is possible even when the slot is held, because a
 * second booking for a later window is legitimate.
 */
@Service
public class ReservationServiceImpl implements ReservationService {

    private static final Logger LOG = LoggerFactory.getLogger(ReservationServiceImpl.class);
    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(2);

    private final ReservationRepository reservationRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingSlipRepository parkingSlipRepository;
    private final SlotAllocationService slotAllocationService;
    private final Clock clock;

    public ReservationServiceImpl(ReservationRepository reservationRepository,
                                  ParkingSlotRepository parkingSlotRepository,
                                  ParkingSlipRepository parkingSlipRepository,
                                  SlotAllocationService slotAllocationService,
                                  Clock clock) {
        this.reservationRepository = reservationRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.parkingSlipRepository = parkingSlipRepository;
        this.slotAllocationService = slotAllocationService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ReservationDTO createReservation(ReservationRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime start = request.getStartTimestamp();
        LocalDateTime end = start.plusHours(request.getDurationInHours());

        if (start.isBefore(now)) {
            throw new InvalidRequestException(
                    "A reservation cannot start in the past (" + start + " is before " + now + ")");
        }

        ParkingSlotDTO slot = request.getParkingSlotId() != null
                ? holdNamedSlot(request, start, end)
                : slotAllocationService.reserve(request.getParkingLotId(), request.getVehicleType());

        ReservationDTO reservation = ReservationDTO.builder()
                .id(UUID.randomUUID())
                .customerId(request.getCustomerId())
                .parkingSlotId(slot.getId())
                .vehicleNumber(normalise(request.getVehicleNumber()))
                .startTimestamp(start)
                .endTimestamp(end)
                .durationInHours(request.getDurationInHours())
                .bookingDate(now.toLocalDate())
                .status(ReservationStatus.BOOKED)
                .createdAt(now)
                .build();

        return reservationRepository.insert(reservation);
    }

    /**
     * Books one specific slot. The slot must belong to the requested lot, must not
     * already be held for an intersecting window, and is moved to RESERVED with the
     * same compare-and-set the allocator uses.
     */
    private ParkingSlotDTO holdNamedSlot(ReservationRequest request, LocalDateTime start, LocalDateTime end) {
        UUID slotId = request.getParkingSlotId();
        ParkingSlotDTO slot = parkingSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking slot", slotId));

        if (!parkingSlotRepository.slotBelongsToLot(slotId, request.getParkingLotId())) {
            throw new InvalidRequestException(
                    "Slot " + slotId + " does not belong to parking lot " + request.getParkingLotId());
        }
        if (slot.getVehicleType() != request.getVehicleType()) {
            throw new InvalidRequestException(
                    "Slot " + slotId + " takes " + slot.getVehicleType()
                            + " but the reservation is for " + request.getVehicleType());
        }

        List<ReservationDTO> clashes = reservationRepository.findOverlapping(slotId, start, end);
        if (!clashes.isEmpty()) {
            ReservationDTO clash = clashes.get(0);
            throw new ReservationConflictException(
                    "Slot " + slotId + " is already booked from " + clash.getStartTimestamp()
                            + " to " + clash.getEndTimestamp());
        }

        if (slot.getStatus() == SlotStatus.OCCUPIED) {
            throw new SlotUnavailableException("Slot " + slotId + " is currently occupied");
        }
        if (slot.getStatus() == SlotStatus.OUT_OF_SERVICE) {
            throw new SlotUnavailableException("Slot " + slotId + " is out of service");
        }
        // Already RESERVED for a non-overlapping window: it stays reserved, nothing to do.
        if (slot.getStatus() == SlotStatus.FREE
                && !parkingSlotRepository.compareAndSetStatus(
                        slotId, SlotStatus.FREE, SlotStatus.RESERVED, slot.getVersion())) {
            throw new SlotUnavailableException(
                    "Slot " + slotId + " was taken while the reservation was being made");
        }
        return slot;
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationDTO getReservationById(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationDTO> getAllReservations(String rsqlFilter) {
        return reservationRepository.findAll(rsqlFilter);
    }

    @Override
    @Transactional
    public ReservationDTO cancelReservation(UUID id) {
        ReservationDTO reservation = getReservationById(id);
        if (reservation.getStatus() != ReservationStatus.BOOKED) {
            throw new InvalidRequestException(
                    "Reservation " + id + " is " + reservation.getStatus() + " and cannot be cancelled");
        }
        if (!reservationRepository.compareAndSetStatus(
                id, ReservationStatus.BOOKED, ReservationStatus.CANCELLED)) {
            throw new InvalidRequestException(
                    "Reservation " + id + " changed state while it was being cancelled");
        }
        releaseIfUnheld(reservation);
        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservation;
    }

    @Override
    @Transactional
    public ParkingSlipDTO claimReservation(UUID id, LocalDateTime arrivalTime) {
        ReservationDTO reservation = getReservationById(id);
        LocalDateTime arrival = arrivalTime != null ? arrivalTime : LocalDateTime.now(clock);

        if (reservation.getStatus() != ReservationStatus.BOOKED) {
            throw new InvalidRequestException(
                    "Reservation " + id + " is " + reservation.getStatus() + " and cannot be claimed");
        }
        if (arrival.isAfter(reservation.getEndTimestamp())) {
            throw new InvalidRequestException(
                    "Reservation " + id + " ended at " + reservation.getEndTimestamp()
                            + "; it can no longer be claimed");
        }

        ParkingSlotDTO slot = slotAllocationService.claimReserved(reservation.getParkingSlotId());

        if (!reservationRepository.compareAndSetStatus(
                id, ReservationStatus.BOOKED, ReservationStatus.CLAIMED)) {
            throw new InvalidRequestException(
                    "Reservation " + id + " changed state while it was being claimed");
        }

        ParkingSlipDTO slip = ParkingSlipDTO.builder()
                .id(UUID.randomUUID())
                .parkingSlotReservationId(reservation.getId())
                .parkingSlotId(reservation.getParkingSlotId())
                .customerId(reservation.getCustomerId())
                .vehicleNumber(reservation.getVehicleNumber())
                .vehicleType(slot.getVehicleType())
                .actualEntryTime(arrival)
                .basicCost(ZERO_MONEY)
                .penalty(ZERO_MONEY)
                .totalCost(ZERO_MONEY)
                .status(SlipStatus.ACTIVE)
                .build();

        return parkingSlipRepository.insert(slip);
    }

    @Override
    @Transactional
    public int expireStaleReservations(LocalDateTime asOf) {
        LocalDateTime cutoff = asOf != null ? asOf : LocalDateTime.now(clock);
        List<ReservationDTO> expirable = reservationRepository.findExpirable(cutoff);

        int expired = 0;
        for (ReservationDTO reservation : expirable) {
            if (reservationRepository.compareAndSetStatus(
                    reservation.getId(), ReservationStatus.BOOKED, ReservationStatus.EXPIRED)) {
                releaseIfUnheld(reservation);
                expired++;
            }
        }
        if (expired > 0) {
            LOG.info("Expired {} unclaimed reservation(s) as of {}", expired, cutoff);
        }
        return expired;
    }

    /**
     * Frees a slot that a cancelled or expired booking was holding - but only if no
     * <em>other</em> live booking still needs it, and only if it is not occupied by
     * a vehicle that is physically there.
     */
    private void releaseIfUnheld(ReservationDTO reservation) {
        UUID slotId = reservation.getParkingSlotId();
        if (slotId == null) {
            return;
        }

        boolean stillHeld = !reservationRepository
                .findOverlapping(slotId, reservation.getStartTimestamp(), reservation.getEndTimestamp())
                .isEmpty();
        if (stillHeld) {
            return;
        }

        parkingSlotRepository.findById(slotId).ifPresent(slot -> {
            if (slot.getStatus() == SlotStatus.RESERVED) {
                parkingSlotRepository.compareAndSetStatus(
                        slotId, SlotStatus.RESERVED, SlotStatus.FREE, slot.getVersion());
            }
        });
    }

    private static String normalise(String vehicleNumber) {
        if (vehicleNumber == null) {
            throw new InvalidRequestException("vehicleNumber is required");
        }
        String normalised = vehicleNumber.replace(" ", "").replace("-", "").toUpperCase();
        if (normalised.isEmpty()) {
            throw new InvalidRequestException("vehicleNumber is required");
        }
        return normalised;
    }
}

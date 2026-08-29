package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.domain.SlipStatus;
import com.netcracker.parkinglotmanagement.api.dto.FeeBreakdownDTO;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlipDTO;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlotDTO;
import com.netcracker.parkinglotmanagement.api.dto.ReservationDTO;
import com.netcracker.parkinglotmanagement.api.dto.VehicleEntryRequest;
import com.netcracker.parkinglotmanagement.api.dto.VehicleExitRequest;
import com.netcracker.parkinglotmanagement.api.dto.VehicleExitResponse;
import com.netcracker.parkinglotmanagement.api.exception.DuplicateEntryException;
import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException;
import com.netcracker.parkinglotmanagement.api.service.ParkingSlipService;
import com.netcracker.parkinglotmanagement.api.service.SlotAllocationService;
import com.netcracker.parkinglotmanagement.service.fee.FeeCalculator;
import com.netcracker.parkinglotmanagement.service.repository.ParkingSlipRepository;
import com.netcracker.parkinglotmanagement.service.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Vehicle entry and exit. */
@Service
public class ParkingSlipServiceImpl implements ParkingSlipService {

    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(2);

    private final ParkingSlipRepository parkingSlipRepository;
    private final ReservationRepository reservationRepository;
    private final SlotAllocationService slotAllocationService;
    private final FeeCalculator feeCalculator;
    private final Clock clock;

    public ParkingSlipServiceImpl(ParkingSlipRepository parkingSlipRepository,
                                  ReservationRepository reservationRepository,
                                  SlotAllocationService slotAllocationService,
                                  FeeCalculator feeCalculator,
                                  Clock clock) {
        this.parkingSlipRepository = parkingSlipRepository;
        this.reservationRepository = reservationRepository;
        this.slotAllocationService = slotAllocationService;
        this.feeCalculator = feeCalculator;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ParkingSlipDTO checkIn(VehicleEntryRequest request) {
        String vehicleNumber = normalise(request.getVehicleNumber());

        // One vehicle, one open slip. Without this a mis-fired gate sensor would
        // allocate a second slot and leave the first one occupied forever.
        parkingSlipRepository.findActiveByVehicleNumber(vehicleNumber).ifPresent(existing -> {
            throw new DuplicateEntryException(
                    "Vehicle " + vehicleNumber + " already has an open slip (" + existing.getId() + ")");
        });

        LocalDateTime entryTime = request.getEntryTime() != null ? request.getEntryTime() : now();
        ParkingSlotDTO slot = slotAllocationService.allocate(request.getParkingLotId(), request.getVehicleType());

        ParkingSlipDTO slip = ParkingSlipDTO.builder()
                .id(UUID.randomUUID())
                .parkingSlotId(slot.getId())
                .customerId(request.getCustomerId())
                .vehicleNumber(vehicleNumber)
                .vehicleType(request.getVehicleType())
                .actualEntryTime(entryTime)
                .basicCost(ZERO_MONEY)
                .penalty(ZERO_MONEY)
                .totalCost(ZERO_MONEY)
                .status(SlipStatus.ACTIVE)
                .build();

        return parkingSlipRepository.insert(slip);
    }

    @Override
    @Transactional
    public VehicleExitResponse checkOut(UUID slipId, VehicleExitRequest request) {
        ParkingSlipDTO slip = parkingSlipRepository.findById(slipId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking slip", slipId));
        return closeSlip(slip, request);
    }

    @Override
    @Transactional
    public VehicleExitResponse checkOutByVehicleNumber(String vehicleNumber, VehicleExitRequest request) {
        String normalised = normalise(vehicleNumber);
        ParkingSlipDTO slip = parkingSlipRepository.findActiveByVehicleNumber(normalised)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle " + normalised + " has no open parking slip"));
        return closeSlip(slip, request);
    }

    private VehicleExitResponse closeSlip(ParkingSlipDTO slip, VehicleExitRequest request) {
        if (slip.getStatus() != SlipStatus.ACTIVE) {
            throw new InvalidRequestException(
                    "Parking slip " + slip.getId() + " is already closed");
        }

        LocalDateTime exitTime = request != null && request.getExitTime() != null
                ? request.getExitTime()
                : now();

        if (exitTime.isBefore(slip.getActualEntryTime())) {
            throw new InvalidRequestException(
                    "exitTime " + exitTime + " precedes the entry time " + slip.getActualEntryTime());
        }

        // A stay that came from a reservation is penalised for running past its window.
        LocalDateTime reservedUntil = Optional.ofNullable(slip.getParkingSlotReservationId())
                .flatMap(reservationRepository::findById)
                .map(ReservationDTO::getEndTimestamp)
                .orElse(null);

        FeeBreakdownDTO fee = feeCalculator.calculate(
                slip.getVehicleType(), slip.getActualEntryTime(), exitTime, reservedUntil);

        if (!parkingSlipRepository.close(
                slip.getId(), exitTime, fee.getBasicCost(), fee.getPenalty(), fee.getTotalCost())) {
            // Another exit request closed it between the read and the write.
            throw new InvalidRequestException(
                    "Parking slip " + slip.getId() + " was closed by a concurrent request");
        }

        slotAllocationService.release(slip.getParkingSlotId());

        slip.setActualExitTime(exitTime);
        slip.setBasicCost(fee.getBasicCost());
        slip.setPenalty(fee.getPenalty());
        slip.setTotalCost(fee.getTotalCost());
        slip.setStatus(SlipStatus.CLOSED);

        return VehicleExitResponse.builder().slip(slip).fee(fee).build();
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingSlipDTO getSlipById(UUID id) {
        return parkingSlipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking slip", id));
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingSlipDTO getActiveSlipByVehicleNumber(String vehicleNumber) {
        String normalised = normalise(vehicleNumber);
        return parkingSlipRepository.findActiveByVehicleNumber(normalised)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle " + normalised + " has no open parking slip"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSlipDTO> getAllSlips(String rsqlFilter) {
        return parkingSlipRepository.findAll(rsqlFilter);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /** Registrations are compared case- and space-insensitively. */
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

package com.netcracker.parkinglotmanagement.web.controller;

import com.netcracker.parkinglotmanagement.api.dto.ParkingSlipDTO;
import com.netcracker.parkinglotmanagement.api.dto.ReservationDTO;
import com.netcracker.parkinglotmanagement.api.dto.ReservationRequest;
import com.netcracker.parkinglotmanagement.api.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/parking-lot-management/reservations")
@Tag(name = "Reservations", description = "Booking a slot ahead of time and claiming it on arrival")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Book a slot for a future window",
            description = "Windows are half-open: a booking ending at 12:00 does not clash with one "
                    + "starting at 12:00. Omit parkingSlotId to let the allocator pick the lowest free slot.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booked"),
            @ApiResponse(responseCode = "400", description = "Validation failed, or the window starts in the past"),
            @ApiResponse(responseCode = "409", description = "Overlapping booking, or no slot available")
    })
    public ResponseEntity<ReservationDTO> create(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createReservation(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one reservation")
    public ResponseEntity<ReservationDTO> get(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    @GetMapping
    @Operation(summary = "List reservations",
            description = "Optional RSQL filter over id, customerId, parkingSlotId, vehicleNumber, status, "
                    + "durationInHours, startTimestamp, endTimestamp and bookingDate. "
                    + "Example: status==BOOKED;durationInHours=ge=2")
    public ResponseEntity<List<ReservationDTO>> list(
            @Parameter(description = "RSQL filter", example = "status==BOOKED")
            @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(reservationService.getAllReservations(search));
    }

    @PostMapping("/{id}/claim")
    @Operation(summary = "The booked vehicle arrived",
            description = "Moves the held slot to OCCUPIED and opens a parking slip against the booking.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Slip issued"),
            @ApiResponse(responseCode = "400", description = "Booking is not claimable, or the window has passed")
    })
    public ResponseEntity<ParkingSlipDTO> claim(
            @PathVariable UUID id,
            @RequestParam(value = "arrivalTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime arrivalTime) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.claimReservation(id, arrivalTime));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a booking and release its slot")
    public ResponseEntity<ReservationDTO> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.cancelReservation(id));
    }

    @PostMapping("/expire")
    @Operation(summary = "Expire unclaimed bookings whose window has closed",
            description = "Idempotent sweep, intended for a scheduler. Returns how many were expired.")
    public ResponseEntity<Map<String, Integer>> expire(
            @RequestParam(value = "asOf", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime asOf) {
        int expired = reservationService.expireStaleReservations(asOf);
        return ResponseEntity.ok(Collections.singletonMap("expired", expired));
    }
}

package com.netcracker.parkinglotmanagement.api.service;

import com.netcracker.parkinglotmanagement.api.dto.ParkingSlipDTO;
import com.netcracker.parkinglotmanagement.api.dto.ReservationDTO;
import com.netcracker.parkinglotmanagement.api.dto.ReservationRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Booking a slot ahead of time, and turning that booking into a stay. */
public interface ReservationService {

    /**
     * Books a slot for {@code [start, start + durationInHours)}.
     *
     * <p>Windows are half-open, so a booking that ends exactly when another starts
     * is accepted; any real overlap is refused.
     *
     * @throws com.netcracker.parkinglotmanagement.api.exception.ReservationConflictException
     *         when the requested slot is already held for an overlapping window
     * @throws com.netcracker.parkinglotmanagement.api.exception.NoSlotAvailableException
     *         when no slot of that type is free and no specific slot was named
     * @throws com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException
     *         when the window starts in the past
     */
    ReservationDTO createReservation(ReservationRequest request);

    ReservationDTO getReservationById(UUID id);

    /** @param rsqlFilter optional RSQL expression over {@code vehicleNumber, status, durationInHours} */
    List<ReservationDTO> getAllReservations(String rsqlFilter);

    /**
     * Releases the held slot and marks the booking CANCELLED.
     *
     * @throws com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException
     *         when the booking is not in BOOKED state
     */
    ReservationDTO cancelReservation(UUID id);

    /**
     * The booked vehicle arrived: moves the slot to OCCUPIED and opens a slip
     * against the reservation.
     *
     * @throws com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException
     *         when the booking is not BOOKED, or the arrival is outside its window
     */
    ParkingSlipDTO claimReservation(UUID id, LocalDateTime arrivalTime);

    /**
     * Expires every BOOKED reservation whose window ended before {@code asOf}
     * without being claimed, releasing each held slot.
     *
     * @return how many bookings were expired
     */
    int expireStaleReservations(LocalDateTime asOf);
}

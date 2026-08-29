package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.domain.ReservationStatus;
import com.netcracker.parkinglotmanagement.api.domain.SlipStatus;
import com.netcracker.parkinglotmanagement.api.domain.SlotStatus;
import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlipDTO;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlotDTO;
import com.netcracker.parkinglotmanagement.api.dto.ReservationDTO;
import com.netcracker.parkinglotmanagement.api.dto.ReservationRequest;
import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import com.netcracker.parkinglotmanagement.api.exception.NoSlotAvailableException;
import com.netcracker.parkinglotmanagement.api.exception.ReservationConflictException;
import com.netcracker.parkinglotmanagement.api.service.ReservationService;
import com.netcracker.parkinglotmanagement.service.repository.ParkingSlotRepository;
import com.netcracker.parkinglotmanagement.support.DatabaseFixture;
import com.netcracker.parkinglotmanagement.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reservation behaviour against the real database.
 *
 * <p>Reservation windows are half-open, {@code [start, end)}. The tests below pin
 * down exactly where the boundary sits, because "adjacent bookings are allowed but
 * overlapping ones are not" is the whole point of the feature and an off-by-one
 * here would either reject legitimate bookings or double-book a bay.
 */
@IntegrationTest
class ReservationServiceIntegrationTest {

    /** Comfortably in the future, so "cannot start in the past" never trips by accident. */
    private static final LocalDateTime TOMORROW_NOON =
            LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0);

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ParkingSlotRepository parkingSlotRepository;

    @Autowired
    private DatabaseFixture fixture;

    @AfterEach
    void tearDown() {
        fixture.clean();
    }

    private ReservationRequest request(UUID lotId, UUID slotId, LocalDateTime start, int hours) {
        return ReservationRequest.builder()
                .vehicleNumber("KA01AB1234")
                .vehicleType(VehicleType.CAR)
                .parkingLotId(lotId)
                .parkingSlotId(slotId)
                .startTimestamp(start)
                .durationInHours(hours)
                .build();
    }

    @Test
    @DisplayName("a booking holds its slot as RESERVED and records a half-open window")
    void bookingHoldsTheSlot() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);

        ReservationDTO reservation = reservationService.createReservation(
                request(lot.lotId(), null, TOMORROW_NOON, 3));

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.BOOKED);
        assertThat(reservation.getStartTimestamp()).isEqualTo(TOMORROW_NOON);
        assertThat(reservation.getEndTimestamp()).isEqualTo(TOMORROW_NOON.plusHours(3));
        assertThat(reservation.getParkingSlotId()).isEqualTo(lot.firstSlot().getId());

        ParkingSlotDTO slot = parkingSlotRepository.findById(lot.firstSlot().getId()).orElseThrow();
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.RESERVED);
    }

    @Test
    @DisplayName("a window that overlaps an existing booking on the same slot is refused")
    void overlappingWindowIsRejected() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        UUID slotId = lot.firstSlot().getId();

        // 12:00-15:00
        reservationService.createReservation(request(lot.lotId(), slotId, TOMORROW_NOON, 3));

        // 14:00-16:00 - one hour of overlap
        assertThatThrownBy(() -> reservationService.createReservation(
                request(lot.lotId(), slotId, TOMORROW_NOON.plusHours(2), 2)))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    @DisplayName("a window fully inside an existing booking is refused")
    void containedWindowIsRejected() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        UUID slotId = lot.firstSlot().getId();

        // 12:00-18:00
        reservationService.createReservation(request(lot.lotId(), slotId, TOMORROW_NOON, 6));

        // 14:00-15:00
        assertThatThrownBy(() -> reservationService.createReservation(
                request(lot.lotId(), slotId, TOMORROW_NOON.plusHours(2), 1)))
                .isInstanceOf(ReservationConflictException.class);
    }

    @Test
    @DisplayName("a window that swallows an existing booking is refused")
    void enclosingWindowIsRejected() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        UUID slotId = lot.firstSlot().getId();

        // 14:00-15:00
        reservationService.createReservation(
                request(lot.lotId(), slotId, TOMORROW_NOON.plusHours(2), 1));

        // 12:00-18:00
        assertThatThrownBy(() -> reservationService.createReservation(
                request(lot.lotId(), slotId, TOMORROW_NOON, 6)))
                .isInstanceOf(ReservationConflictException.class);
    }

    @Test
    @DisplayName("a booking that starts exactly when another ends is allowed")
    void adjacentWindowIsAccepted() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        UUID slotId = lot.firstSlot().getId();

        // 12:00-15:00 then 15:00-17:00 - touching, not overlapping.
        ReservationDTO first = reservationService.createReservation(
                request(lot.lotId(), slotId, TOMORROW_NOON, 3));
        ReservationDTO second = reservationService.createReservation(
                request(lot.lotId(), slotId, TOMORROW_NOON.plusHours(3), 2));

        assertThat(first.getEndTimestamp()).isEqualTo(second.getStartTimestamp());
        assertThat(second.getStatus()).isEqualTo(ReservationStatus.BOOKED);
        assertThat(second.getParkingSlotId()).isEqualTo(slotId);
    }

    @Test
    @DisplayName("a booking that ends exactly when another starts is allowed")
    void precedingAdjacentWindowIsAccepted() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        UUID slotId = lot.firstSlot().getId();

        reservationService.createReservation(
                request(lot.lotId(), slotId, TOMORROW_NOON.plusHours(3), 2));
        ReservationDTO earlier = reservationService.createReservation(
                request(lot.lotId(), slotId, TOMORROW_NOON, 3));

        assertThat(earlier.getStatus()).isEqualTo(ReservationStatus.BOOKED);
    }

    @Test
    @DisplayName("an overlapping window on a different slot is fine")
    void differentSlotsMayOverlap() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 2, VehicleType.CAR);

        ReservationDTO first = reservationService.createReservation(
                request(lot.lotId(), lot.slots().get(0).getId(), TOMORROW_NOON, 3));
        ReservationDTO second = reservationService.createReservation(
                request(lot.lotId(), lot.slots().get(1).getId(), TOMORROW_NOON, 3));

        assertThat(first.getParkingSlotId()).isNotEqualTo(second.getParkingSlotId());
    }

    @Test
    @DisplayName("a booking in the past is refused")
    void pastWindowIsRejected() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);

        assertThatThrownBy(() -> reservationService.createReservation(
                request(lot.lotId(), null, LocalDateTime.now().minusHours(1), 2)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("past");
    }

    @Test
    @DisplayName("booking a slot cut for another vehicle type is refused")
    void mismatchedVehicleTypeIsRejected() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);

        ReservationRequest truckOnACarSlot = ReservationRequest.builder()
                .vehicleNumber("KA01TR9999")
                .vehicleType(VehicleType.TRUCK)
                .parkingLotId(lot.lotId())
                .parkingSlotId(lot.firstSlot().getId())
                .startTimestamp(TOMORROW_NOON)
                .durationInHours(2)
                .build();

        assertThatThrownBy(() -> reservationService.createReservation(truckOnACarSlot))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("takes CAR");
    }

    @Test
    @DisplayName("when no slot is named and the lot is full, the booking is refused")
    void fullLotCannotBeBooked() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        reservationService.createReservation(request(lot.lotId(), null, TOMORROW_NOON, 2));

        assertThatThrownBy(() -> reservationService.createReservation(
                request(lot.lotId(), null, TOMORROW_NOON.plusDays(1), 2)))
                .isInstanceOf(NoSlotAvailableException.class);
    }

    @Test
    @DisplayName("cancelling releases the held slot")
    void cancellingReleasesTheSlot() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        ReservationDTO reservation = reservationService.createReservation(
                request(lot.lotId(), null, TOMORROW_NOON, 3));

        ReservationDTO cancelled = reservationService.cancelReservation(reservation.getId());

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(parkingSlotRepository.findById(reservation.getParkingSlotId()).orElseThrow().getStatus())
                .isEqualTo(SlotStatus.FREE);
    }

    @Test
    @DisplayName("a booking cannot be cancelled twice")
    void doubleCancellationIsRejected() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        ReservationDTO reservation = reservationService.createReservation(
                request(lot.lotId(), null, TOMORROW_NOON, 3));
        reservationService.cancelReservation(reservation.getId());

        assertThatThrownBy(() -> reservationService.cancelReservation(reservation.getId()))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    @DisplayName("claiming a booking occupies the slot and opens a slip against it")
    void claimingOpensASlip() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        ReservationDTO reservation = reservationService.createReservation(
                request(lot.lotId(), null, TOMORROW_NOON, 3));

        ParkingSlipDTO slip = reservationService.claimReservation(
                reservation.getId(), TOMORROW_NOON.plusMinutes(10));

        assertThat(slip.getStatus()).isEqualTo(SlipStatus.ACTIVE);
        assertThat(slip.getParkingSlotReservationId()).isEqualTo(reservation.getId());
        assertThat(slip.getParkingSlotId()).isEqualTo(reservation.getParkingSlotId());
        assertThat(slip.getVehicleNumber()).isEqualTo("KA01AB1234");
        assertThat(slip.getVehicleType()).isEqualTo(VehicleType.CAR);

        assertThat(parkingSlotRepository.findById(reservation.getParkingSlotId()).orElseThrow().getStatus())
                .isEqualTo(SlotStatus.OCCUPIED);
        assertThat(reservationService.getReservationById(reservation.getId()).getStatus())
                .isEqualTo(ReservationStatus.CLAIMED);
    }

    @Test
    @DisplayName("a booking cannot be claimed after its window has closed")
    void claimingAfterTheWindowIsRejected() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        ReservationDTO reservation = reservationService.createReservation(
                request(lot.lotId(), null, TOMORROW_NOON, 3));

        assertThatThrownBy(() -> reservationService.claimReservation(
                reservation.getId(), TOMORROW_NOON.plusHours(4)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("no longer be claimed");
    }

    @Test
    @DisplayName("a booking cannot be claimed twice")
    void doubleClaimIsRejected() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        ReservationDTO reservation = reservationService.createReservation(
                request(lot.lotId(), null, TOMORROW_NOON, 3));
        reservationService.claimReservation(reservation.getId(), TOMORROW_NOON);

        assertThatThrownBy(() -> reservationService.claimReservation(reservation.getId(), TOMORROW_NOON))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("CLAIMED");
    }

    @Test
    @DisplayName("an unclaimed booking expires once its window has closed, freeing the slot")
    void unclaimedBookingsExpire() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        ReservationDTO reservation = reservationService.createReservation(
                request(lot.lotId(), null, TOMORROW_NOON, 3));

        int expired = reservationService.expireStaleReservations(TOMORROW_NOON.plusHours(4));

        assertThat(expired).isEqualTo(1);
        assertThat(reservationService.getReservationById(reservation.getId()).getStatus())
                .isEqualTo(ReservationStatus.EXPIRED);
        assertThat(parkingSlotRepository.findById(reservation.getParkingSlotId()).orElseThrow().getStatus())
                .isEqualTo(SlotStatus.FREE);
    }

    @Test
    @DisplayName("a booking whose window is still open is not expired")
    void liveBookingsAreNotExpired() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        ReservationDTO reservation = reservationService.createReservation(
                request(lot.lotId(), null, TOMORROW_NOON, 3));

        int expired = reservationService.expireStaleReservations(TOMORROW_NOON.plusHours(1));

        assertThat(expired).isZero();
        assertThat(reservationService.getReservationById(reservation.getId()).getStatus())
                .isEqualTo(ReservationStatus.BOOKED);
    }

    @Test
    @DisplayName("the expiry sweep is idempotent")
    void expirySweepIsIdempotent() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        reservationService.createReservation(request(lot.lotId(), null, TOMORROW_NOON, 3));

        assertThat(reservationService.expireStaleReservations(TOMORROW_NOON.plusHours(4))).isEqualTo(1);
        assertThat(reservationService.expireStaleReservations(TOMORROW_NOON.plusHours(4))).isZero();
    }

    @Test
    @DisplayName("an expired booking does not free a slot a later booking still holds")
    void expiryRespectsOtherHolders() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        UUID slotId = lot.firstSlot().getId();

        // 12:00-15:00, then an adjacent 15:00-17:00 on the same slot.
        reservationService.createReservation(request(lot.lotId(), slotId, TOMORROW_NOON, 3));
        reservationService.createReservation(request(lot.lotId(), slotId, TOMORROW_NOON.plusHours(3), 2));

        // Sweep at 15:30: the first has closed, the second has not.
        int expired = reservationService.expireStaleReservations(TOMORROW_NOON.plusHours(3).plusMinutes(30));

        assertThat(expired).isEqualTo(1);
        assertThat(parkingSlotRepository.findById(slotId).orElseThrow().getStatus())
                .as("the second booking still needs the slot")
                .isEqualTo(SlotStatus.RESERVED);
    }

    @Test
    @DisplayName("RSQL filters apply to the reservation listing")
    void listingIsFilterable() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 2, VehicleType.CAR);
        ReservationDTO booked = reservationService.createReservation(
                request(lot.lotId(), lot.slots().get(0).getId(), TOMORROW_NOON, 3));
        ReservationDTO cancelled = reservationService.createReservation(
                request(lot.lotId(), lot.slots().get(1).getId(), TOMORROW_NOON, 3));
        reservationService.cancelReservation(cancelled.getId());

        List<ReservationDTO> stillBooked = reservationService.getAllReservations("status==BOOKED");

        assertThat(stillBooked).extracting(ReservationDTO::getId).containsExactly(booked.getId());
    }
}

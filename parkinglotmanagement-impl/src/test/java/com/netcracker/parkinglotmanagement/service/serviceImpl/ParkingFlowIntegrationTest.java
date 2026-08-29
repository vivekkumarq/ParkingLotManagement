package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.domain.SlipStatus;
import com.netcracker.parkinglotmanagement.api.domain.SlotStatus;
import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.OccupancySummaryDTO;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlipDTO;
import com.netcracker.parkinglotmanagement.api.dto.SlotAvailabilityDTO;
import com.netcracker.parkinglotmanagement.api.dto.VehicleEntryRequest;
import com.netcracker.parkinglotmanagement.api.dto.VehicleExitRequest;
import com.netcracker.parkinglotmanagement.api.dto.VehicleExitResponse;
import com.netcracker.parkinglotmanagement.api.exception.DuplicateEntryException;
import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import com.netcracker.parkinglotmanagement.api.exception.NoSlotAvailableException;
import com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException;
import com.netcracker.parkinglotmanagement.api.service.ParkingSlipService;
import com.netcracker.parkinglotmanagement.api.service.ParkingSlotService;
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
 * End-to-end vehicle entry and exit against the real database: allocate, issue a
 * slip, price the stay, free the bay.
 *
 * <p>Entry and exit timestamps are supplied explicitly rather than read from the
 * clock, so the money in these assertions is deterministic.
 */
@IntegrationTest
class ParkingFlowIntegrationTest {

    private static final LocalDateTime ENTRY = LocalDateTime.of(2026, 3, 1, 8, 0);

    @Autowired
    private ParkingSlipService parkingSlipService;

    @Autowired
    private ParkingSlotService parkingSlotService;

    @Autowired
    private ParkingSlotRepository parkingSlotRepository;

    @Autowired
    private DatabaseFixture fixture;

    @AfterEach
    void tearDown() {
        fixture.clean();
    }

    private VehicleEntryRequest entry(UUID lotId, String vehicleNumber, VehicleType type, LocalDateTime at) {
        return VehicleEntryRequest.builder()
                .vehicleNumber(vehicleNumber)
                .vehicleType(type)
                .parkingLotId(lotId)
                .entryTime(at)
                .build();
    }

    @Test
    @DisplayName("check-in allocates a slot and opens a slip")
    void checkInIssuesASlip() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 2, VehicleType.CAR);

        ParkingSlipDTO slip = parkingSlipService.checkIn(
                entry(lot.lotId(), "KA01AB1234", VehicleType.CAR, ENTRY));

        assertThat(slip.getId()).isNotNull();
        assertThat(slip.getStatus()).isEqualTo(SlipStatus.ACTIVE);
        assertThat(slip.getActualEntryTime()).isEqualTo(ENTRY);
        assertThat(slip.getActualExitTime()).isNull();
        assertThat(slip.getParkingSlotId()).isEqualTo(lot.firstSlot().getId());
        assertThat(slip.getTotalCost()).isEqualByComparingTo("0.00");

        assertThat(parkingSlotRepository.findById(slip.getParkingSlotId()).orElseThrow().getStatus())
                .isEqualTo(SlotStatus.OCCUPIED);
    }

    @Test
    @DisplayName("registrations are normalised, so spacing and case do not create a second slip")
    void registrationsAreNormalised() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 2, VehicleType.CAR);

        ParkingSlipDTO slip = parkingSlipService.checkIn(
                entry(lot.lotId(), "ka 01-ab 1234", VehicleType.CAR, ENTRY));

        assertThat(slip.getVehicleNumber()).isEqualTo("KA01AB1234");
        assertThat(parkingSlipService.getActiveSlipByVehicleNumber("KA01AB1234").getId())
                .isEqualTo(slip.getId());
    }

    @Test
    @DisplayName("a vehicle that is already inside cannot check in again")
    void doubleEntryIsRejected() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 4, VehicleType.CAR);
        parkingSlipService.checkIn(entry(lot.lotId(), "KA01AB1234", VehicleType.CAR, ENTRY));

        assertThatThrownBy(() -> parkingSlipService.checkIn(
                entry(lot.lotId(), "KA01AB1234", VehicleType.CAR, ENTRY.plusMinutes(5))))
                .isInstanceOf(DuplicateEntryException.class)
                .hasMessageContaining("already has an open slip");

        assertThat(parkingSlotService.getFreeSlots(lot.lotId(), VehicleType.CAR))
                .as("the rejected entry must not have consumed a second slot")
                .hasSize(3);
    }

    @Test
    @DisplayName("checking into a full lot is refused")
    void fullLotRefusesEntry() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        parkingSlipService.checkIn(entry(lot.lotId(), "KA01AB1111", VehicleType.CAR, ENTRY));

        assertThatThrownBy(() -> parkingSlipService.checkIn(
                entry(lot.lotId(), "KA01AB2222", VehicleType.CAR, ENTRY)))
                .isInstanceOf(NoSlotAvailableException.class);
    }

    @Test
    @DisplayName("check-out prices the stay, closes the slip and frees the bay")
    void checkOutPricesAndReleases() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        ParkingSlipDTO slip = parkingSlipService.checkIn(
                entry(lot.lotId(), "KA01AB1234", VehicleType.CAR, ENTRY));

        // 27 h 30 m: one capped day (400.00) plus 4 started hours (50 + 3 x 30 = 140).
        LocalDateTime exit = LocalDateTime.of(2026, 3, 2, 11, 30);
        VehicleExitResponse response = parkingSlipService.checkOut(
                slip.getId(), VehicleExitRequest.builder().exitTime(exit).build());

        assertThat(response.getSlip().getStatus()).isEqualTo(SlipStatus.CLOSED);
        assertThat(response.getSlip().getActualExitTime()).isEqualTo(exit);
        assertThat(response.getSlip().getTotalCost()).isEqualByComparingTo("540.00");
        assertThat(response.getFee().getBasicCost()).isEqualByComparingTo("540.00");
        assertThat(response.getFee().getChargedDays()).isEqualTo(1L);
        assertThat(response.getFee().getChargedHours()).isEqualTo(4L);
        assertThat(response.getFee().getCurrency()).isEqualTo("INR");

        assertThat(parkingSlotRepository.findById(slip.getParkingSlotId()).orElseThrow().getStatus())
                .isEqualTo(SlotStatus.FREE);
    }

    @Test
    @DisplayName("the closed slip is persisted, not just returned")
    void closedSlipIsPersisted() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        ParkingSlipDTO slip = parkingSlipService.checkIn(
                entry(lot.lotId(), "KA01AB1234", VehicleType.CAR, ENTRY));
        parkingSlipService.checkOut(slip.getId(),
                VehicleExitRequest.builder().exitTime(ENTRY.plusHours(2)).build());

        ParkingSlipDTO reloaded = parkingSlipService.getSlipById(slip.getId());

        assertThat(reloaded.getStatus()).isEqualTo(SlipStatus.CLOSED);
        assertThat(reloaded.getTotalCost()).isEqualByComparingTo("80.00");
        assertThat(reloaded.getActualExitTime()).isEqualTo(ENTRY.plusHours(2));
    }

    @Test
    @DisplayName("a stay inside the grace period costs nothing")
    void gracePeriodStayIsFree() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        ParkingSlipDTO slip = parkingSlipService.checkIn(
                entry(lot.lotId(), "KA01AB1234", VehicleType.CAR, ENTRY));

        VehicleExitResponse response = parkingSlipService.checkOut(slip.getId(),
                VehicleExitRequest.builder().exitTime(ENTRY.plusMinutes(10)).build());

        assertThat(response.getFee().getWithinGracePeriod()).isTrue();
        assertThat(response.getSlip().getTotalCost()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("checking out twice is refused and does not re-price the stay")
    void doubleExitIsRejected() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        ParkingSlipDTO slip = parkingSlipService.checkIn(
                entry(lot.lotId(), "KA01AB1234", VehicleType.CAR, ENTRY));
        parkingSlipService.checkOut(slip.getId(),
                VehicleExitRequest.builder().exitTime(ENTRY.plusHours(2)).build());

        assertThatThrownBy(() -> parkingSlipService.checkOut(slip.getId(),
                VehicleExitRequest.builder().exitTime(ENTRY.plusHours(9)).build()))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("already closed");

        assertThat(parkingSlipService.getSlipById(slip.getId()).getTotalCost())
                .isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("an exit before the entry is refused")
    void exitBeforeEntryIsRejected() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        ParkingSlipDTO slip = parkingSlipService.checkIn(
                entry(lot.lotId(), "KA01AB1234", VehicleType.CAR, ENTRY));

        assertThatThrownBy(() -> parkingSlipService.checkOut(slip.getId(),
                VehicleExitRequest.builder().exitTime(ENTRY.minusHours(1)).build()))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("precedes");
    }

    @Test
    @DisplayName("a vehicle can check out by registration")
    void checkOutByRegistration() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        parkingSlipService.checkIn(entry(lot.lotId(), "KA01AB1234", VehicleType.CAR, ENTRY));

        VehicleExitResponse response = parkingSlipService.checkOutByVehicleNumber("ka01ab1234",
                VehicleExitRequest.builder().exitTime(ENTRY.plusHours(3)).build());

        assertThat(response.getSlip().getStatus()).isEqualTo(SlipStatus.CLOSED);
        assertThat(response.getSlip().getTotalCost()).isEqualByComparingTo("110.00"); // 50 + 2 x 30
    }

    @Test
    @DisplayName("a vehicle that is not inside cannot check out")
    void unknownVehicleCannotCheckOut() {
        assertThatThrownBy(() -> parkingSlipService.checkOutByVehicleNumber("KA01ZZ0000", null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no open parking slip");
    }

    @Test
    @DisplayName("the same vehicle may return after leaving")
    void vehicleMayReturn() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        ParkingSlipDTO first = parkingSlipService.checkIn(
                entry(lot.lotId(), "KA01AB1234", VehicleType.CAR, ENTRY));
        parkingSlipService.checkOut(first.getId(),
                VehicleExitRequest.builder().exitTime(ENTRY.plusHours(1)).build());

        ParkingSlipDTO second = parkingSlipService.checkIn(
                entry(lot.lotId(), "KA01AB1234", VehicleType.CAR, ENTRY.plusHours(2)));

        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(second.getStatus()).isEqualTo(SlipStatus.ACTIVE);
    }

    @Test
    @DisplayName("occupancy reflects what is actually parked")
    void occupancyTracksReality() {
        DatabaseFixture.Lot lot = fixture.createLot(2, 2, VehicleType.CAR); // 4 slots
        parkingSlipService.checkIn(entry(lot.lotId(), "KA01AB1111", VehicleType.CAR, ENTRY));
        parkingSlipService.checkIn(entry(lot.lotId(), "KA01AB2222", VehicleType.CAR, ENTRY));

        OccupancySummaryDTO occupancy = parkingSlotService.getOccupancy(lot.lotId());

        assertThat(occupancy.getTotalSlots()).isEqualTo(4);
        assertThat(occupancy.getOccupiedSlots()).isEqualTo(2);
        assertThat(occupancy.getFreeSlots()).isEqualTo(2);
        assertThat(occupancy.getReservedSlots()).isZero();
        assertThat(occupancy.getOccupancyRate()).isEqualByComparingTo("50.00");
        assertThat(occupancy.getActiveSlips()).isEqualTo(2);
    }

    @Test
    @DisplayName("availability breaks down per floor")
    void availabilityByFloor() {
        DatabaseFixture.Lot lot = fixture.createLot(2, 2, VehicleType.CAR);
        parkingSlipService.checkIn(entry(lot.lotId(), "KA01AB1111", VehicleType.CAR, ENTRY));

        List<SlotAvailabilityDTO> byFloor = parkingSlotService.getAvailabilityByFloor(lot.lotId());

        assertThat(byFloor).hasSize(2);
        assertThat(byFloor.get(0).getFloorNo()).isEqualTo(1);
        assertThat(byFloor.get(0).getOccupiedSlots()).isEqualTo(1);
        assertThat(byFloor.get(0).getFreeSlots()).isEqualTo(1);
        assertThat(byFloor.get(1).getFloorNo()).isEqualTo(2);
        assertThat(byFloor.get(1).getFreeSlots()).isEqualTo(2);
    }

    @Test
    @DisplayName("availability breaks down per vehicle type")
    void availabilityByVehicleType() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 2, VehicleType.CAR);
        fixture.createSlot(lot.slots().get(0).getFloorId(), 90, VehicleType.MOTORCYCLE);

        List<SlotAvailabilityDTO> byType = parkingSlotService.getAvailabilityByVehicleType(lot.lotId());

        assertThat(byType).extracting(SlotAvailabilityDTO::getVehicleType)
                .containsExactlyInAnyOrder(VehicleType.CAR, VehicleType.MOTORCYCLE);
        assertThat(byType).filteredOn(row -> row.getVehicleType() == VehicleType.CAR)
                .allSatisfy(row -> assertThat(row.getTotalSlots()).isEqualTo(2));
    }

    @Test
    @DisplayName("RSQL filters apply to the slip listing")
    void slipListingIsFilterable() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 2, VehicleType.CAR);
        ParkingSlipDTO open = parkingSlipService.checkIn(
                entry(lot.lotId(), "KA01AB1111", VehicleType.CAR, ENTRY));
        ParkingSlipDTO closed = parkingSlipService.checkIn(
                entry(lot.lotId(), "KA01AB2222", VehicleType.CAR, ENTRY));
        parkingSlipService.checkOut(closed.getId(),
                VehicleExitRequest.builder().exitTime(ENTRY.plusHours(5)).build());

        assertThat(parkingSlipService.getAllSlips("status==ACTIVE"))
                .extracting(ParkingSlipDTO::getId).containsExactly(open.getId());
        assertThat(parkingSlipService.getAllSlips("status==CLOSED;totalCost=gt=100"))
                .extracting(ParkingSlipDTO::getId).containsExactly(closed.getId());
        assertThat(parkingSlipService.getAllSlips("status==CLOSED;totalCost=gt=100000")).isEmpty();
    }
}

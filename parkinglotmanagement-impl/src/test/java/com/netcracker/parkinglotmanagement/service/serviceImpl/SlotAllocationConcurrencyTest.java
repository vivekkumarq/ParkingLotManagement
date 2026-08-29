package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.domain.SlotStatus;
import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlotDTO;
import com.netcracker.parkinglotmanagement.api.exception.NoSlotAvailableException;
import com.netcracker.parkinglotmanagement.api.service.SlotAllocationService;
import com.netcracker.parkinglotmanagement.service.repository.ParkingSlotRepository;
import com.netcracker.parkinglotmanagement.support.DatabaseFixture;
import com.netcracker.parkinglotmanagement.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The allocation contract under real contention, against a real database.
 *
 * <p>Mockito can prove the service reacts correctly when a compare-and-set reports
 * zero rows changed, but only an actual database can prove the compare-and-set is
 * itself atomic. These tests run many threads through the live
 * {@link SlotAllocationService} - transactions and all - and assert that the number
 * of successful allocations never exceeds the number of slots.
 */
@IntegrationTest
class SlotAllocationConcurrencyTest {

    private static final int THREADS = 16;

    @Autowired
    private SlotAllocationService slotAllocationService;

    @Autowired
    private ParkingSlotRepository parkingSlotRepository;

    @Autowired
    private DatabaseFixture fixture;

    @AfterEach
    void tearDown() {
        fixture.clean();
    }

    @Test
    @DisplayName("with a single free slot, exactly one of sixteen simultaneous arrivals wins")
    void onlyOneThreadCanTakeTheLastSlot() throws Exception {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);

        List<Outcome> outcomes = runConcurrently(THREADS,
                () -> slotAllocationService.allocate(lot.lotId(), VehicleType.CAR));

        List<UUID> allocatedSlotIds = successfulSlotIds(outcomes);

        assertThat(allocatedSlotIds)
                .as("exactly one arrival may be given the only slot")
                .hasSize(1);
        assertThat(outcomes.stream().filter(Outcome::failed).count())
                .isEqualTo(THREADS - 1L);
        assertThat(outcomes.stream().filter(Outcome::failed))
                .allSatisfy(outcome -> assertThat(outcome.error)
                        .isInstanceOf(NoSlotAvailableException.class));

        Optional<ParkingSlotDTO> slot = parkingSlotRepository.findById(lot.firstSlot().getId());
        assertThat(slot).isPresent();
        assertThat(slot.get().getStatus()).isEqualTo(SlotStatus.OCCUPIED);
        assertThat(slot.get().getVersion())
                .as("the version is bumped exactly once, by the single winning claim")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("no slot is ever handed to two vehicles when threads outnumber slots")
    void noSlotIsAllocatedTwice() throws Exception {
        // 6 slots, 16 threads: 6 winners, 10 losers, and no slot in two hands.
        DatabaseFixture.Lot lot = fixture.createLot(2, 3, VehicleType.CAR);

        List<Outcome> outcomes = runConcurrently(THREADS,
                () -> slotAllocationService.allocate(lot.lotId(), VehicleType.CAR));

        List<UUID> allocatedSlotIds = successfulSlotIds(outcomes);

        assertThat(allocatedSlotIds).hasSize(6);
        assertThat(allocatedSlotIds)
                .as("every winner got a different slot")
                .doesNotHaveDuplicates();

        assertThat(parkingSlotRepository.findAllocatableSlots(lot.lotId(), VehicleType.CAR, 100))
                .as("the lot is now full")
                .isEmpty();
    }

    @Test
    @DisplayName("when every slot is taken the lot reports itself full")
    void aFullLotIsReportedAsFull() throws Exception {
        DatabaseFixture.Lot lot = fixture.createLot(1, 2, VehicleType.CAR);

        slotAllocationService.allocate(lot.lotId(), VehicleType.CAR);
        slotAllocationService.allocate(lot.lotId(), VehicleType.CAR);

        assertThatThrownBy(() -> slotAllocationService.allocate(lot.lotId(), VehicleType.CAR))
                .isInstanceOf(NoSlotAvailableException.class);
    }

    @Test
    @DisplayName("a lot fills from the lowest floor and slot number upwards")
    void allocatesTheLowestSlotFirst() {
        DatabaseFixture.Lot lot = fixture.createLot(3, 2, VehicleType.CAR);

        ParkingSlotDTO first = slotAllocationService.allocate(lot.lotId(), VehicleType.CAR);
        ParkingSlotDTO second = slotAllocationService.allocate(lot.lotId(), VehicleType.CAR);
        ParkingSlotDTO third = slotAllocationService.allocate(lot.lotId(), VehicleType.CAR);

        // Floor 1 slots 1 and 2, then floor 2 slot 1.
        assertThat(first.getId()).isEqualTo(lot.slots().get(0).getId());
        assertThat(second.getId()).isEqualTo(lot.slots().get(1).getId());
        assertThat(third.getId()).isEqualTo(lot.slots().get(2).getId());
    }

    @Test
    @DisplayName("a vehicle is never given a slot cut for another type")
    void vehicleTypeIsRespected() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 2, VehicleType.CAR);

        assertThatThrownBy(() -> slotAllocationService.allocate(lot.lotId(), VehicleType.TRUCK))
                .isInstanceOf(NoSlotAvailableException.class)
                .hasMessageContaining("TRUCK");
    }

    @Test
    @DisplayName("a reserved slot is not offered to a walk-in")
    void reservedSlotsAreNotAllocatable() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);

        slotAllocationService.reserve(lot.lotId(), VehicleType.CAR);

        assertThatThrownBy(() -> slotAllocationService.allocate(lot.lotId(), VehicleType.CAR))
                .isInstanceOf(NoSlotAvailableException.class);
    }

    @Test
    @DisplayName("releasing a slot puts it back in circulation")
    void releasedSlotsBecomeAllocatableAgain() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);

        ParkingSlotDTO allocated = slotAllocationService.allocate(lot.lotId(), VehicleType.CAR);
        slotAllocationService.release(allocated.getId());

        ParkingSlotDTO reallocated = slotAllocationService.allocate(lot.lotId(), VehicleType.CAR);
        assertThat(reallocated.getId()).isEqualTo(allocated.getId());
    }

    /** Fires {@code threads} calls at the same instant and collects what each one did. */
    private static List<Outcome> runConcurrently(int threads, Callable<ParkingSlotDTO> action)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Outcome>> futures = new ArrayList<>(threads);

        try {
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    startGate.await();
                    try {
                        return Outcome.won(action.call());
                    } catch (Exception e) {
                        return Outcome.lost(e);
                    }
                }));
            }
            startGate.countDown();

            List<Outcome> outcomes = new ArrayList<>(threads);
            for (Future<Outcome> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    private static List<UUID> successfulSlotIds(List<Outcome> outcomes) {
        return Collections.unmodifiableList(outcomes.stream()
                .filter(outcome -> !outcome.failed())
                .map(outcome -> outcome.slot.getId())
                .collect(Collectors.toList()));
    }

    private static final class Outcome {

        private final ParkingSlotDTO slot;
        private final Exception error;

        private Outcome(ParkingSlotDTO slot, Exception error) {
            this.slot = slot;
            this.error = error;
        }

        static Outcome won(ParkingSlotDTO slot) {
            return new Outcome(slot, null);
        }

        static Outcome lost(Exception error) {
            return new Outcome(null, error);
        }

        boolean failed() {
            return error != null;
        }
    }
}

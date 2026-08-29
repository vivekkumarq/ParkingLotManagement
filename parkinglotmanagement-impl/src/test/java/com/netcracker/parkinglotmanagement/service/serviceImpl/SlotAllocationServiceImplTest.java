package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.domain.SlotStatus;
import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlotDTO;
import com.netcracker.parkinglotmanagement.api.exception.NoSlotAvailableException;
import com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException;
import com.netcracker.parkinglotmanagement.api.exception.SlotUnavailableException;
import com.netcracker.parkinglotmanagement.service.repository.ParkingSlotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotAllocationServiceImplTest {

    private static final UUID LOT = UUID.randomUUID();

    @Mock
    private ParkingSlotRepository parkingSlotRepository;

    @InjectMocks
    private SlotAllocationServiceImpl slotAllocationService;

    private static ParkingSlotDTO slot(int number, SlotStatus status, long version) {
        return ParkingSlotDTO.builder()
                .id(UUID.randomUUID())
                .floorId(UUID.randomUUID())
                .slotNumber(number)
                .charges(0)
                .vehicleType(VehicleType.CAR)
                .status(status)
                .version(version)
                .build();
    }

    @Test
    @DisplayName("takes the first candidate the repository offers and marks it OCCUPIED")
    void allocatesTheFirstCandidate() {
        ParkingSlotDTO first = slot(1, SlotStatus.FREE, 7L);
        ParkingSlotDTO second = slot(2, SlotStatus.FREE, 0L);
        when(parkingSlotRepository.findAllocatableSlots(LOT, VehicleType.CAR, SlotAllocationServiceImpl.CANDIDATE_BATCH_SIZE))
                .thenReturn(Arrays.asList(first, second));
        when(parkingSlotRepository.compareAndSetStatus(first.getId(), SlotStatus.FREE, SlotStatus.OCCUPIED, 7L))
                .thenReturn(true);

        ParkingSlotDTO allocated = slotAllocationService.allocate(LOT, VehicleType.CAR);

        assertThat(allocated.getId()).isEqualTo(first.getId());
        assertThat(allocated.getStatus()).isEqualTo(SlotStatus.OCCUPIED);
        assertThat(allocated.getVersion()).isEqualTo(8L);
        verify(parkingSlotRepository, never())
                .compareAndSetStatus(eq(second.getId()), any(), any(), anyLong());
    }

    @Test
    @DisplayName("the claim carries the version that was read, so a stale slot cannot be taken")
    void claimIsGuardedByTheObservedVersion() {
        ParkingSlotDTO candidate = slot(1, SlotStatus.FREE, 42L);
        when(parkingSlotRepository.findAllocatableSlots(any(), any(), anyInt()))
                .thenReturn(Collections.singletonList(candidate));
        when(parkingSlotRepository.compareAndSetStatus(candidate.getId(), SlotStatus.FREE, SlotStatus.OCCUPIED, 42L))
                .thenReturn(true);

        slotAllocationService.allocate(LOT, VehicleType.CAR);

        verify(parkingSlotRepository)
                .compareAndSetStatus(candidate.getId(), SlotStatus.FREE, SlotStatus.OCCUPIED, 42L);
    }

    @Test
    @DisplayName("when a rival claims the first candidate, the search falls through to the next")
    void fallsThroughToTheNextCandidateWhenOneIsTaken() {
        ParkingSlotDTO taken = slot(1, SlotStatus.FREE, 1L);
        ParkingSlotDTO available = slot(2, SlotStatus.FREE, 3L);
        when(parkingSlotRepository.findAllocatableSlots(any(), any(), anyInt()))
                .thenReturn(Arrays.asList(taken, available));
        when(parkingSlotRepository.compareAndSetStatus(taken.getId(), SlotStatus.FREE, SlotStatus.OCCUPIED, 1L))
                .thenReturn(false);
        when(parkingSlotRepository.compareAndSetStatus(available.getId(), SlotStatus.FREE, SlotStatus.OCCUPIED, 3L))
                .thenReturn(true);

        ParkingSlotDTO allocated = slotAllocationService.allocate(LOT, VehicleType.CAR);

        assertThat(allocated.getId()).isEqualTo(available.getId());
    }

    @Test
    @DisplayName("an empty lot raises NoSlotAvailable without attempting any claim")
    void fullLotIsReported() {
        when(parkingSlotRepository.findAllocatableSlots(any(), any(), anyInt()))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> slotAllocationService.allocate(LOT, VehicleType.TRUCK))
                .isInstanceOf(NoSlotAvailableException.class)
                .hasMessageContaining("TRUCK");

        verify(parkingSlotRepository, never()).compareAndSetStatus(any(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("sustained contention gives up after a bounded number of rounds rather than spinning")
    void givesUpAfterTheRoundLimit() {
        ParkingSlotDTO contended = slot(1, SlotStatus.FREE, 0L);
        when(parkingSlotRepository.findAllocatableSlots(any(), any(), anyInt()))
                .thenReturn(Collections.singletonList(contended));
        when(parkingSlotRepository.compareAndSetStatus(any(), any(), any(), anyLong()))
                .thenReturn(false);

        assertThatThrownBy(() -> slotAllocationService.allocate(LOT, VehicleType.CAR))
                .isInstanceOf(NoSlotAvailableException.class)
                .hasMessageContaining("contended");

        verify(parkingSlotRepository, times(SlotAllocationServiceImpl.MAX_SEARCH_ROUNDS))
                .findAllocatableSlots(any(), any(), anyInt());
    }

    @Test
    @DisplayName("reserve() uses the same search but leaves the slot RESERVED")
    void reserveLeavesTheSlotReserved() {
        ParkingSlotDTO candidate = slot(1, SlotStatus.FREE, 0L);
        when(parkingSlotRepository.findAllocatableSlots(any(), any(), anyInt()))
                .thenReturn(Collections.singletonList(candidate));
        when(parkingSlotRepository.compareAndSetStatus(candidate.getId(), SlotStatus.FREE, SlotStatus.RESERVED, 0L))
                .thenReturn(true);

        ParkingSlotDTO reserved = slotAllocationService.reserve(LOT, VehicleType.CAR);

        assertThat(reserved.getStatus()).isEqualTo(SlotStatus.RESERVED);
    }

    @Test
    @DisplayName("claiming a reserved slot moves it to OCCUPIED")
    void claimReservedMovesToOccupied() {
        ParkingSlotDTO reserved = slot(1, SlotStatus.RESERVED, 5L);
        when(parkingSlotRepository.findById(reserved.getId())).thenReturn(Optional.of(reserved));
        when(parkingSlotRepository.compareAndSetStatus(reserved.getId(), SlotStatus.RESERVED, SlotStatus.OCCUPIED, 5L))
                .thenReturn(true);

        ParkingSlotDTO claimed = slotAllocationService.claimReserved(reserved.getId());

        assertThat(claimed.getStatus()).isEqualTo(SlotStatus.OCCUPIED);
        assertThat(claimed.getVersion()).isEqualTo(6L);
    }

    @Test
    @DisplayName("claiming a slot that is not RESERVED is refused")
    void claimReservedRejectsAFreeSlot() {
        ParkingSlotDTO free = slot(1, SlotStatus.FREE, 0L);
        when(parkingSlotRepository.findById(free.getId())).thenReturn(Optional.of(free));

        assertThatThrownBy(() -> slotAllocationService.claimReserved(free.getId()))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("not RESERVED");
    }

    @Test
    @DisplayName("claiming a slot that changed under us is refused")
    void claimReservedDetectsALostRace() {
        ParkingSlotDTO reserved = slot(1, SlotStatus.RESERVED, 5L);
        when(parkingSlotRepository.findById(reserved.getId())).thenReturn(Optional.of(reserved));
        when(parkingSlotRepository.compareAndSetStatus(any(), any(), any(), anyLong())).thenReturn(false);

        assertThatThrownBy(() -> slotAllocationService.claimReserved(reserved.getId()))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("changed state");
    }

    @Test
    @DisplayName("release is unconditional so an exit always frees the bay")
    void releaseIsUnconditional() {
        UUID slotId = UUID.randomUUID();
        when(parkingSlotRepository.setStatus(slotId, SlotStatus.FREE)).thenReturn(1);

        slotAllocationService.release(slotId);

        verify(parkingSlotRepository).setStatus(slotId, SlotStatus.FREE);
    }

    @Test
    void releasingAnUnknownSlotIsAnError() {
        UUID slotId = UUID.randomUUID();
        when(parkingSlotRepository.setStatus(slotId, SlotStatus.FREE)).thenReturn(0);

        assertThatThrownBy(() -> slotAllocationService.release(slotId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

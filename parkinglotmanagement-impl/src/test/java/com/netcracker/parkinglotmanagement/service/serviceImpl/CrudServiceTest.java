package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.domain.SlotStatus;
import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.BlockDTO;
import com.netcracker.parkinglotmanagement.api.dto.CustomerDTO;
import com.netcracker.parkinglotmanagement.api.dto.FloorDTO;
import com.netcracker.parkinglotmanagement.api.dto.ParkingLotDTO;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlotDTO;
import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException;
import com.netcracker.parkinglotmanagement.service.repository.BlockRepository;
import com.netcracker.parkinglotmanagement.service.repository.CustomerRepository;
import com.netcracker.parkinglotmanagement.service.repository.FloorRepository;
import com.netcracker.parkinglotmanagement.service.repository.ParkingLotRepository;
import com.netcracker.parkinglotmanagement.service.repository.ParkingSlotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the four services that already existed plus the new slot service.
 *
 * <p>The recurring theme is the pair of bugs these used to share: a "not found"
 * that returned null instead of raising, and an update path that called an
 * insert-only {@code save()} and so could never succeed.
 */
class CrudServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-01T08:00:00Z"), ZoneOffset.UTC);

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("ParkingLotService")
    class ParkingLots {

        @Mock
        private ParkingLotRepository repository;

        @InjectMocks
        private ParkingLotServiceImpl service;

        private ParkingLotDTO lot() {
            return new ParkingLotDTO(UUID.randomUUID(), 3, "12 MG Road", 77.5946, 12.9716);
        }

        @Test
        void createDelegatesToInsert() {
            ParkingLotDTO lot = lot();
            when(repository.insert(lot)).thenReturn(lot);

            assertThat(service.createParkingLot(lot)).isSameAs(lot);
        }

        @Test
        void getByIdReturnsTheLot() {
            ParkingLotDTO lot = lot();
            when(repository.findById(lot.getId())).thenReturn(Optional.of(lot));

            assertThat(service.getParkingLotById(lot.getId())).isSameAs(lot);
        }

        @Test
        @DisplayName("an unknown id raises instead of returning null")
        void getByIdRaisesWhenMissing() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getParkingLotById(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(id.toString());
        }

        @Test
        @DisplayName("update issues an UPDATE, not a second INSERT")
        void updateUsesUpdate() {
            ParkingLotDTO lot = lot();
            when(repository.update(lot)).thenReturn(1);

            assertThat(service.updateParkingLot(lot)).isSameAs(lot);
            verify(repository, never()).insert(any());
        }

        @Test
        void updatingAnUnknownLotRaises() {
            ParkingLotDTO lot = lot();
            when(repository.update(lot)).thenReturn(0);

            assertThatThrownBy(() -> service.updateParkingLot(lot))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void updateWithoutAnIdRaises() {
            ParkingLotDTO lot = lot();
            lot.setId(null);

            assertThatThrownBy(() -> service.updateParkingLot(lot))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("id is required");
        }

        @Test
        void deletingAnUnknownLotRaises() {
            UUID id = UUID.randomUUID();
            when(repository.deleteById(id)).thenReturn(0);

            assertThatThrownBy(() -> service.deleteParkingLot(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void listReturnsEverything() {
            List<ParkingLotDTO> lots = Arrays.asList(lot(), lot());
            when(repository.findAll()).thenReturn(lots);

            assertThat(service.getAllParkingLots()).hasSize(2);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("BlockService")
    class Blocks {

        @Mock
        private BlockRepository repository;

        @InjectMocks
        private BlockServiceImpl service;

        private BlockDTO block() {
            return new BlockDTO(UUID.randomUUID(), UUID.randomUUID(), "A1", 4);
        }

        @Test
        @DisplayName("create returns the stored block so the caller learns its id")
        void createReturnsTheStoredBlock() {
            BlockDTO block = block();
            when(repository.insert(block)).thenReturn(block);

            assertThat(service.createBlock(block).getId()).isEqualTo(block.getId());
        }

        @Test
        void getByIdRaisesWhenMissing() {
            UUID blockId = UUID.randomUUID();
            UUID lotId = UUID.randomUUID();
            when(repository.findById(blockId, lotId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getBlockById(blockId, lotId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(lotId.toString());
        }

        @Test
        void deleteRaisesWhenTheBlockIsNotInThatLot() {
            UUID blockId = UUID.randomUUID();
            UUID lotId = UUID.randomUUID();
            when(repository.delete(blockId, lotId)).thenReturn(0);

            assertThatThrownBy(() -> service.deleteBlock(blockId, lotId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void updateUsesUpdate() {
            BlockDTO block = block();
            when(repository.update(block)).thenReturn(1);

            assertThat(service.updateBlock(block)).isSameAs(block);
            verify(repository, never()).insert(any());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("FloorService")
    class Floors {

        @Mock
        private FloorRepository repository;

        @InjectMocks
        private FloorServiceImpl service;

        private FloorDTO floor() {
            return new FloorDTO(UUID.randomUUID(), UUID.randomUUID(), 1, 50);
        }

        @Test
        void createDelegatesToInsert() {
            FloorDTO floor = floor();
            when(repository.insert(floor)).thenReturn(floor);

            assertThat(service.createFloor(floor)).isSameAs(floor);
        }

        @Test
        @DisplayName("update issues an UPDATE - it used to call an insert-only save()")
        void updateUsesUpdate() {
            FloorDTO floor = floor();
            when(repository.update(floor)).thenReturn(1);

            assertThat(service.updateFloor(floor)).isSameAs(floor);
            verify(repository, never()).insert(any());
        }

        @Test
        void getByIdRaisesWhenMissing() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getFloorById(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void deletingAnUnknownFloorRaises() {
            UUID id = UUID.randomUUID();
            when(repository.deleteById(id)).thenReturn(0);

            assertThatThrownBy(() -> service.deleteFloor(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("CustomerService")
    class Customers {

        @Mock
        private CustomerRepository repository;

        @InjectMocks
        private CustomerServiceImpl service;

        private CustomerDTO customer() {
            return new CustomerDTO(UUID.randomUUID(), "KA01AB1234", "9876543210",
                    "Asha Rao", "asha.rao@example.com");
        }

        @Test
        @DisplayName("the RSQL filter is passed down instead of being discarded")
        void filterIsPassedToTheRepository() {
            when(repository.findAll("name==Asha")).thenReturn(Collections.singletonList(customer()));

            assertThat(service.getAllCustomers("name==Asha")).hasSize(1);
            verify(repository).findAll("name==Asha");
        }

        @Test
        void aNullFilterListsEverything() {
            when(repository.findAll(null)).thenReturn(Arrays.asList(customer(), customer()));

            assertThat(service.getAllCustomers(null)).hasSize(2);
        }

        @Test
        void getByIdRaisesWhenMissing() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCustomerById(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void updateUsesUpdate() {
            CustomerDTO customer = customer();
            when(repository.update(customer)).thenReturn(1);

            assertThat(service.updateCustomer(customer)).isSameAs(customer);
            verify(repository, never()).insert(any());
        }

        @Test
        void deletingAnUnknownCustomerRaises() {
            UUID id = UUID.randomUUID();
            when(repository.deleteById(id)).thenReturn(0);

            assertThatThrownBy(() -> service.deleteCustomer(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("ParkingSlotService")
    class Slots {

        @Mock
        private ParkingSlotRepository repository;

        private ParkingSlotServiceImpl service;

        private ParkingSlotServiceImpl service() {
            if (service == null) {
                service = new ParkingSlotServiceImpl(repository, FIXED_CLOCK);
            }
            return service;
        }

        private ParkingSlotDTO slot(SlotStatus status) {
            return ParkingSlotDTO.builder()
                    .id(UUID.randomUUID())
                    .floorId(UUID.randomUUID())
                    .slotNumber(1)
                    .charges(0)
                    .vehicleType(VehicleType.CAR)
                    .status(status)
                    .version(0L)
                    .build();
        }

        @Test
        @DisplayName("a FREE slot can be withdrawn from service")
        void freeSlotCanBeWithdrawn() {
            ParkingSlotDTO slot = slot(SlotStatus.FREE);
            when(repository.findById(slot.getId())).thenReturn(Optional.of(slot));
            when(repository.compareAndSetStatus(slot.getId(), SlotStatus.FREE, SlotStatus.OUT_OF_SERVICE, 0L))
                    .thenReturn(true);

            assertThat(service().setOutOfService(slot.getId(), true).getStatus())
                    .isEqualTo(SlotStatus.OUT_OF_SERVICE);
        }

        @Test
        @DisplayName("an occupied bay cannot be withdrawn from under the vehicle in it")
        void occupiedSlotCannotBeWithdrawn() {
            ParkingSlotDTO slot = slot(SlotStatus.OCCUPIED);
            when(repository.findById(slot.getId())).thenReturn(Optional.of(slot));

            assertThatThrownBy(() -> service().setOutOfService(slot.getId(), true))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("OCCUPIED");

            verify(repository, never()).compareAndSetStatus(any(), any(), any(), anyLong());
        }

        @Test
        @DisplayName("withdrawing a slot that is already out of service is a no-op")
        void withdrawingTwiceIsIdempotent() {
            ParkingSlotDTO slot = slot(SlotStatus.OUT_OF_SERVICE);
            when(repository.findById(slot.getId())).thenReturn(Optional.of(slot));

            assertThat(service().setOutOfService(slot.getId(), true).getStatus())
                    .isEqualTo(SlotStatus.OUT_OF_SERVICE);
            verify(repository, never()).compareAndSetStatus(any(), any(), any(), anyLong());
        }

        @Test
        void getByIdRaisesWhenMissing() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().getSlotById(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void deletingAnUnknownSlotRaises() {
            UUID id = UUID.randomUUID();
            when(repository.deleteById(id)).thenReturn(0);

            assertThatThrownBy(() -> service().deleteSlot(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("the occupancy rate of an empty lot is 0.00, not a division by zero")
        void percentageOfAnEmptyLot() {
            assertThat(ParkingSlotServiceImpl.percentage(0, 0)).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("the occupancy rate is rounded to two decimals")
        void percentageIsRounded() {
            assertThat(ParkingSlotServiceImpl.percentage(98, 150)).isEqualByComparingTo("65.33");
            assertThat(ParkingSlotServiceImpl.percentage(1, 3)).isEqualByComparingTo("33.33");
            assertThat(ParkingSlotServiceImpl.percentage(2, 3)).isEqualByComparingTo("66.67");
        }
    }
}

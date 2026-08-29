package com.netcracker.parkinglotmanagement.support;

import com.netcracker.parkinglotmanagement.api.domain.SlotStatus;
import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.BlockDTO;
import com.netcracker.parkinglotmanagement.api.dto.FloorDTO;
import com.netcracker.parkinglotmanagement.api.dto.ParkingLotDTO;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlotDTO;
import com.netcracker.parkinglotmanagement.service.repository.BlockRepository;
import com.netcracker.parkinglotmanagement.service.repository.FloorRepository;
import com.netcracker.parkinglotmanagement.service.repository.ParkingLotRepository;
import com.netcracker.parkinglotmanagement.service.repository.ParkingSlotRepository;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.netcracker.parkinglotmanagement.data.Tables.BLOCK;
import static com.netcracker.parkinglotmanagement.data.Tables.CUSTOMER;
import static com.netcracker.parkinglotmanagement.data.Tables.FLOOR;
import static com.netcracker.parkinglotmanagement.data.Tables.PARKING_LOT;
import static com.netcracker.parkinglotmanagement.data.Tables.PARKING_SLIP;
import static com.netcracker.parkinglotmanagement.data.Tables.PARKING_SLOT;
import static com.netcracker.parkinglotmanagement.data.Tables.RESERVATION;

/**
 * Builds a small car park in the test database and wipes it between tests.
 *
 * <p>Everything is written through the production repositories, so the fixture also
 * exercises the insert paths and would fail if the schema and the generated jOOQ
 * classes ever drifted apart.
 */
@Component
public class DatabaseFixture {

    private final DSLContext dsl;
    private final ParkingLotRepository parkingLotRepository;
    private final BlockRepository blockRepository;
    private final FloorRepository floorRepository;
    private final ParkingSlotRepository parkingSlotRepository;

    public DatabaseFixture(DSLContext dsl,
                           ParkingLotRepository parkingLotRepository,
                           BlockRepository blockRepository,
                           FloorRepository floorRepository,
                           ParkingSlotRepository parkingSlotRepository) {
        this.dsl = dsl;
        this.parkingLotRepository = parkingLotRepository;
        this.blockRepository = blockRepository;
        this.floorRepository = floorRepository;
        this.parkingSlotRepository = parkingSlotRepository;
    }

    /** Deletes every row, respecting foreign-key order. */
    public void clean() {
        dsl.deleteFrom(PARKING_SLIP).execute();
        dsl.deleteFrom(RESERVATION).execute();
        dsl.deleteFrom(PARKING_SLOT).execute();
        dsl.deleteFrom(FLOOR).execute();
        dsl.deleteFrom(BLOCK).execute();
        dsl.deleteFrom(PARKING_LOT).execute();
        dsl.deleteFrom(CUSTOMER).execute();
    }

    public ParkingLotDTO createLot() {
        return parkingLotRepository.insert(
                new ParkingLotDTO(UUID.randomUUID(), 1, "12 MG Road, Bengaluru", 77.5946, 12.9716));
    }

    public BlockDTO createBlock(UUID lotId, String code) {
        return blockRepository.insert(new BlockDTO(UUID.randomUUID(), lotId, code, 1));
    }

    public FloorDTO createFloor(UUID blockId, int floorNo, int slots) {
        return floorRepository.insert(new FloorDTO(UUID.randomUUID(), blockId, floorNo, slots));
    }

    public ParkingSlotDTO createSlot(UUID floorId, int slotNumber, VehicleType vehicleType) {
        return parkingSlotRepository.insert(ParkingSlotDTO.builder()
                .floorId(floorId)
                .slotNumber(slotNumber)
                .charges(0)
                .vehicleType(vehicleType)
                .status(SlotStatus.FREE)
                .build());
    }

    /**
     * A lot with one block, {@code floors} floors and {@code slotsPerFloor} CAR slots
     * on each. Floor numbers start at 1 and slot numbers restart on every floor, so
     * ordering by floor then slot number is observable.
     */
    public Lot createLot(int floors, int slotsPerFloor, VehicleType vehicleType) {
        ParkingLotDTO lot = createLot();
        BlockDTO block = createBlock(lot.getId(), "A");
        List<ParkingSlotDTO> slots = new ArrayList<>();
        for (int floorNo = 1; floorNo <= floors; floorNo++) {
            FloorDTO floor = createFloor(block.getId(), floorNo, slotsPerFloor);
            for (int slotNumber = 1; slotNumber <= slotsPerFloor; slotNumber++) {
                slots.add(createSlot(floor.getId(), slotNumber, vehicleType));
            }
        }
        return new Lot(lot, block, slots);
    }

    /** A created car park and the slots inside it. */
    public static final class Lot {

        private final ParkingLotDTO lot;
        private final BlockDTO block;
        private final List<ParkingSlotDTO> slots;

        Lot(ParkingLotDTO lot, BlockDTO block, List<ParkingSlotDTO> slots) {
            this.lot = lot;
            this.block = block;
            this.slots = slots;
        }

        public ParkingLotDTO lot() {
            return lot;
        }

        public UUID lotId() {
            return lot.getId();
        }

        public BlockDTO block() {
            return block;
        }

        public List<ParkingSlotDTO> slots() {
            return slots;
        }

        public ParkingSlotDTO firstSlot() {
            return slots.get(0);
        }
    }
}

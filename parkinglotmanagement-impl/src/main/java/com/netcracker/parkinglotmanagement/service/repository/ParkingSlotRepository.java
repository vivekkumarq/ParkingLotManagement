package com.netcracker.parkinglotmanagement.service.repository;

import com.netcracker.parkinglotmanagement.api.domain.SlotStatus;
import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlotDTO;
import com.netcracker.parkinglotmanagement.api.dto.SlotAvailabilityDTO;
import com.netcracker.parkinglotmanagement.service.rsql.RsqlFilter;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.netcracker.parkinglotmanagement.data.Tables.BLOCK;
import static com.netcracker.parkinglotmanagement.data.Tables.FLOOR;
import static com.netcracker.parkinglotmanagement.data.Tables.PARKING_SLIP;
import static com.netcracker.parkinglotmanagement.data.Tables.PARKING_SLOT;

/**
 * Slot persistence, including the compare-and-set primitives the allocator is
 * built on.
 */
@Repository
public class ParkingSlotRepository {

    private static final Map<String, Field<?>> FILTERABLE = filterable();

    private final DSLContext dsl;

    public ParkingSlotRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public ParkingSlotDTO insert(ParkingSlotDTO dto) {
        UUID id = dto.getId() != null ? dto.getId() : UUID.randomUUID();
        SlotStatus status = dto.getStatus() != null ? dto.getStatus() : SlotStatus.FREE;
        dsl.insertInto(PARKING_SLOT)
                .set(PARKING_SLOT.ID, id)
                .set(PARKING_SLOT.FLOOR_ID, dto.getFloorId())
                .set(PARKING_SLOT.SLOT_NUMBER, dto.getSlotNumber())
                .set(PARKING_SLOT.CHARGES, dto.getCharges() != null ? dto.getCharges() : 0)
                .set(PARKING_SLOT.VEHICLE_TYPE, dto.getVehicleType().name())
                .set(PARKING_SLOT.STATUS, status.name())
                .set(PARKING_SLOT.VERSION, 0L)
                .execute();
        dto.setId(id);
        dto.setStatus(status);
        dto.setVersion(0L);
        return dto;
    }

    /** Updates the descriptive columns only; status and version belong to the allocator. */
    public int update(ParkingSlotDTO dto) {
        return dsl.update(PARKING_SLOT)
                .set(PARKING_SLOT.FLOOR_ID, dto.getFloorId())
                .set(PARKING_SLOT.SLOT_NUMBER, dto.getSlotNumber())
                .set(PARKING_SLOT.CHARGES, dto.getCharges() != null ? dto.getCharges() : 0)
                .set(PARKING_SLOT.VEHICLE_TYPE, dto.getVehicleType().name())
                .where(PARKING_SLOT.ID.eq(dto.getId()))
                .execute();
    }

    public Optional<ParkingSlotDTO> findById(UUID id) {
        return dsl.select(PARKING_SLOT.fields())
                .from(PARKING_SLOT)
                .where(PARKING_SLOT.ID.eq(id))
                .fetchOptional(ParkingSlotRepository::toDto);
    }

    public List<ParkingSlotDTO> findAll(String rsqlFilter) {
        Condition condition = RsqlFilter.toCondition(rsqlFilter, FILTERABLE);
        return dsl.select(PARKING_SLOT.fields())
                .from(PARKING_SLOT)
                .where(condition)
                .orderBy(PARKING_SLOT.SLOT_NUMBER)
                .fetch(ParkingSlotRepository::toDto);
    }

    public int deleteById(UUID id) {
        return dsl.deleteFrom(PARKING_SLOT).where(PARKING_SLOT.ID.eq(id)).execute();
    }

    /**
     * Candidate slots for allocation, cheapest-to-reach first: lowest floor, then
     * block code, then slot number.
     *
     * <p>Returned in one query rather than walking floors and asking per floor, so
     * that a lot with many floors still costs a single round trip.
     *
     * @param limit how many candidates to bring back; the allocator walks them in
     *              order until one compare-and-set succeeds
     */
    public List<ParkingSlotDTO> findAllocatableSlots(UUID parkingLotId, VehicleType vehicleType, int limit) {
        return dsl.select(PARKING_SLOT.fields())
                .from(PARKING_SLOT)
                .join(FLOOR).on(FLOOR.ID.eq(PARKING_SLOT.FLOOR_ID))
                .join(BLOCK).on(BLOCK.ID.eq(FLOOR.BLOCK_ID))
                .where(BLOCK.PARKING_LOT_ID.eq(parkingLotId))
                .and(PARKING_SLOT.VEHICLE_TYPE.eq(vehicleType.name()))
                .and(PARKING_SLOT.STATUS.eq(SlotStatus.FREE.name()))
                .orderBy(FLOOR.FLOOR_NO.asc(), BLOCK.BLOCK_CODE.asc(), PARKING_SLOT.SLOT_NUMBER.asc())
                .limit(limit)
                .fetch(ParkingSlotRepository::toDto);
    }

    /**
     * The allocation primitive: moves a slot from one status to another only if it
     * is still in the status and at the version the caller observed.
     *
     * <p>This is what makes concurrent check-ins safe. Two threads that both read
     * the same FREE slot will both issue this UPDATE; the database serialises them,
     * the first bumps the version and the second matches zero rows and is told to
     * try the next candidate.
     *
     * @return true when exactly one row changed
     */
    public boolean compareAndSetStatus(UUID slotId, SlotStatus expected, SlotStatus next, long expectedVersion) {
        return dsl.update(PARKING_SLOT)
                .set(PARKING_SLOT.STATUS, next.name())
                .set(PARKING_SLOT.VERSION, PARKING_SLOT.VERSION.plus(1L))
                .where(PARKING_SLOT.ID.eq(slotId))
                .and(PARKING_SLOT.STATUS.eq(expected.name()))
                .and(PARKING_SLOT.VERSION.eq(expectedVersion))
                .execute() == 1;
    }

    /**
     * Unconditional transition, used when releasing a slot: an exit must always
     * succeed, whatever state the slot drifted into.
     */
    public int setStatus(UUID slotId, SlotStatus next) {
        return dsl.update(PARKING_SLOT)
                .set(PARKING_SLOT.STATUS, next.name())
                .set(PARKING_SLOT.VERSION, PARKING_SLOT.VERSION.plus(1L))
                .where(PARKING_SLOT.ID.eq(slotId))
                .execute();
    }

    /** Availability counts for a lot, one row per floor. */
    public List<SlotAvailabilityDTO> availabilityByFloor(UUID parkingLotId) {
        return dsl.select(BLOCK.PARKING_LOT_ID, BLOCK.ID, BLOCK.BLOCK_CODE, FLOOR.ID, FLOOR.FLOOR_NO,
                        countWhere(SlotStatus.FREE), countWhere(SlotStatus.RESERVED),
                        countWhere(SlotStatus.OCCUPIED), countWhere(SlotStatus.OUT_OF_SERVICE), DSL.count())
                .from(PARKING_SLOT)
                .join(FLOOR).on(FLOOR.ID.eq(PARKING_SLOT.FLOOR_ID))
                .join(BLOCK).on(BLOCK.ID.eq(FLOOR.BLOCK_ID))
                .where(BLOCK.PARKING_LOT_ID.eq(parkingLotId))
                .groupBy(BLOCK.PARKING_LOT_ID, BLOCK.ID, BLOCK.BLOCK_CODE, FLOOR.ID, FLOOR.FLOOR_NO)
                .orderBy(FLOOR.FLOOR_NO.asc(), BLOCK.BLOCK_CODE.asc())
                // Positional access: BLOCK.ID and FLOOR.ID both come back as a column
                // called "id", so resolving by field name would be ambiguous.
                .fetch(record -> SlotAvailabilityDTO.builder()
                        .parkingLotId(record.get(0, UUID.class))
                        .blockId(record.get(1, UUID.class))
                        .blockCode(record.get(2, String.class))
                        .floorId(record.get(3, UUID.class))
                        .floorNo(record.get(4, Integer.class))
                        .freeSlots(record.get(5, Integer.class))
                        .reservedSlots(record.get(6, Integer.class))
                        .occupiedSlots(record.get(7, Integer.class))
                        .outOfServiceSlots(record.get(8, Integer.class))
                        .totalSlots(record.get(9, Integer.class))
                        .build());
    }

    /** Availability counts for a lot, one row per block. */
    public List<SlotAvailabilityDTO> availabilityByBlock(UUID parkingLotId) {
        return dsl.select(BLOCK.PARKING_LOT_ID, BLOCK.ID, BLOCK.BLOCK_CODE,
                        countWhere(SlotStatus.FREE), countWhere(SlotStatus.RESERVED),
                        countWhere(SlotStatus.OCCUPIED), countWhere(SlotStatus.OUT_OF_SERVICE), DSL.count())
                .from(PARKING_SLOT)
                .join(FLOOR).on(FLOOR.ID.eq(PARKING_SLOT.FLOOR_ID))
                .join(BLOCK).on(BLOCK.ID.eq(FLOOR.BLOCK_ID))
                .where(BLOCK.PARKING_LOT_ID.eq(parkingLotId))
                .groupBy(BLOCK.PARKING_LOT_ID, BLOCK.ID, BLOCK.BLOCK_CODE)
                .orderBy(BLOCK.BLOCK_CODE.asc())
                .fetch(record -> SlotAvailabilityDTO.builder()
                        .parkingLotId(record.get(0, UUID.class))
                        .blockId(record.get(1, UUID.class))
                        .blockCode(record.get(2, String.class))
                        .freeSlots(record.get(3, Integer.class))
                        .reservedSlots(record.get(4, Integer.class))
                        .occupiedSlots(record.get(5, Integer.class))
                        .outOfServiceSlots(record.get(6, Integer.class))
                        .totalSlots(record.get(7, Integer.class))
                        .build());
    }

    /** Availability counts for a lot, one row per vehicle type. */
    public List<SlotAvailabilityDTO> availabilityByVehicleType(UUID parkingLotId) {
        return dsl.select(PARKING_SLOT.VEHICLE_TYPE,
                        countWhere(SlotStatus.FREE), countWhere(SlotStatus.RESERVED),
                        countWhere(SlotStatus.OCCUPIED), countWhere(SlotStatus.OUT_OF_SERVICE), DSL.count())
                .from(PARKING_SLOT)
                .join(FLOOR).on(FLOOR.ID.eq(PARKING_SLOT.FLOOR_ID))
                .join(BLOCK).on(BLOCK.ID.eq(FLOOR.BLOCK_ID))
                .where(BLOCK.PARKING_LOT_ID.eq(parkingLotId))
                .groupBy(PARKING_SLOT.VEHICLE_TYPE)
                .orderBy(PARKING_SLOT.VEHICLE_TYPE.asc())
                .fetch(record -> SlotAvailabilityDTO.builder()
                        .parkingLotId(parkingLotId)
                        .vehicleType(VehicleType.from(record.get(0, String.class)))
                        .freeSlots(record.get(1, Integer.class))
                        .reservedSlots(record.get(2, Integer.class))
                        .occupiedSlots(record.get(3, Integer.class))
                        .outOfServiceSlots(record.get(4, Integer.class))
                        .totalSlots(record.get(5, Integer.class))
                        .build());
    }

    /** Whole-lot counts in a single row: free, reserved, occupied, out of service, total. */
    public SlotAvailabilityDTO totalsForLot(UUID parkingLotId) {
        Record record = dsl.select(countWhere(SlotStatus.FREE), countWhere(SlotStatus.RESERVED),
                        countWhere(SlotStatus.OCCUPIED), countWhere(SlotStatus.OUT_OF_SERVICE), DSL.count())
                .from(PARKING_SLOT)
                .join(FLOOR).on(FLOOR.ID.eq(PARKING_SLOT.FLOOR_ID))
                .join(BLOCK).on(BLOCK.ID.eq(FLOOR.BLOCK_ID))
                .where(BLOCK.PARKING_LOT_ID.eq(parkingLotId))
                .fetchOne();

        if (record == null) {
            return SlotAvailabilityDTO.builder()
                    .parkingLotId(parkingLotId)
                    .freeSlots(0).reservedSlots(0).occupiedSlots(0).outOfServiceSlots(0).totalSlots(0)
                    .build();
        }
        return SlotAvailabilityDTO.builder()
                .parkingLotId(parkingLotId)
                .freeSlots(record.get(0, Integer.class))
                .reservedSlots(record.get(1, Integer.class))
                .occupiedSlots(record.get(2, Integer.class))
                .outOfServiceSlots(record.get(3, Integer.class))
                .totalSlots(record.get(4, Integer.class))
                .build();
    }

    /** How many slips are currently open against slots in this lot. */
    public int countActiveSlipsInLot(UUID parkingLotId) {
        Integer count = dsl.selectCount()
                .from(PARKING_SLIP)
                .join(PARKING_SLOT).on(PARKING_SLOT.ID.eq(PARKING_SLIP.PARKING_SLOT_ID))
                .join(FLOOR).on(FLOOR.ID.eq(PARKING_SLOT.FLOOR_ID))
                .join(BLOCK).on(BLOCK.ID.eq(FLOOR.BLOCK_ID))
                .where(BLOCK.PARKING_LOT_ID.eq(parkingLotId))
                .and(PARKING_SLIP.STATUS.eq(com.netcracker.parkinglotmanagement.api.domain.SlipStatus.ACTIVE.name()))
                .fetchOne(0, Integer.class);
        return count != null ? count : 0;
    }

    /** True when the slot belongs to a floor in a block of this lot. */
    public boolean slotBelongsToLot(UUID slotId, UUID parkingLotId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(PARKING_SLOT)
                        .join(FLOOR).on(FLOOR.ID.eq(PARKING_SLOT.FLOOR_ID))
                        .join(BLOCK).on(BLOCK.ID.eq(FLOOR.BLOCK_ID))
                        .where(PARKING_SLOT.ID.eq(slotId))
                        .and(BLOCK.PARKING_LOT_ID.eq(parkingLotId)));
    }

    private static Field<Integer> countWhere(SlotStatus status) {
        return DSL.count().filterWhere(PARKING_SLOT.STATUS.eq(status.name()));
    }

    static ParkingSlotDTO toDto(Record record) {
        return ParkingSlotDTO.builder()
                .id(record.get(PARKING_SLOT.ID))
                .floorId(record.get(PARKING_SLOT.FLOOR_ID))
                .slotNumber(record.get(PARKING_SLOT.SLOT_NUMBER))
                .charges(record.get(PARKING_SLOT.CHARGES))
                .vehicleType(VehicleType.from(record.get(PARKING_SLOT.VEHICLE_TYPE)))
                .status(SlotStatus.from(record.get(PARKING_SLOT.STATUS)))
                .version(record.get(PARKING_SLOT.VERSION))
                .build();
    }

    private static Map<String, Field<?>> filterable() {
        Map<String, Field<?>> fields = new LinkedHashMap<>();
        fields.put("id", PARKING_SLOT.ID);
        fields.put("floorId", PARKING_SLOT.FLOOR_ID);
        fields.put("slotNumber", PARKING_SLOT.SLOT_NUMBER);
        fields.put("charges", PARKING_SLOT.CHARGES);
        fields.put("vehicleType", PARKING_SLOT.VEHICLE_TYPE);
        fields.put("status", PARKING_SLOT.STATUS);
        return Collections.unmodifiableMap(fields);
    }
}

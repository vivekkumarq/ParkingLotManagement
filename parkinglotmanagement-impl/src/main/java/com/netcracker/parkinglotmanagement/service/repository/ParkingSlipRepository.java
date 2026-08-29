package com.netcracker.parkinglotmanagement.service.repository;

import com.netcracker.parkinglotmanagement.api.domain.SlipStatus;
import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlipDTO;
import com.netcracker.parkinglotmanagement.service.rsql.RsqlFilter;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.Record4;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

/** Slip persistence plus the aggregates the reporting service reads. */
@Repository
public class ParkingSlipRepository {

    private static final Map<String, Field<?>> FILTERABLE = filterable();

    private final DSLContext dsl;

    public ParkingSlipRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public ParkingSlipDTO insert(ParkingSlipDTO dto) {
        UUID id = dto.getId() != null ? dto.getId() : UUID.randomUUID();
        dsl.insertInto(PARKING_SLIP)
                .set(PARKING_SLIP.ID, id)
                .set(PARKING_SLIP.PARKING_SLOT_RESERVATION_ID, dto.getParkingSlotReservationId())
                .set(PARKING_SLIP.PARKING_SLOT_ID, dto.getParkingSlotId())
                .set(PARKING_SLIP.CUSTOMER_ID, dto.getCustomerId())
                .set(PARKING_SLIP.VEHICLE_NUMBER, dto.getVehicleNumber())
                .set(PARKING_SLIP.VEHICLE_TYPE, dto.getVehicleType().name())
                .set(PARKING_SLIP.ACTUAL_ENTRY_TIME, dto.getActualEntryTime())
                .set(PARKING_SLIP.ACTUAL_EXIT_TIME, dto.getActualExitTime())
                .set(PARKING_SLIP.BASIC_COST, zeroIfNull(dto.getBasicCost()))
                .set(PARKING_SLIP.PENALTY, zeroIfNull(dto.getPenalty()))
                .set(PARKING_SLIP.TOTAL_COST, zeroIfNull(dto.getTotalCost()))
                .set(PARKING_SLIP.STATUS, dto.getStatus().name())
                .execute();
        dto.setId(id);
        return dto;
    }

    /**
     * Closes a slip. The WHERE clause also matches on ACTIVE, so a duplicate exit
     * request cannot re-price an already closed stay.
     *
     * @return true when the slip was open and is now closed
     */
    public boolean close(UUID slipId,
                        LocalDateTime exitTime,
                        BigDecimal basicCost,
                        BigDecimal penalty,
                        BigDecimal totalCost) {
        return dsl.update(PARKING_SLIP)
                .set(PARKING_SLIP.ACTUAL_EXIT_TIME, exitTime)
                .set(PARKING_SLIP.BASIC_COST, basicCost)
                .set(PARKING_SLIP.PENALTY, penalty)
                .set(PARKING_SLIP.TOTAL_COST, totalCost)
                .set(PARKING_SLIP.STATUS, SlipStatus.CLOSED.name())
                .where(PARKING_SLIP.ID.eq(slipId))
                .and(PARKING_SLIP.STATUS.eq(SlipStatus.ACTIVE.name()))
                .execute() == 1;
    }

    public Optional<ParkingSlipDTO> findById(UUID id) {
        return dsl.select(PARKING_SLIP.fields())
                .from(PARKING_SLIP)
                .where(PARKING_SLIP.ID.eq(id))
                .fetchOptional(ParkingSlipRepository::toDto);
    }

    public Optional<ParkingSlipDTO> findActiveByVehicleNumber(String vehicleNumber) {
        return dsl.select(PARKING_SLIP.fields())
                .from(PARKING_SLIP)
                .where(PARKING_SLIP.VEHICLE_NUMBER.eq(vehicleNumber))
                .and(PARKING_SLIP.STATUS.eq(SlipStatus.ACTIVE.name()))
                .orderBy(PARKING_SLIP.ACTUAL_ENTRY_TIME.desc())
                .limit(1)
                .fetchOptional(ParkingSlipRepository::toDto);
    }

    public List<ParkingSlipDTO> findAll(String rsqlFilter) {
        Condition condition = RsqlFilter.toCondition(rsqlFilter, FILTERABLE);
        return dsl.select(PARKING_SLIP.fields())
                .from(PARKING_SLIP)
                .where(condition)
                .orderBy(PARKING_SLIP.ACTUAL_ENTRY_TIME.desc())
                .fetch(ParkingSlipRepository::toDto);
    }

    public int deleteById(UUID id) {
        return dsl.deleteFrom(PARKING_SLIP).where(PARKING_SLIP.ID.eq(id)).execute();
    }

    // ------------------------------------------------------------------
    // Reporting aggregates. Each is a single statement over an indexed range.
    // ------------------------------------------------------------------

    /**
     * Slip count and the three money totals for stays that ended in
     * {@code [from, to)}.
     *
     * @return count, basic, penalty, total - never null, zeros when the range is empty
     */
    public Record4<Integer, BigDecimal, BigDecimal, BigDecimal> revenueTotals(UUID parkingLotId, LocalDateTime from, LocalDateTime to) {
        return dsl.select(DSL.count(),
                        DSL.coalesce(DSL.sum(PARKING_SLIP.BASIC_COST), BigDecimal.ZERO),
                        DSL.coalesce(DSL.sum(PARKING_SLIP.PENALTY), BigDecimal.ZERO),
                        DSL.coalesce(DSL.sum(PARKING_SLIP.TOTAL_COST), BigDecimal.ZERO))
                .from(PARKING_SLIP)
                .where(closedInRange(from, to)).and(inLot(parkingLotId))
                .fetchOne();
    }

    /** Total revenue grouped by vehicle type over {@code [from, to)}. */
    public List<Record2<String, BigDecimal>> revenueByVehicleType(UUID parkingLotId, LocalDateTime from, LocalDateTime to) {
        return dsl.select(PARKING_SLIP.VEHICLE_TYPE,
                        DSL.coalesce(DSL.sum(PARKING_SLIP.TOTAL_COST), BigDecimal.ZERO))
                .from(PARKING_SLIP)
                .where(closedInRange(from, to)).and(inLot(parkingLotId))
                .groupBy(PARKING_SLIP.VEHICLE_TYPE)
                .orderBy(PARKING_SLIP.VEHICLE_TYPE.asc())
                .fetch();
    }

    /**
     * Entry and exit timestamps of every stay that ended in {@code [from, to)}.
     *
     * <p>Duration statistics are derived from these in Java rather than with
     * {@code TIMESTAMPDIFF} or {@code AGE()}, because no portable SQL spelling
     * covers both PostgreSQL and H2. It is still one query, not one per slip.
     */
    public List<Record2<LocalDateTime, LocalDateTime>> closedStayWindows(UUID parkingLotId, LocalDateTime from, LocalDateTime to) {
        return dsl.select(PARKING_SLIP.ACTUAL_ENTRY_TIME, PARKING_SLIP.ACTUAL_EXIT_TIME)
                .from(PARKING_SLIP)
                .where(closedInRange(from, to)).and(inLot(parkingLotId))
                .and(PARKING_SLIP.ACTUAL_ENTRY_TIME.isNotNull())
                .fetch();
    }

    /**
     * Every stay that overlaps {@code [from, to)}, open ones included, so the
     * caller can bucket them into an occupancy series with one round trip.
     */
    public List<Record2<LocalDateTime, LocalDateTime>> staysOverlapping(UUID parkingLotId, LocalDateTime from, LocalDateTime to) {
        return dsl.select(PARKING_SLIP.ACTUAL_ENTRY_TIME, PARKING_SLIP.ACTUAL_EXIT_TIME)
                .from(PARKING_SLIP)
                .where(PARKING_SLIP.ACTUAL_ENTRY_TIME.lessThan(to))
                .and(PARKING_SLIP.ACTUAL_EXIT_TIME.isNull()
                        .or(PARKING_SLIP.ACTUAL_EXIT_TIME.greaterThan(from)))
                .and(inLot(parkingLotId))
                .fetch();
    }

    /** Arrivals per hour of day over {@code [from, to)}. */
    public List<Record2<Integer, Integer>> entriesByHourOfDay(UUID parkingLotId, LocalDateTime from, LocalDateTime to) {
        Field<Integer> hourOfDay = DSL.extract(PARKING_SLIP.ACTUAL_ENTRY_TIME, org.jooq.DatePart.HOUR);
        return dsl.select(hourOfDay, DSL.count())
                .from(PARKING_SLIP)
                .where(PARKING_SLIP.ACTUAL_ENTRY_TIME.greaterOrEqual(from))
                .and(PARKING_SLIP.ACTUAL_ENTRY_TIME.lessThan(to))
                .and(inLot(parkingLotId))
                .groupBy(hourOfDay)
                .fetch();
    }

    /** Daily takings over {@code [from, to)}, keyed by the exit timestamp. */
    public List<Record2<LocalDateTime, BigDecimal>> closedSlipTotals(UUID parkingLotId, LocalDateTime from, LocalDateTime to) {
        return dsl.select(PARKING_SLIP.ACTUAL_EXIT_TIME,
                        DSL.coalesce(PARKING_SLIP.TOTAL_COST, BigDecimal.ZERO))
                .from(PARKING_SLIP)
                .where(closedInRange(from, to)).and(inLot(parkingLotId))
                .fetch();
    }

    /**
     * Restricts a slip query to one car park, or to all of them when
     * {@code parkingLotId} is null.
     *
     * <p>Written as a semi-join rather than an inner join so that callers keep a
     * single-table projection and a legacy slip with no slot reference is not
     * silently dropped from an unscoped report.
     */
    private static Condition inLot(UUID parkingLotId) {
        if (parkingLotId == null) {
            return DSL.noCondition();
        }
        return PARKING_SLIP.PARKING_SLOT_ID.in(
                DSL.select(PARKING_SLOT.ID)
                        .from(PARKING_SLOT)
                        .join(FLOOR).on(FLOOR.ID.eq(PARKING_SLOT.FLOOR_ID))
                        .join(BLOCK).on(BLOCK.ID.eq(FLOOR.BLOCK_ID))
                        .where(BLOCK.PARKING_LOT_ID.eq(parkingLotId)));
    }

    private static Condition closedInRange(LocalDateTime from, LocalDateTime to) {
        return PARKING_SLIP.STATUS.eq(SlipStatus.CLOSED.name())
                .and(PARKING_SLIP.ACTUAL_EXIT_TIME.greaterOrEqual(from))
                .and(PARKING_SLIP.ACTUAL_EXIT_TIME.lessThan(to));
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO.setScale(2);
    }

    static ParkingSlipDTO toDto(Record record) {
        return ParkingSlipDTO.builder()
                .id(record.get(PARKING_SLIP.ID))
                .parkingSlotReservationId(record.get(PARKING_SLIP.PARKING_SLOT_RESERVATION_ID))
                .parkingSlotId(record.get(PARKING_SLIP.PARKING_SLOT_ID))
                .customerId(record.get(PARKING_SLIP.CUSTOMER_ID))
                .vehicleNumber(record.get(PARKING_SLIP.VEHICLE_NUMBER))
                .vehicleType(VehicleType.from(record.get(PARKING_SLIP.VEHICLE_TYPE)))
                .actualEntryTime(record.get(PARKING_SLIP.ACTUAL_ENTRY_TIME))
                .actualExitTime(record.get(PARKING_SLIP.ACTUAL_EXIT_TIME))
                .basicCost(record.get(PARKING_SLIP.BASIC_COST))
                .penalty(record.get(PARKING_SLIP.PENALTY))
                .totalCost(record.get(PARKING_SLIP.TOTAL_COST))
                .status(SlipStatus.from(record.get(PARKING_SLIP.STATUS)))
                .build();
    }

    private static Map<String, Field<?>> filterable() {
        Map<String, Field<?>> fields = new LinkedHashMap<>();
        fields.put("id", PARKING_SLIP.ID);
        fields.put("parkingSlotId", PARKING_SLIP.PARKING_SLOT_ID);
        fields.put("customerId", PARKING_SLIP.CUSTOMER_ID);
        fields.put("vehicleNumber", PARKING_SLIP.VEHICLE_NUMBER);
        fields.put("vehicleType", PARKING_SLIP.VEHICLE_TYPE);
        fields.put("status", PARKING_SLIP.STATUS);
        fields.put("basicCost", PARKING_SLIP.BASIC_COST);
        fields.put("totalCost", PARKING_SLIP.TOTAL_COST);
        fields.put("actualEntryTime", PARKING_SLIP.ACTUAL_ENTRY_TIME);
        fields.put("actualExitTime", PARKING_SLIP.ACTUAL_EXIT_TIME);
        return Collections.unmodifiableMap(fields);
    }
}

package com.netcracker.parkinglotmanagement.service.repository;

import com.netcracker.parkinglotmanagement.api.domain.ReservationStatus;
import com.netcracker.parkinglotmanagement.api.dto.ReservationDTO;
import com.netcracker.parkinglotmanagement.service.rsql.RsqlFilter;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.netcracker.parkinglotmanagement.data.Tables.RESERVATION;

@Repository
public class ReservationRepository {

    private static final Map<String, Field<?>> FILTERABLE = filterable();

    private final DSLContext dsl;

    public ReservationRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public ReservationDTO insert(ReservationDTO dto) {
        UUID id = dto.getId() != null ? dto.getId() : UUID.randomUUID();
        dsl.insertInto(RESERVATION)
                .set(RESERVATION.ID, id)
                .set(RESERVATION.CUSTOMER_ID, dto.getCustomerId())
                .set(RESERVATION.PARKING_SLOT_ID, dto.getParkingSlotId())
                .set(RESERVATION.VEHICLE_NUMBER, dto.getVehicleNumber())
                .set(RESERVATION.START_TIMESTAMP, dto.getStartTimestamp())
                .set(RESERVATION.END_TIMESTAMP, dto.getEndTimestamp())
                .set(RESERVATION.DURATION_IN_HOURS, dto.getDurationInHours())
                .set(RESERVATION.BOOKING_DATE, dto.getBookingDate())
                .set(RESERVATION.STATUS, dto.getStatus().name())
                .set(RESERVATION.CREATED_AT, dto.getCreatedAt())
                .execute();
        dto.setId(id);
        return dto;
    }

    public Optional<ReservationDTO> findById(UUID id) {
        return dsl.select(RESERVATION.fields())
                .from(RESERVATION)
                .where(RESERVATION.ID.eq(id))
                .fetchOptional(ReservationRepository::toDto);
    }

    public List<ReservationDTO> findAll(String rsqlFilter) {
        Condition condition = RsqlFilter.toCondition(rsqlFilter, FILTERABLE);
        return dsl.select(RESERVATION.fields())
                .from(RESERVATION)
                .where(condition)
                .orderBy(RESERVATION.START_TIMESTAMP.desc())
                .fetch(ReservationRepository::toDto);
    }

    /**
     * Bookings that still hold {@code slotId} and whose window overlaps
     * {@code [start, end)}.
     *
     * <p>Windows are half-open, so two intervals overlap exactly when
     * {@code start < otherEnd AND otherStart < end}. A booking that ends at 12:00
     * and one that starts at 12:00 therefore do not conflict.
     *
     * <p>Only BOOKED and CLAIMED bookings hold a slot; cancelled and expired ones
     * are ignored.
     */
    public List<ReservationDTO> findOverlapping(UUID slotId, LocalDateTime start, LocalDateTime end) {
        return dsl.select(RESERVATION.fields())
                .from(RESERVATION)
                .where(RESERVATION.PARKING_SLOT_ID.eq(slotId))
                .and(RESERVATION.STATUS.in(ReservationStatus.BOOKED.name(), ReservationStatus.CLAIMED.name()))
                .and(RESERVATION.START_TIMESTAMP.lessThan(end))
                .and(RESERVATION.END_TIMESTAMP.greaterThan(start))
                .orderBy(RESERVATION.START_TIMESTAMP.asc())
                .fetch(ReservationRepository::toDto);
    }

    /** BOOKED reservations whose window closed before {@code asOf} without being claimed. */
    public List<ReservationDTO> findExpirable(LocalDateTime asOf) {
        return dsl.select(RESERVATION.fields())
                .from(RESERVATION)
                .where(RESERVATION.STATUS.eq(ReservationStatus.BOOKED.name()))
                .and(RESERVATION.END_TIMESTAMP.lessThan(asOf))
                .orderBy(RESERVATION.END_TIMESTAMP.asc())
                .fetch(ReservationRepository::toDto);
    }

    /**
     * Moves a reservation between lifecycle states, but only from the state the
     * caller observed - so a cancel racing a claim cannot both succeed.
     *
     * @return true when exactly one row changed
     */
    public boolean compareAndSetStatus(UUID id, ReservationStatus expected, ReservationStatus next) {
        return dsl.update(RESERVATION)
                .set(RESERVATION.STATUS, next.name())
                .where(RESERVATION.ID.eq(id))
                .and(RESERVATION.STATUS.eq(expected.name()))
                .execute() == 1;
    }

    public int deleteById(UUID id) {
        return dsl.deleteFrom(RESERVATION).where(RESERVATION.ID.eq(id)).execute();
    }

    static ReservationDTO toDto(Record record) {
        return ReservationDTO.builder()
                .id(record.get(RESERVATION.ID))
                .customerId(record.get(RESERVATION.CUSTOMER_ID))
                .parkingSlotId(record.get(RESERVATION.PARKING_SLOT_ID))
                .vehicleNumber(record.get(RESERVATION.VEHICLE_NUMBER))
                .startTimestamp(record.get(RESERVATION.START_TIMESTAMP))
                .endTimestamp(record.get(RESERVATION.END_TIMESTAMP))
                .durationInHours(record.get(RESERVATION.DURATION_IN_HOURS))
                .bookingDate(record.get(RESERVATION.BOOKING_DATE))
                .status(ReservationStatus.from(record.get(RESERVATION.STATUS)))
                .createdAt(record.get(RESERVATION.CREATED_AT))
                .build();
    }

    private static Map<String, Field<?>> filterable() {
        Map<String, Field<?>> fields = new LinkedHashMap<>();
        fields.put("id", RESERVATION.ID);
        fields.put("customerId", RESERVATION.CUSTOMER_ID);
        fields.put("parkingSlotId", RESERVATION.PARKING_SLOT_ID);
        fields.put("vehicleNumber", RESERVATION.VEHICLE_NUMBER);
        fields.put("status", RESERVATION.STATUS);
        fields.put("durationInHours", RESERVATION.DURATION_IN_HOURS);
        fields.put("startTimestamp", RESERVATION.START_TIMESTAMP);
        fields.put("endTimestamp", RESERVATION.END_TIMESTAMP);
        fields.put("bookingDate", RESERVATION.BOOKING_DATE);
        return Collections.unmodifiableMap(fields);
    }
}

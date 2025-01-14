/*
 * This file is generated by jOOQ.
 */
package com.netcracker.parkinglotmanagement.data.tables.records;


import com.netcracker.parkinglotmanagement.data.tables.ReservationTable;

import java.time.LocalDateTime;
import java.util.UUID;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record6;
import org.jooq.Row6;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ReservationRecord extends UpdatableRecordImpl<ReservationRecord> implements Record6<UUID, LocalDateTime, UUID, Integer, UUID, byte[]> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>RESERVATION.ID</code>.
     */
    public ReservationRecord setId(UUID value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>RESERVATION.ID</code>.
     */
    public UUID getId() {
        return (UUID) get(0);
    }

    /**
     * Setter for <code>RESERVATION.BOOKING_DATE</code>.
     */
    public ReservationRecord setBookingDate(LocalDateTime value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>RESERVATION.BOOKING_DATE</code>.
     */
    public LocalDateTime getBookingDate() {
        return (LocalDateTime) get(1);
    }

    /**
     * Setter for <code>RESERVATION.CUSTOMER_ID</code>.
     */
    public ReservationRecord setCustomerId(UUID value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>RESERVATION.CUSTOMER_ID</code>.
     */
    public UUID getCustomerId() {
        return (UUID) get(2);
    }

    /**
     * Setter for <code>RESERVATION.DURATION_IN_HOURS</code>.
     */
    public ReservationRecord setDurationInHours(Integer value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>RESERVATION.DURATION_IN_HOURS</code>.
     */
    public Integer getDurationInHours() {
        return (Integer) get(3);
    }

    /**
     * Setter for <code>RESERVATION.PARKING_SLOT_ID</code>.
     */
    public ReservationRecord setParkingSlotId(UUID value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>RESERVATION.PARKING_SLOT_ID</code>.
     */
    public UUID getParkingSlotId() {
        return (UUID) get(4);
    }

    /**
     * Setter for <code>RESERVATION.START_TIMESTAMP</code>.
     */
    public ReservationRecord setStartTimestamp(byte[] value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>RESERVATION.START_TIMESTAMP</code>.
     */
    public byte[] getStartTimestamp() {
        return (byte[]) get(5);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<UUID> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record6 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row6<UUID, LocalDateTime, UUID, Integer, UUID, byte[]> fieldsRow() {
        return (Row6) super.fieldsRow();
    }

    @Override
    public Row6<UUID, LocalDateTime, UUID, Integer, UUID, byte[]> valuesRow() {
        return (Row6) super.valuesRow();
    }

    @Override
    public Field<UUID> field1() {
        return ReservationTable.RESERVATION.ID;
    }

    @Override
    public Field<LocalDateTime> field2() {
        return ReservationTable.RESERVATION.BOOKING_DATE;
    }

    @Override
    public Field<UUID> field3() {
        return ReservationTable.RESERVATION.CUSTOMER_ID;
    }

    @Override
    public Field<Integer> field4() {
        return ReservationTable.RESERVATION.DURATION_IN_HOURS;
    }

    @Override
    public Field<UUID> field5() {
        return ReservationTable.RESERVATION.PARKING_SLOT_ID;
    }

    @Override
    public Field<byte[]> field6() {
        return ReservationTable.RESERVATION.START_TIMESTAMP;
    }

    @Override
    public UUID component1() {
        return getId();
    }

    @Override
    public LocalDateTime component2() {
        return getBookingDate();
    }

    @Override
    public UUID component3() {
        return getCustomerId();
    }

    @Override
    public Integer component4() {
        return getDurationInHours();
    }

    @Override
    public UUID component5() {
        return getParkingSlotId();
    }

    @Override
    public byte[] component6() {
        return getStartTimestamp();
    }

    @Override
    public UUID value1() {
        return getId();
    }

    @Override
    public LocalDateTime value2() {
        return getBookingDate();
    }

    @Override
    public UUID value3() {
        return getCustomerId();
    }

    @Override
    public Integer value4() {
        return getDurationInHours();
    }

    @Override
    public UUID value5() {
        return getParkingSlotId();
    }

    @Override
    public byte[] value6() {
        return getStartTimestamp();
    }

    @Override
    public ReservationRecord value1(UUID value) {
        setId(value);
        return this;
    }

    @Override
    public ReservationRecord value2(LocalDateTime value) {
        setBookingDate(value);
        return this;
    }

    @Override
    public ReservationRecord value3(UUID value) {
        setCustomerId(value);
        return this;
    }

    @Override
    public ReservationRecord value4(Integer value) {
        setDurationInHours(value);
        return this;
    }

    @Override
    public ReservationRecord value5(UUID value) {
        setParkingSlotId(value);
        return this;
    }

    @Override
    public ReservationRecord value6(byte[] value) {
        setStartTimestamp(value);
        return this;
    }

    @Override
    public ReservationRecord values(UUID value1, LocalDateTime value2, UUID value3, Integer value4, UUID value5, byte[] value6) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ReservationRecord
     */
    public ReservationRecord() {
        super(ReservationTable.RESERVATION);
    }

    /**
     * Create a detached, initialised ReservationRecord
     */
    public ReservationRecord(UUID id, LocalDateTime bookingDate, UUID customerId, Integer durationInHours, UUID parkingSlotId, byte[] startTimestamp) {
        super(ReservationTable.RESERVATION);

        setId(id);
        setBookingDate(bookingDate);
        setCustomerId(customerId);
        setDurationInHours(durationInHours);
        setParkingSlotId(parkingSlotId);
        setStartTimestamp(startTimestamp);
    }
}

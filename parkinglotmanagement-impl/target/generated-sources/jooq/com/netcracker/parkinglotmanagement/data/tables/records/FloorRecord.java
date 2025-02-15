/*
 * This file is generated by jOOQ.
 */
package com.netcracker.parkinglotmanagement.data.tables.records;


import com.netcracker.parkinglotmanagement.data.tables.FloorTable;

import java.util.UUID;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class FloorRecord extends UpdatableRecordImpl<FloorRecord> implements Record4<UUID, UUID, Integer, Integer> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>FLOOR.ID</code>.
     */
    public FloorRecord setId(UUID value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>FLOOR.ID</code>.
     */
    public UUID getId() {
        return (UUID) get(0);
    }

    /**
     * Setter for <code>FLOOR.BLOCK_ID</code>.
     */
    public FloorRecord setBlockId(UUID value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>FLOOR.BLOCK_ID</code>.
     */
    public UUID getBlockId() {
        return (UUID) get(1);
    }

    /**
     * Setter for <code>FLOOR.FLOOR_NO</code>.
     */
    public FloorRecord setFloorNo(Integer value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>FLOOR.FLOOR_NO</code>.
     */
    public Integer getFloorNo() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>FLOOR.NUMBER_OF_SLOTS</code>.
     */
    public FloorRecord setNumberOfSlots(Integer value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>FLOOR.NUMBER_OF_SLOTS</code>.
     */
    public Integer getNumberOfSlots() {
        return (Integer) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<UUID> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row4<UUID, UUID, Integer, Integer> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    public Row4<UUID, UUID, Integer, Integer> valuesRow() {
        return (Row4) super.valuesRow();
    }

    @Override
    public Field<UUID> field1() {
        return FloorTable.FLOOR.ID;
    }

    @Override
    public Field<UUID> field2() {
        return FloorTable.FLOOR.BLOCK_ID;
    }

    @Override
    public Field<Integer> field3() {
        return FloorTable.FLOOR.FLOOR_NO;
    }

    @Override
    public Field<Integer> field4() {
        return FloorTable.FLOOR.NUMBER_OF_SLOTS;
    }

    @Override
    public UUID component1() {
        return getId();
    }

    @Override
    public UUID component2() {
        return getBlockId();
    }

    @Override
    public Integer component3() {
        return getFloorNo();
    }

    @Override
    public Integer component4() {
        return getNumberOfSlots();
    }

    @Override
    public UUID value1() {
        return getId();
    }

    @Override
    public UUID value2() {
        return getBlockId();
    }

    @Override
    public Integer value3() {
        return getFloorNo();
    }

    @Override
    public Integer value4() {
        return getNumberOfSlots();
    }

    @Override
    public FloorRecord value1(UUID value) {
        setId(value);
        return this;
    }

    @Override
    public FloorRecord value2(UUID value) {
        setBlockId(value);
        return this;
    }

    @Override
    public FloorRecord value3(Integer value) {
        setFloorNo(value);
        return this;
    }

    @Override
    public FloorRecord value4(Integer value) {
        setNumberOfSlots(value);
        return this;
    }

    @Override
    public FloorRecord values(UUID value1, UUID value2, Integer value3, Integer value4) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached FloorRecord
     */
    public FloorRecord() {
        super(FloorTable.FLOOR);
    }

    /**
     * Create a detached, initialised FloorRecord
     */
    public FloorRecord(UUID id, UUID blockId, Integer floorNo, Integer numberOfSlots) {
        super(FloorTable.FLOOR);

        setId(id);
        setBlockId(blockId);
        setFloorNo(floorNo);
        setNumberOfSlots(numberOfSlots);
    }
}

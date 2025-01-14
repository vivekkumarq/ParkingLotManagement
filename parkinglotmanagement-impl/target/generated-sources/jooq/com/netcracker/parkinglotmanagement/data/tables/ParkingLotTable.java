/*
 * This file is generated by jOOQ.
 */
package com.netcracker.parkinglotmanagement.data.tables;


import com.netcracker.parkinglotmanagement.data.DefaultSchema;
import com.netcracker.parkinglotmanagement.data.Keys;
import com.netcracker.parkinglotmanagement.data.tables.records.ParkingLotRecord;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row5;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ParkingLotTable extends TableImpl<ParkingLotRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>PARKING_LOT</code>
     */
    public static final ParkingLotTable PARKING_LOT = new ParkingLotTable();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ParkingLotRecord> getRecordType() {
        return ParkingLotRecord.class;
    }

    /**
     * The column <code>PARKING_LOT.ID</code>.
     */
    public final TableField<ParkingLotRecord, UUID> ID = createField(DSL.name("ID"), SQLDataType.UUID.nullable(false), this, "");

    /**
     * The column <code>PARKING_LOT.ADDRESS</code>.
     */
    public final TableField<ParkingLotRecord, String> ADDRESS = createField(DSL.name("ADDRESS"), SQLDataType.VARCHAR(255), this, "");

    /**
     * The column <code>PARKING_LOT.LATITUDE</code>.
     */
    public final TableField<ParkingLotRecord, Double> LATITUDE = createField(DSL.name("LATITUDE"), SQLDataType.DOUBLE, this, "");

    /**
     * The column <code>PARKING_LOT.LONGITUDE</code>.
     */
    public final TableField<ParkingLotRecord, Double> LONGITUDE = createField(DSL.name("LONGITUDE"), SQLDataType.DOUBLE, this, "");

    /**
     * The column <code>PARKING_LOT.NUMBER_OF_BLOCKS</code>.
     */
    public final TableField<ParkingLotRecord, Integer> NUMBER_OF_BLOCKS = createField(DSL.name("NUMBER_OF_BLOCKS"), SQLDataType.INTEGER, this, "");

    private ParkingLotTable(Name alias, Table<ParkingLotRecord> aliased) {
        this(alias, aliased, null);
    }

    private ParkingLotTable(Name alias, Table<ParkingLotRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>PARKING_LOT</code> table reference
     */
    public ParkingLotTable(String alias) {
        this(DSL.name(alias), PARKING_LOT);
    }

    /**
     * Create an aliased <code>PARKING_LOT</code> table reference
     */
    public ParkingLotTable(Name alias) {
        this(alias, PARKING_LOT);
    }

    /**
     * Create a <code>PARKING_LOT</code> table reference
     */
    public ParkingLotTable() {
        this(DSL.name("PARKING_LOT"), null);
    }

    public <O extends Record> ParkingLotTable(Table<O> child, ForeignKey<O, ParkingLotRecord> key) {
        super(child, key, PARKING_LOT);
    }

    @Override
    public Schema getSchema() {
        return DefaultSchema.DEFAULT_SCHEMA;
    }

    @Override
    public UniqueKey<ParkingLotRecord> getPrimaryKey() {
        return Keys.CONSTRAINT_1;
    }

    @Override
    public List<UniqueKey<ParkingLotRecord>> getKeys() {
        return Arrays.<UniqueKey<ParkingLotRecord>>asList(Keys.CONSTRAINT_1);
    }

    @Override
    public ParkingLotTable as(String alias) {
        return new ParkingLotTable(DSL.name(alias), this);
    }

    @Override
    public ParkingLotTable as(Name alias) {
        return new ParkingLotTable(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public ParkingLotTable rename(String name) {
        return new ParkingLotTable(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public ParkingLotTable rename(Name name) {
        return new ParkingLotTable(name, null);
    }

    // -------------------------------------------------------------------------
    // Row5 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row5<UUID, String, Double, Double, Integer> fieldsRow() {
        return (Row5) super.fieldsRow();
    }
}

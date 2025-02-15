/*
 * This file is generated by jOOQ.
 */
package com.netcracker.parkinglotmanagement.data.tables.records;


import com.netcracker.parkinglotmanagement.data.tables.CustomerTable;

import java.util.UUID;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record5;
import org.jooq.Row5;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class CustomerRecord extends UpdatableRecordImpl<CustomerRecord> implements Record5<UUID, Integer, String, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>CUSTOMER.ID</code>.
     */
    public CustomerRecord setId(UUID value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>CUSTOMER.ID</code>.
     */
    public UUID getId() {
        return (UUID) get(0);
    }

    /**
     * Setter for <code>CUSTOMER.CONTACT_NUMBER</code>.
     */
    public CustomerRecord setContactNumber(Integer value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>CUSTOMER.CONTACT_NUMBER</code>.
     */
    public Integer getContactNumber() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>CUSTOMER.EMAIL</code>.
     */
    public CustomerRecord setEmail(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>CUSTOMER.EMAIL</code>.
     */
    public String getEmail() {
        return (String) get(2);
    }

    /**
     * Setter for <code>CUSTOMER.NAME</code>.
     */
    public CustomerRecord setName(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>CUSTOMER.NAME</code>.
     */
    public String getName() {
        return (String) get(3);
    }

    /**
     * Setter for <code>CUSTOMER.VEHICLE_NUMBER</code>.
     */
    public CustomerRecord setVehicleNumber(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>CUSTOMER.VEHICLE_NUMBER</code>.
     */
    public String getVehicleNumber() {
        return (String) get(4);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<UUID> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record5 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row5<UUID, Integer, String, String, String> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    @Override
    public Row5<UUID, Integer, String, String, String> valuesRow() {
        return (Row5) super.valuesRow();
    }

    @Override
    public Field<UUID> field1() {
        return CustomerTable.CUSTOMER.ID;
    }

    @Override
    public Field<Integer> field2() {
        return CustomerTable.CUSTOMER.CONTACT_NUMBER;
    }

    @Override
    public Field<String> field3() {
        return CustomerTable.CUSTOMER.EMAIL;
    }

    @Override
    public Field<String> field4() {
        return CustomerTable.CUSTOMER.NAME;
    }

    @Override
    public Field<String> field5() {
        return CustomerTable.CUSTOMER.VEHICLE_NUMBER;
    }

    @Override
    public UUID component1() {
        return getId();
    }

    @Override
    public Integer component2() {
        return getContactNumber();
    }

    @Override
    public String component3() {
        return getEmail();
    }

    @Override
    public String component4() {
        return getName();
    }

    @Override
    public String component5() {
        return getVehicleNumber();
    }

    @Override
    public UUID value1() {
        return getId();
    }

    @Override
    public Integer value2() {
        return getContactNumber();
    }

    @Override
    public String value3() {
        return getEmail();
    }

    @Override
    public String value4() {
        return getName();
    }

    @Override
    public String value5() {
        return getVehicleNumber();
    }

    @Override
    public CustomerRecord value1(UUID value) {
        setId(value);
        return this;
    }

    @Override
    public CustomerRecord value2(Integer value) {
        setContactNumber(value);
        return this;
    }

    @Override
    public CustomerRecord value3(String value) {
        setEmail(value);
        return this;
    }

    @Override
    public CustomerRecord value4(String value) {
        setName(value);
        return this;
    }

    @Override
    public CustomerRecord value5(String value) {
        setVehicleNumber(value);
        return this;
    }

    @Override
    public CustomerRecord values(UUID value1, Integer value2, String value3, String value4, String value5) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached CustomerRecord
     */
    public CustomerRecord() {
        super(CustomerTable.CUSTOMER);
    }

    /**
     * Create a detached, initialised CustomerRecord
     */
    public CustomerRecord(UUID id, Integer contactNumber, String email, String name, String vehicleNumber) {
        super(CustomerTable.CUSTOMER);

        setId(id);
        setContactNumber(contactNumber);
        setEmail(email);
        setName(name);
        setVehicleNumber(vehicleNumber);
    }
}

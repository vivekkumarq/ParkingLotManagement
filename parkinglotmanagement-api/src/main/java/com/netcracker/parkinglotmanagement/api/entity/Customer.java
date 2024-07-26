package com.netcracker.parkinglotmanagement.api.entity;

import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = ParkingLotConstants.TableNames.CUSTOMER )
public class Customer {


    @Id
    @Column(name = ParkingLotConstants.TableColumnNames.ID, columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID id;

    @Column(name = ParkingLotConstants.TableColumnNames.VEHICLE_NUMBER, nullable = false, length = 20)
    private String vehicleNumber;

    @Column(name = ParkingLotConstants.TableColumnNames.CONTACT_NUMBER, nullable = false)
    private int contactNumber;

    @Column(name = ParkingLotConstants.TableColumnNames.NAME, nullable = false, length = 50)
    private String name;

    @Column(name = ParkingLotConstants.TableColumnNames.EMAIL, nullable = false, length = 50)
    private String email;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public int getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(int contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }




}

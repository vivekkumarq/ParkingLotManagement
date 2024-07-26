package com.netcracker.parkinglotmanagement.api.entity;

import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = ParkingLotConstants.TableNames.PARKING_LOT)
public class ParkingLotEntity {
    @Id
    @Column(name = ParkingLotConstants.TableColumnNames.ID, columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID id;

    @Column(name = ParkingLotConstants.TableColumnNames.NUMBER_OF_BLOCKS)
    private Integer numberOfBlocks;

    @Column(name = ParkingLotConstants.TableColumnNames.ADDRESS)
    private String address;

    @Column(name = ParkingLotConstants.TableColumnNames.LONGITUDE)
    private Double longitude;

    @Column(name = ParkingLotConstants.TableColumnNames.LATITUDE)
    private Double latitude;

    public ParkingLotEntity() {
    }

    public ParkingLotEntity(Integer numberOfBlocks, String address, Double longitude, Double latitude) {
        this.id = UUID.randomUUID();
        this.numberOfBlocks = numberOfBlocks;
        this.address = address;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public void setNumberOfBlocks(Integer numberOfBlocks) {
        this.numberOfBlocks = numberOfBlocks;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
}
package com.netcracker.parkinglotmanagement.api.entity;

import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = ParkingLotConstants.TableNames.BLOCK )
public class Block {

    @Id
    @Column(name = ParkingLotConstants.TableColumnNames.ID,columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID id;

    @Id
    @Column(name= ParkingLotConstants.TableColumnNames.PARKING_LOT_ID,columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID parkingLotId;

    @Column(name = ParkingLotConstants.TableColumnNames.BLOCK_CODE)
    private String blockCode;

    @Column(name = ParkingLotConstants.TableColumnNames.NUMBER_OF_FLOORS)
    private int numberOfFloors;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getParkingLotId() {
        return parkingLotId;
    }

    public void setParkingLotId(UUID parkingLotId) {
        this.parkingLotId = parkingLotId;
    }

    public String getBlockCode() {
        return blockCode;
    }

    public void setBlockCode(String blockCode) {
        this.blockCode = blockCode;
    }

    public int getNumberOfFloors() {
        return numberOfFloors;
    }

    public void setNumberOfFloors(int numberOfFloors) {
        this.numberOfFloors = numberOfFloors;
    }
}

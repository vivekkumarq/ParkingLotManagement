package com.netcracker.parkinglotmanagement.api.entity;

import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = ParkingLotConstants.TableNames.FLOOR )
public class Floor {
    @Id
    @Column(name = ParkingLotConstants.TableColumnNames.ID,columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID id;

    @Id
    @Column(name = ParkingLotConstants.TableColumnNames.ID,columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID blockId;

    @Column(name = ParkingLotConstants.TableColumnNames.FLOOR_NO)
    private int floorNo;

    @Column(name = ParkingLotConstants.TableColumnNames.NUMBER_OF_SLOTS)
    private int numberOfSlots;


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBlockId() {
        return blockId;
    }

    public void setBlockId(UUID blockId) {
        this.blockId = blockId;
    }

    public int getFloorNo() {
        return floorNo;
    }

    public void setFloorNo(int floorNo) {
        this.floorNo = floorNo;
    }

    public int getNumberOfSlots() {
        return numberOfSlots;
    }

    public void setNumberOfSlots(int numberOfSlots) {
        this.numberOfSlots = numberOfSlots;
    }
}

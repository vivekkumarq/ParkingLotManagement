package com.netcracker.parkinglotmanagement.api.entity;

import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = ParkingLotConstants.TableNames.PARKING_SLOT)
public class ParkingSlot {
    @Id
    @Column(name = ParkingLotConstants.TableColumnNames.ID, columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID id;

    @Column(name = ParkingLotConstants.TableColumnNames.FLOOR_ID, columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID floorId;

    @Column(name = ParkingLotConstants.TableColumnNames.SLOT_NUMBER, nullable = false)
    private int slotNumber;

    @Column(name = ParkingLotConstants.TableColumnNames.CHARGES, nullable = false)
    private int charges;

    @Column(name = ParkingLotConstants.TableColumnNames.VEHICLE_TYPE, nullable = false)
    private String vehicleType;
}

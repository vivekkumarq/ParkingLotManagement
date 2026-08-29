package com.netcracker.parkinglotmanagement.api.entity;

import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

/** A level within a block; owns the parking slots. */
@Entity
@Table(name = ParkingLotConstants.TableNames.FLOOR)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Floor {

    @Id
    @Column(name = ParkingLotConstants.TableColumnNames.ID, columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID id;

    @Column(name = ParkingLotConstants.TableColumnNames.BLOCK_ID,
            columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID blockId;

    @Column(name = ParkingLotConstants.TableColumnNames.FLOOR_NO, nullable = false)
    private Integer floorNo;

    @Column(name = ParkingLotConstants.TableColumnNames.NUMBER_OF_SLOTS, nullable = false)
    private Integer numberOfSlots;
}

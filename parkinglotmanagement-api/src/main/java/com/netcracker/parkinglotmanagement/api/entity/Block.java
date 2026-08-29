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

/** A wing of a parking lot; owns one or more floors. */
@Entity
@Table(name = ParkingLotConstants.TableNames.BLOCK)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Block {

    @Id
    @Column(name = ParkingLotConstants.TableColumnNames.ID, columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID id;

    @Column(name = ParkingLotConstants.TableColumnNames.PARKING_LOT_ID,
            columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID parkingLotId;

    @Column(name = ParkingLotConstants.TableColumnNames.BLOCK_CODE, nullable = false, length = 2)
    private String blockCode;

    @Column(name = ParkingLotConstants.TableColumnNames.NUMBER_OF_FLOORS, nullable = false)
    private Integer numberOfFloors;
}

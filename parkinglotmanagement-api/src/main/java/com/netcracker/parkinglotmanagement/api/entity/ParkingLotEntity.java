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

/** The car park itself: an address, a geo position and a number of blocks. */
@Entity
@Table(name = ParkingLotConstants.TableNames.PARKING_LOT)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLotEntity {

    @Id
    @Column(name = ParkingLotConstants.TableColumnNames.ID, columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID id;

    @Column(name = ParkingLotConstants.TableColumnNames.NUMBER_OF_BLOCKS, nullable = false)
    private Integer numberOfBlocks;

    @Column(name = ParkingLotConstants.TableColumnNames.ADDRESS, nullable = false, length = 500)
    private String address;

    @Column(name = ParkingLotConstants.TableColumnNames.LONGITUDE)
    private Double longitude;

    @Column(name = ParkingLotConstants.TableColumnNames.LATITUDE)
    private Double latitude;
}

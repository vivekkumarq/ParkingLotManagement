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

/** A registered customer and the vehicle they park. */
@Entity
@Table(name = ParkingLotConstants.TableNames.CUSTOMER)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @Column(name = ParkingLotConstants.TableColumnNames.ID, columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID id;

    @Column(name = ParkingLotConstants.TableColumnNames.VEHICLE_NUMBER, nullable = false, length = 20)
    private String vehicleNumber;

    /**
     * Text, not a number: a ten-digit phone number overflows a 32-bit integer and
     * leading zeros and country prefixes are meaningful.
     */
    @Column(name = ParkingLotConstants.TableColumnNames.CONTACT_NUMBER, nullable = false, length = 20)
    private String contactNumber;

    @Column(name = ParkingLotConstants.TableColumnNames.NAME, nullable = false, length = 50)
    private String name;

    @Column(name = ParkingLotConstants.TableColumnNames.EMAIL, nullable = false, length = 50)
    private String email;
}

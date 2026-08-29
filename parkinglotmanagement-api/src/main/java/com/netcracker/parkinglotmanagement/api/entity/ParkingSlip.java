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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The ticket issued when a vehicle enters and priced when it leaves.
 *
 * <p>A slip may be backed by a reservation ({@code parkingSlotReservationId}) or
 * be a plain walk-in, but it always names the slot it occupies.
 */
@Entity
@Table(name = ParkingLotConstants.TableNames.PARKING_SLIP)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSlip {

    @Id
    @Column(name = ParkingLotConstants.TableColumnNames.ID, columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID id;

    /** Null for a walk-in. */
    @Column(name = ParkingLotConstants.TableColumnNames.PARKING_SLOT_RESERVATION_ID,
            columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID parkingSlotReservationId;

    @Column(name = ParkingLotConstants.TableColumnNames.PARKING_SLOT_ID,
            columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID parkingSlotId;

    /** Null when the vehicle is not a registered customer. */
    @Column(name = ParkingLotConstants.TableColumnNames.CUSTOMER_ID,
            columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID customerId;

    @Column(name = ParkingLotConstants.TableColumnNames.VEHICLE_NUMBER, nullable = false, length = 20)
    private String vehicleNumber;

    @Column(name = ParkingLotConstants.TableColumnNames.VEHICLE_TYPE, nullable = false, length = 20)
    private String vehicleType;

    @Column(name = ParkingLotConstants.TableColumnNames.ACTUAL_ENTRY_TIME)
    private LocalDateTime actualEntryTime;

    /** Null while the slip is ACTIVE. */
    @Column(name = ParkingLotConstants.TableColumnNames.ACTUAL_EXIT_TIME)
    private LocalDateTime actualExitTime;

    @Column(name = ParkingLotConstants.TableColumnNames.BASIC_COST, nullable = false, precision = 10, scale = 2)
    private BigDecimal basicCost;

    @Column(name = ParkingLotConstants.TableColumnNames.PENALTY, precision = 10, scale = 2)
    private BigDecimal penalty;

    @Column(name = ParkingLotConstants.TableColumnNames.TOTAL_COST, nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCost;

    @Column(name = ParkingLotConstants.TableColumnNames.STATUS, nullable = false, length = 20)
    private String status;
}

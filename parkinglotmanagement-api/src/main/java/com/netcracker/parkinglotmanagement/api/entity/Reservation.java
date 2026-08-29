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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A slot held for a future window.
 *
 * <p>{@code endTimestamp} is derived from {@code startTimestamp + durationInHours}
 * and stored explicitly so that overlap detection is a plain range predicate the
 * database can answer with an index.
 */
@Entity
@Table(name = ParkingLotConstants.TableNames.RESERVATION)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @Column(name = ParkingLotConstants.TableColumnNames.ID, columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID id;

    @Column(name = ParkingLotConstants.TableColumnNames.CUSTOMER_ID,
            columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID customerId;

    @Column(name = ParkingLotConstants.TableColumnNames.PARKING_SLOT_ID,
            columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID parkingSlotId;

    @Column(name = ParkingLotConstants.TableColumnNames.VEHICLE_NUMBER, nullable = false, length = 20)
    private String vehicleNumber;

    @Column(name = ParkingLotConstants.TableColumnNames.START_TIMESTAMP, nullable = false)
    private LocalDateTime startTimestamp;

    @Column(name = ParkingLotConstants.TableColumnNames.END_TIMESTAMP, nullable = false)
    private LocalDateTime endTimestamp;

    @Column(name = ParkingLotConstants.TableColumnNames.DURATION_IN_HOURS, nullable = false)
    private Integer durationInHours;

    @Column(name = ParkingLotConstants.TableColumnNames.BOOKING_DATE, nullable = false)
    private LocalDate bookingDate;

    @Column(name = ParkingLotConstants.TableColumnNames.STATUS, nullable = false, length = 20)
    private String status;

    @Column(name = ParkingLotConstants.TableColumnNames.CREATED_AT, nullable = false)
    private LocalDateTime createdAt;
}

package com.netcracker.parkinglotmanagement.api.entity;

import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.security.Timestamp;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = ParkingLotConstants.TableNames.RESERVATION)
public class Reservation {


    @Id
    @Column(name = ParkingLotConstants.TableColumnNames.ID, columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID id;

    @Column(name = ParkingLotConstants.TableColumnNames.CUSTOMER_ID, columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID customerId;

    @Column(name = ParkingLotConstants.TableColumnNames.START_TIMESTAMP, nullable = false)
    private Timestamp startTimestamp;

    @Column(name = ParkingLotConstants.TableColumnNames.DURATION_IN_HOURS, nullable = false)
    private int durationInHours;

    @Column(name = ParkingLotConstants.TableColumnNames.BOOKING_DATE, nullable = false)
    private Date bookingDate;

    @Column(name = ParkingLotConstants.TableColumnNames.PARKING_SLOT_ID, columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID parkingSlotId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public Timestamp getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(Timestamp startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public int getDurationInHours() {
        return durationInHours;
    }

    public void setDurationInHours(int durationInHours) {
        this.durationInHours = durationInHours;
    }

    public Date getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(Date bookingDate) {
        this.bookingDate = bookingDate;
    }

    public UUID getParkingSlotId() {
        return parkingSlotId;
    }

    public void setParkingSlotId(UUID parkingSlotId) {
        this.parkingSlotId = parkingSlotId;
    }

}

package com.netcracker.parkinglotmanagement.api.entity;

import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.security.Timestamp;
import java.util.UUID;

@Entity
@Table(name = ParkingLotConstants.TableNames.PARKING_SLIP)
public class ParkingSlip {
    @Id
    @Column(name = ParkingLotConstants.TableColumnNames.ID, columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID id;

    @Column(name = ParkingLotConstants.TableColumnNames.PARKING_SLOT_RESERVATION_ID, columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID parkingSlotReservationId;

    @Column(name = ParkingLotConstants.TableColumnNames.ACTUAL_ENTRY_TIME)
    private Timestamp actualEntryTime;

    @Column(name = ParkingLotConstants.TableColumnNames.ACTUAL_EXIT_TIME)
    private Timestamp actualExitTime;

    @Column(name = ParkingLotConstants.TableColumnNames.BASIC_COST, nullable = false)
    private BigDecimal basicCost;

    @Column(name = ParkingLotConstants.TableColumnNames.PENALTY)
    private BigDecimal penalty;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getParkingSlotReservationId() {
        return parkingSlotReservationId;
    }

    public void setParkingSlotReservationId(UUID parkingSlotReservationId) {
        this.parkingSlotReservationId = parkingSlotReservationId;
    }

    public Timestamp getActualEntryTime() {
        return actualEntryTime;
    }

    public void setActualEntryTime(Timestamp actualEntryTime) {
        this.actualEntryTime = actualEntryTime;
    }

    public Timestamp getActualExitTime() {
        return actualExitTime;
    }

    public void setActualExitTime(Timestamp actualExitTime) {
        this.actualExitTime = actualExitTime;
    }

    public BigDecimal getBasicCost() {
        return basicCost;
    }

    public void setBasicCost(BigDecimal basicCost) {
        this.basicCost = basicCost;
    }

    public BigDecimal getPenalty() {
        return penalty;
    }

    public void setPenalty(BigDecimal penalty) {
        this.penalty = penalty;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    @Column(name = ParkingLotConstants.TableColumnNames.TOTAL_COST, nullable = false)
    private BigDecimal totalCost;
}

package com.netcracker.parkinglotmanagement.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ParkingSlipDTO {
    private UUID id;
    private UUID parkingSlotReservationId;
    private LocalDateTime actualEntryTime;
    private LocalDateTime actualExitTime;
    private Double basicCost;
    private Double penalty;
    private Double totalCost;

    @JsonProperty(ParkingLotConstants.TableColumnNames.TYPE)
    private final String type = "ParkingSlip";

    public ParkingSlipDTO(UUID id, UUID parkingSlotReservationId, LocalDateTime actualEntryTime, LocalDateTime actualExitTime, Double basicCost, Double penalty, Double totalCost) {
        this.id = id;
        this.parkingSlotReservationId = parkingSlotReservationId;
        this.actualEntryTime = actualEntryTime;
        this.actualExitTime = actualExitTime;
        this.basicCost = basicCost;
        this.penalty = penalty;
        this.totalCost = totalCost;
    }
}

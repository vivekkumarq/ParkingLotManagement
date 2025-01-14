package com.netcracker.parkinglotmanagement.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReservationDTO {
    private UUID id;
    private UUID customerId;
    private LocalDateTime startTimestamp;
    private Integer durationInHours;
    private LocalDate bookingDate;
    private UUID parkingSlotId;

    @JsonProperty(ParkingLotConstants.TableColumnNames.TYPE)
    private final String type = "Reservation";

    public ReservationDTO(UUID id, UUID customerId, LocalDateTime startTimestamp, Integer durationInHours, LocalDate bookingDate, UUID parkingSlotId) {
        this.id = id;
        this.customerId = customerId;
        this.startTimestamp = startTimestamp;
        this.durationInHours = durationInHours;
        this.bookingDate = bookingDate;
        this.parkingSlotId = parkingSlotId;
    }
}


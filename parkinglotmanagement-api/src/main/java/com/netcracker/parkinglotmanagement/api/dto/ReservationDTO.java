package com.netcracker.parkinglotmanagement.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;
import com.netcracker.parkinglotmanagement.api.domain.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Reservation", description = "A slot held for a future window")
public class ReservationDTO {

    private UUID id;

    private UUID customerId;

    private UUID parkingSlotId;

    @Schema(example = "KA01AB1234")
    private String vehicleNumber;

    private LocalDateTime startTimestamp;

    @Schema(description = "startTimestamp + durationInHours; the window is half-open [start, end)")
    private LocalDateTime endTimestamp;

    @Schema(example = "3")
    private Integer durationInHours;

    private LocalDate bookingDate;

    private ReservationStatus status;

    private LocalDateTime createdAt;

    @JsonProperty(ParkingLotConstants.TableColumnNames.TYPE)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private final String type = "Reservation";
}

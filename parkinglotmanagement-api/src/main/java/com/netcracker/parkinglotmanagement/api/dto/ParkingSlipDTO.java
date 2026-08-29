package com.netcracker.parkinglotmanagement.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;
import com.netcracker.parkinglotmanagement.api.domain.SlipStatus;
import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The ticket for one stay. Money is {@link BigDecimal} scaled to 2 decimals -
 * never {@code double}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ParkingSlip", description = "Ticket issued on entry and priced on exit")
public class ParkingSlipDTO {

    private UUID id;

    @Schema(description = "Set only when the stay came from a reservation")
    private UUID parkingSlotReservationId;

    private UUID parkingSlotId;

    private UUID customerId;

    @Schema(example = "KA01AB1234")
    private String vehicleNumber;

    @Schema(example = "CAR")
    private VehicleType vehicleType;

    private LocalDateTime actualEntryTime;

    @Schema(description = "Null while the slip is ACTIVE")
    private LocalDateTime actualExitTime;

    @Schema(description = "Duration charge from the rate card", example = "540.00")
    private BigDecimal basicCost;

    @Schema(description = "Overstay charge; 0.00 unless a reserved window was exceeded", example = "0.00")
    private BigDecimal penalty;

    @Schema(description = "basicCost + penalty", example = "540.00")
    private BigDecimal totalCost;

    private SlipStatus status;

    @JsonProperty(ParkingLotConstants.TableColumnNames.TYPE)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private final String type = "ParkingSlip";
}

package com.netcracker.parkinglotmanagement.api.dto;

import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Free / reserved / occupied counts for one bucket of slots. Which of the
 * location fields are populated depends on the grouping the caller asked for.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SlotAvailability")
public class SlotAvailabilityDTO {

    private UUID parkingLotId;

    private UUID blockId;

    @Schema(example = "A1")
    private String blockCode;

    private UUID floorId;

    @Schema(example = "1")
    private Integer floorNo;

    private VehicleType vehicleType;

    @Schema(example = "12")
    private Integer freeSlots;

    @Schema(example = "3")
    private Integer reservedSlots;

    @Schema(example = "35")
    private Integer occupiedSlots;

    @Schema(example = "0")
    private Integer outOfServiceSlots;

    @Schema(description = "free + reserved + occupied + outOfService", example = "50")
    private Integer totalSlots;
}

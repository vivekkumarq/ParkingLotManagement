package com.netcracker.parkinglotmanagement.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Live occupancy for one lot, with the per-vehicle-type breakdown. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "OccupancySummary")
public class OccupancySummaryDTO {

    private UUID parkingLotId;

    private LocalDateTime asOf;

    @Schema(example = "150")
    private Integer totalSlots;

    @Schema(example = "98")
    private Integer occupiedSlots;

    @Schema(example = "9")
    private Integer reservedSlots;

    @Schema(example = "43")
    private Integer freeSlots;

    @Schema(description = "occupied / total, as a percentage rounded to 2 decimals", example = "65.33")
    private BigDecimal occupancyRate;

    @Schema(description = "Number of slips currently open in this lot", example = "98")
    private Integer activeSlips;

    private List<SlotAvailabilityDTO> byVehicleType;
}

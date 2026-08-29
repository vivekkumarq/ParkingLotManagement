package com.netcracker.parkinglotmanagement.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One sample in an occupancy-over-time series. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "OccupancyPoint")
public class OccupancyPointDTO {

    @Schema(description = "Start of the bucket", example = "2026-03-01T09:00:00")
    private LocalDateTime bucketStart;

    @Schema(description = "Slips open at any point during the bucket", example = "84")
    private Long occupiedSlots;

    @Schema(example = "150")
    private Integer totalSlots;

    @Schema(description = "occupiedSlots / totalSlots as a percentage", example = "56.00")
    private BigDecimal occupancyRate;
}

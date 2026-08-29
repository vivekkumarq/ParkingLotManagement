package com.netcracker.parkinglotmanagement.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Arrivals per hour-of-day, used to rank the busiest hours. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PeakHour")
public class PeakHourDTO {

    @Schema(description = "Hour of day, 0-23", example = "18")
    private Integer hourOfDay;

    @Schema(description = "Vehicles that entered during this hour across the range", example = "97")
    private Long entries;
}

package com.netcracker.parkinglotmanagement.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** How long vehicles stayed, over a date range. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "DurationReport")
public class DurationReportDTO {

    private LocalDateTime from;

    private LocalDateTime to;

    @Schema(example = "412")
    private Long closedSlips;

    @Schema(example = "168.45")
    private BigDecimal averageMinutes;

    @Schema(example = "2.81")
    private BigDecimal averageHours;

    @Schema(example = "12")
    private Long minMinutes;

    @Schema(example = "2874")
    private Long maxMinutes;
}

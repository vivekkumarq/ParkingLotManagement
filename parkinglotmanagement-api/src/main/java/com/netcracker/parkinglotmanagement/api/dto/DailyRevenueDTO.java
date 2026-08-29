package com.netcracker.parkinglotmanagement.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One day's takings inside a {@link RevenueReportDTO}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "DailyRevenue")
public class DailyRevenueDTO {

    private LocalDate date;

    @Schema(example = "37")
    private Long closedSlips;

    @Schema(example = "16780.00")
    private BigDecimal totalRevenue;
}

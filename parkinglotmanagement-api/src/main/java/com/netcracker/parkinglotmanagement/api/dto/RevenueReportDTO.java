package com.netcracker.parkinglotmanagement.api.dto;

import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Revenue taken from slips closed inside a date range. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RevenueReport")
public class RevenueReportDTO {

    private LocalDateTime from;

    private LocalDateTime to;

    @Schema(example = "412")
    private Long closedSlips;

    @Schema(example = "184320.00")
    private BigDecimal basicRevenue;

    @Schema(example = "2400.00")
    private BigDecimal penaltyRevenue;

    @Schema(description = "basicRevenue + penaltyRevenue", example = "186720.00")
    private BigDecimal totalRevenue;

    @Schema(description = "totalRevenue / closedSlips", example = "453.20")
    private BigDecimal averageTicket;

    private Map<VehicleType, BigDecimal> revenueByVehicleType;

    private List<DailyRevenueDTO> daily;

    @Schema(example = "INR")
    private String currency;
}

package com.netcracker.parkinglotmanagement.api.dto;

import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * How a stay was priced. Returned alongside the closed slip so a customer can
 * see why they were charged what they were charged.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "FeeBreakdown")
public class FeeBreakdownDTO {

    private VehicleType vehicleType;

    @Schema(description = "Whole minutes between entry and exit", example = "1650")
    private Long durationMinutes;

    @Schema(description = "True when the stay fell inside the free grace period", example = "false")
    private Boolean withinGracePeriod;

    @Schema(description = "Complete 24-hour periods, each billed at the daily cap", example = "1")
    private Long chargedDays;

    @Schema(description = "Hours billed for the part-day remainder, rounded up", example = "4")
    private Long chargedHours;

    @Schema(example = "400.00")
    private BigDecimal dayCharges;

    @Schema(example = "140.00")
    private BigDecimal hourCharges;

    @Schema(description = "dayCharges + hourCharges", example = "540.00")
    private BigDecimal basicCost;

    @Schema(description = "Overstay beyond a reserved window", example = "0.00")
    private BigDecimal penalty;

    @Schema(description = "basicCost + penalty", example = "540.00")
    private BigDecimal totalCost;

    @Schema(example = "INR")
    private String currency;
}

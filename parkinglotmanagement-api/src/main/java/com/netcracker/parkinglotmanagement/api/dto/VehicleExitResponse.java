package com.netcracker.parkinglotmanagement.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** What the exit gate gets back: the closed slip and how its total was reached. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "VehicleExitResponse")
public class VehicleExitResponse {

    private ParkingSlipDTO slip;

    private FeeBreakdownDTO fee;
}

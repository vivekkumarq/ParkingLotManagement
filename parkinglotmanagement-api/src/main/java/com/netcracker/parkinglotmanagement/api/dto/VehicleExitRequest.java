package com.netcracker.parkinglotmanagement.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Check a vehicle out: close the slip, price the stay and free the slot. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "VehicleExitRequest")
public class VehicleExitRequest {

    @Schema(description = "Defaults to the server clock.")
    private LocalDateTime exitTime;
}

package com.netcracker.parkinglotmanagement.api.dto;

import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

/** Check a vehicle in: allocate a slot and issue a slip. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "VehicleEntryRequest")
public class VehicleEntryRequest {

    @NotBlank
    @Size(max = 20)
    @Schema(example = "KA01AB1234")
    private String vehicleNumber;

    @NotNull
    @Schema(example = "CAR")
    private VehicleType vehicleType;

    @NotNull
    @Schema(description = "Lot to park in")
    private UUID parkingLotId;

    @Schema(description = "Links the slip to a registered customer; optional for a walk-in")
    private UUID customerId;

    @Schema(description = "Defaults to the server clock. Accepted so that gate hardware can " +
            "replay a reading taken while it was offline.")
    private LocalDateTime entryTime;
}

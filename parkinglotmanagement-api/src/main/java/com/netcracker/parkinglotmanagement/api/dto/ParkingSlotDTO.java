package com.netcracker.parkinglotmanagement.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;
import com.netcracker.parkinglotmanagement.api.domain.SlotStatus;
import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ParkingSlot", description = "A single bay on a floor")
public class ParkingSlotDTO {

    @Schema(description = "Server-assigned identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @NotNull
    private UUID floorId;

    @NotNull
    @Min(value = 1, message = "slotNumber starts at 1")
    @Schema(example = "17")
    private Integer slotNumber;

    /** Optional per-slot surcharge carried over from V1; billing uses the rate card. */
    @Min(value = 0)
    @Schema(example = "0")
    private Integer charges;

    @NotNull
    @Schema(example = "CAR")
    private VehicleType vehicleType;

    @Schema(description = "Managed by the allocation engine", accessMode = Schema.AccessMode.READ_ONLY)
    private SlotStatus status;

    @Schema(description = "Optimistic-lock counter", accessMode = Schema.AccessMode.READ_ONLY)
    private Long version;

    @JsonProperty(ParkingLotConstants.TableColumnNames.TYPE)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private final String type = "ParkingSlot";
}

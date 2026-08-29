package com.netcracker.parkinglotmanagement.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Floor", description = "A level within a block")
public class FloorDTO {

    @Schema(description = "Server-assigned identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @NotNull
    private UUID blockId;

    @NotNull
    @Min(value = 0, message = "floorNo cannot be negative")
    @Schema(example = "1")
    private Integer floorNo;

    @NotNull
    @Min(value = 1, message = "a floor must have at least one slot")
    @Schema(example = "50")
    private Integer numberOfSlots;

    @JsonProperty(ParkingLotConstants.TableColumnNames.TYPE)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private final String type = "Floor";
}

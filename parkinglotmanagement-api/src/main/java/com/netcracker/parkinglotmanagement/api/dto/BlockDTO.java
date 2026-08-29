package com.netcracker.parkinglotmanagement.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Block", description = "A wing of a parking lot")
public class BlockDTO {

    @Schema(description = "Server-assigned identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @NotNull
    private UUID parkingLotId;

    @NotBlank
    @Size(max = 2, message = "blockCode is at most 2 characters")
    @Schema(example = "A1")
    private String blockCode;

    @NotNull
    @Min(value = 1, message = "a block must have at least one floor")
    @Schema(example = "4")
    private Integer numberOfFloors;

    @JsonProperty(ParkingLotConstants.TableColumnNames.TYPE)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private final String type = "Block";
}

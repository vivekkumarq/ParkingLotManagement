package com.netcracker.parkinglotmanagement.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ParkingLot", description = "A car park")
public class ParkingLotDTO {

    @Schema(description = "Server-assigned identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @NotNull
    @Min(value = 1, message = "a parking lot must have at least one block")
    @Schema(example = "3")
    private Integer numberOfBlocks;

    @NotBlank
    @Size(max = 500)
    @Schema(example = "12 MG Road, Bengaluru 560001")
    private String address;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    @Schema(example = "77.5946")
    private Double longitude;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    @Schema(example = "12.9716")
    private Double latitude;

    @JsonProperty(ParkingLotConstants.TableColumnNames.TYPE)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private final String type = "ParkingLot";
}

package com.netcracker.parkinglotmanagement.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Customer", description = "A registered customer and their vehicle")
public class CustomerDTO {

    @Schema(description = "Server-assigned identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @NotBlank
    @Size(max = 20)
    @Schema(example = "KA01AB1234")
    private String vehicleNumber;

    /** Text, not a number: ten digits overflow an int and leading zeros matter. */
    @NotBlank
    @Pattern(regexp = "^[+]?[0-9 ()-]{6,20}$", message = "contactNumber is not a valid phone number")
    @Schema(example = "9876543210")
    private String contactNumber;

    @NotBlank
    @Size(max = 50)
    @Schema(example = "Asha Rao")
    private String name;

    @NotBlank
    @Email
    @Size(max = 50)
    @Schema(example = "asha.rao@example.com")
    private String email;

    @JsonProperty(ParkingLotConstants.TableColumnNames.TYPE)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private final String type = "Customer";
}

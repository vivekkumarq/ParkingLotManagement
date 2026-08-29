package com.netcracker.parkinglotmanagement.api.dto;

import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

/** Book a slot for a future window. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ReservationRequest")
public class ReservationRequest {

    @NotBlank
    @Size(max = 20)
    @Schema(example = "KA01AB1234")
    private String vehicleNumber;

    @NotNull
    @Schema(example = "CAR")
    private VehicleType vehicleType;

    @NotNull
    private UUID parkingLotId;

    @Schema(description = "Optional: book one specific slot instead of letting the " +
            "allocator choose the lowest free one")
    private UUID parkingSlotId;

    private UUID customerId;

    @NotNull
    @Schema(example = "2026-09-01T09:00:00")
    private LocalDateTime startTimestamp;

    @NotNull
    @Min(value = 1, message = "a reservation is at least one hour")
    @Max(value = 720, message = "a reservation is at most 30 days")
    @Schema(example = "3")
    private Integer durationInHours;
}

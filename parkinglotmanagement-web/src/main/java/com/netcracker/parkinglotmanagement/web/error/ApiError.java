package com.netcracker.parkinglotmanagement.web.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The single error shape every failing endpoint returns.
 *
 * <pre>
 * {
 *   "timestamp": "2026-08-29T13:45:12.318",
 *   "status": 409,
 *   "error": "Conflict",
 *   "code": "NO_SLOT_AVAILABLE",
 *   "message": "No free slot is available for vehicle type TRUCK",
 *   "path": "/parking-lot-management/parking/entry"
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ApiError", description = "Structured error payload")
public class ApiError {

    private LocalDateTime timestamp;

    @Schema(example = "409")
    private Integer status;

    @Schema(description = "HTTP reason phrase", example = "Conflict")
    private String error;

    @Schema(description = "Stable machine-readable code", example = "NO_SLOT_AVAILABLE")
    private String code;

    @Schema(example = "No free slot is available for vehicle type TRUCK")
    private String message;

    @Schema(example = "/parking-lot-management/parking/entry")
    private String path;

    /** Present only for validation failures: one entry per rejected field. */
    private List<FieldViolation> violations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "FieldViolation")
    public static class FieldViolation {

        @Schema(example = "vehicleNumber")
        private String field;

        @Schema(example = "must not be blank")
        private String message;
    }
}

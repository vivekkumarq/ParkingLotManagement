package com.netcracker.parkinglotmanagement.web.error;

import com.netcracker.parkinglotmanagement.api.exception.DuplicateEntryException;
import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import com.netcracker.parkinglotmanagement.api.exception.NoSlotAvailableException;
import com.netcracker.parkinglotmanagement.api.exception.ParkingLotException;
import com.netcracker.parkinglotmanagement.api.exception.ReservationConflictException;
import com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException;
import com.netcracker.parkinglotmanagement.api.exception.SlotUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Turns every failure into the same {@link ApiError} document.
 *
 * <p>The project previously had no advice at all, so a missing record surfaced as
 * an empty 200, and anything unexpected produced Spring's default HTML error page
 * with the exception class name in it. Both are fixed here: domain exceptions map
 * to a deliberate status and a stable {@code code}, and anything genuinely
 * unforeseen is logged in full but answered with a bare 500 that leaks nothing.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, e, e.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(InvalidRequestException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, e, e.getMessage(), request, null);
    }

    /**
     * 409 rather than 400: the request was well-formed and may succeed later, once a
     * vehicle leaves or a booking is cancelled.
     */
    @ExceptionHandler({NoSlotAvailableException.class, SlotUnavailableException.class,
            ReservationConflictException.class, DuplicateEntryException.class})
    public ResponseEntity<ApiError> handleConflict(ParkingLotException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, e, e.getMessage(), request, null);
    }

    /** Bean Validation on a {@code @Valid @RequestBody}. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException e,
                                                         HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = new ArrayList<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            violations.add(ApiError.FieldViolation.builder()
                    .field(fieldError.getField())
                    .message(fieldError.getDefaultMessage())
                    .build());
        }
        return build(HttpStatus.BAD_REQUEST, null, "Request validation failed", request, violations);
    }

    /** Bean Validation on path variables and request parameters. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleParameterValidation(ConstraintViolationException e,
                                                              HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = e.getConstraintViolations().stream()
                .map(GlobalExceptionHandler::toViolation)
                .collect(Collectors.toList());
        return build(HttpStatus.BAD_REQUEST, null, "Request validation failed", request, violations);
    }

    /** Unparseable JSON, or an enum value the domain does not know. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException e,
                                                         HttpServletRequest request) {
        LOG.debug("Rejected an unreadable request body", e);
        return build(HttpStatus.BAD_REQUEST, null,
                "Request body could not be read; check the field types and enum values", request, null);
    }

    /** A path variable or query parameter of the wrong type, e.g. a malformed UUID. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                       HttpServletRequest request) {
        String message = "Parameter '" + e.getName() + "' has an invalid value: " + e.getValue();
        return build(HttpStatus.BAD_REQUEST, null, message, request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException e,
                                                           HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, null,
                "Required parameter '" + e.getParameterName() + "' is missing", request, null);
    }

    /** Values that failed a domain-level {@code valueOf}, e.g. an unknown vehicle type. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException e,
                                                          HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, null, e.getMessage(), request, null);
    }

    /**
     * Last resort. The stack trace goes to the log, never to the client - an error
     * response should not tell a caller which classes and libraries are in use.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception e, HttpServletRequest request) {
        LOG.error("Unhandled exception while serving {} {}", request.getMethod(), request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, null,
                "An unexpected error occurred. Please contact support with the timestamp above.",
                request, null);
    }

    private static ApiError.FieldViolation toViolation(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : null;
        return ApiError.FieldViolation.builder()
                .field(path)
                .message(violation.getMessage())
                .build();
    }

    private static ResponseEntity<ApiError> build(HttpStatus status,
                                                  ParkingLotException domainException,
                                                  String message,
                                                  HttpServletRequest request,
                                                  List<ApiError.FieldViolation> violations) {
        ApiError body = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(domainException != null ? domainException.getErrorCode() : defaultCode(status))
                .message(message)
                .path(request != null ? request.getRequestURI() : null)
                .violations(violations)
                .build();
        return ResponseEntity.status(status).body(body);
    }

    private static String defaultCode(HttpStatus status) {
        switch (status) {
            case BAD_REQUEST:
                return "VALIDATION_FAILED";
            case NOT_FOUND:
                return "RESOURCE_NOT_FOUND";
            case CONFLICT:
                return "CONFLICT";
            default:
                return "INTERNAL_ERROR";
        }
    }
}

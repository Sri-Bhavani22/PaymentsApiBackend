package com.payment.orchestration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response DTO.
 * Used for consistent error formatting across all API endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Error code for programmatic handling
     */
    private String errorCode;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * Detailed description of the error
     */
    private String detail;

    /**
     * HTTP status code
     */
    private Integer status;

    /**
     * Request path that caused the error
     */
    private String path;

    /**
     * Timestamp of the error
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Field-level validation errors
     */
    private List<FieldError> fieldErrors;

    /**
     * Payment ID if error is related to a specific payment
     */
    private String paymentId;

    /**
     * Inner class for field validation errors
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }

    /**
     * Create a validation error response
     */
    public static ErrorResponse validationError(String message, List<FieldError> fieldErrors, String path) {
        return ErrorResponse.builder()
                .errorCode("PAY_001")
                .message("Validation failed")
                .detail(message)
                .status(400)
                .path(path)
                .fieldErrors(fieldErrors)
                .build();
    }

    /**
     * Create a not found error response
     */
    public static ErrorResponse notFound(String resource, String identifier, String path) {
        return ErrorResponse.builder()
                .errorCode("PAY_002")
                .message(resource + " not found")
                .detail(resource + " with identifier '" + identifier + "' was not found")
                .status(404)
                .path(path)
                .build();
    }

    /**
     * Create a conflict error response (duplicate idempotency key)
     */
    public static ErrorResponse conflict(String message, String path) {
        return ErrorResponse.builder()
                .errorCode("PAY_003")
                .message("Conflict")
                .detail(message)
                .status(409)
                .path(path)
                .build();
    }

    /**
     * Create a service unavailable error response
     */
    public static ErrorResponse serviceUnavailable(String message, String path) {
        return ErrorResponse.builder()
                .errorCode("PAY_004")
                .message("Service unavailable")
                .detail(message)
                .status(503)
                .path(path)
                .build();
    }

    /**
     * Create an internal error response
     */
    public static ErrorResponse internalError(String message, String path) {
        return ErrorResponse.builder()
                .errorCode("PAY_005")
                .message("Internal server error")
                .detail(message)
                .status(500)
                .path(path)
                .build();
    }

    /**
     * Create a bad request error response
     */
    public static ErrorResponse badRequest(String errorCode, String message, String detail, String path) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .detail(detail)
                .status(400)
                .path(path)
                .build();
    }
}

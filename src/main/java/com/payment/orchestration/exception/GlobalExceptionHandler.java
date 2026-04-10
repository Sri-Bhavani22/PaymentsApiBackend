package com.payment.orchestration.exception;

import com.payment.orchestration.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for the payment orchestration API.
 * Converts exceptions to standardized error responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle PaymentException
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(
            PaymentException ex, HttpServletRequest request) {
        
        log.error("Payment exception: {} - {}", ex.getErrorCode(), ex.getMessage());

        HttpStatus status = mapErrorCodeToStatus(ex.getErrorCode());

        ErrorResponse error = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .detail(ex.getDetails())
                .status(status.value())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(error);
    }

    /**
     * Handle IdempotencyException
     */
    @ExceptionHandler(IdempotencyException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyException(
            IdempotencyException ex, HttpServletRequest request) {
        
        log.warn("Idempotency exception: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message("Idempotency conflict")
                .detail(ex.getMessage())
                .status(HttpStatus.CONFLICT.value())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        log.warn("Validation exception: {}", ex.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .collect(Collectors.toList());

        ErrorResponse error = ErrorResponse.validationError(
                "Request validation failed",
                fieldErrors,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle missing request header (idempotency key)
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
            MissingRequestHeaderException ex, HttpServletRequest request) {
        
        log.warn("Missing required header: {}", ex.getHeaderName());

        ErrorResponse error = ErrorResponse.builder()
                .errorCode("PAY_001")
                .message("Missing required header")
                .detail("Required header '" + ex.getHeaderName() + "' is missing")
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        log.warn("Illegal argument: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .errorCode("PAY_001")
                .message("Invalid request")
                .detail(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected exception", ex);

        ErrorResponse error = ErrorResponse.builder()
                .errorCode("PAY_005")
                .message("Internal server error")
                .detail("An unexpected error occurred. Please try again later.")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Map error code to HTTP status
     */
    private HttpStatus mapErrorCodeToStatus(String errorCode) {
        return switch (errorCode) {
            case "PAY_001", "PAY_006" -> HttpStatus.BAD_REQUEST;
            case "PAY_002" -> HttpStatus.NOT_FOUND;
            case "PAY_003" -> HttpStatus.CONFLICT;
            case "PAY_004" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * Map Spring FieldError to our FieldError DTO
     */
    private ErrorResponse.FieldError mapFieldError(FieldError fieldError) {
        return ErrorResponse.FieldError.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .rejectedValue(fieldError.getRejectedValue())
                .build();
    }
}

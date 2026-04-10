package com.payment.orchestration.controller;

import com.payment.orchestration.dto.PaymentRequest;
import com.payment.orchestration.dto.PaymentResponse;
import com.payment.orchestration.dto.PaymentStatusResponse;
import com.payment.orchestration.exception.PaymentException;
import com.payment.orchestration.service.PaymentOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for payment operations.
 * Provides endpoints for creating and fetching payments.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment orchestration API endpoints")
public class PaymentController {

    private final PaymentOrchestrationService paymentOrchestrationService;

    /**
     * Create a new payment
     * 
     * @param idempotencyKey Unique key to ensure idempotent processing
     * @param request Payment request details
     * @return Created payment response
     */
    @PostMapping
    @Operation(
            summary = "Create a new payment",
            description = "Creates a new payment request and processes it through the appropriate provider. " +
                    "Requires an Idempotency-Key header to prevent duplicate processing."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Payment created successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Idempotency conflict"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PaymentResponse> createPayment(
            @Parameter(description = "Unique idempotency key for this request", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {

        log.info("Received payment request with idempotency key: {}", idempotencyKey);

        // Validate idempotency key
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        // Validate idempotency key format (should be UUID-like)
        if (idempotencyKey.length() < 8 || idempotencyKey.length() > 100) {
            throw new IllegalArgumentException("Idempotency-Key must be between 8 and 100 characters");
        }

        // Validate payment method specific fields
        if (!request.isValid()) {
            throw PaymentException.validation(request.getValidationError());
        }

        PaymentResponse response = paymentOrchestrationService.createPayment(request, idempotencyKey);

        HttpStatus status = switch (response.getStatus()) {
            case SUCCESS -> HttpStatus.CREATED;
            case PROCESSING, PENDING, RETRY -> HttpStatus.ACCEPTED;
            default -> HttpStatus.OK;
        };

        log.info("Payment {} created with status: {}", response.getPaymentId(), response.getStatus());

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Get payment by ID
     * 
     * @param paymentId The unique payment identifier
     * @return Payment details
     */
    @GetMapping("/{paymentId}")
    @Operation(
            summary = "Get payment by ID",
            description = "Retrieves payment details by the payment ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment found",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<PaymentResponse> getPayment(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable String paymentId) {

        log.debug("Fetching payment: {}", paymentId);

        PaymentResponse response = paymentOrchestrationService.getPayment(paymentId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment status with history
     * 
     * @param paymentId The unique payment identifier
     * @return Payment status with full history
     */
    @GetMapping("/{paymentId}/status")
    @Operation(
            summary = "Get payment status",
            description = "Retrieves the current status and complete status history of a payment"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Status retrieved",
                    content = @Content(schema = @Schema(implementation = PaymentStatusResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable String paymentId) {

        log.debug("Fetching payment status: {}", paymentId);

        PaymentStatusResponse response = paymentOrchestrationService.getPaymentStatus(paymentId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the payment service is healthy")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}

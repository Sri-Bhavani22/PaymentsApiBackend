package com.payment.orchestration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.payment.orchestration.entity.Payment;
import com.payment.orchestration.enums.PaymentMethod;
import com.payment.orchestration.enums.PaymentStatus;
import com.payment.orchestration.enums.ProviderType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for payment operations.
 * Contains payment details to be returned to the client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {

    /**
     * Unique payment identifier
     */
    private String paymentId;

    /**
     * Current payment status
     */
    private PaymentStatus status;

    /**
     * Status description
     */
    private String statusDescription;

    /**
     * Payment amount
     */
    private BigDecimal amount;

    /**
     * Currency code
     */
    private String currency;

    /**
     * Payment method used
     */
    private PaymentMethod paymentMethod;

    /**
     * Provider handling the payment
     */
    private ProviderType provider;

    /**
     * Reference from the payment provider
     */
    private String providerReference;

    /**
     * Masked card number (for card payments)
     */
    private String maskedCardNumber;

    /**
     * UPI ID (for UPI payments)
     */
    private String upiId;

    /**
     * Number of retry attempts
     */
    private Integer retryCount;

    /**
     * Error code if payment failed
     */
    private String errorCode;

    /**
     * Error message if payment failed
     */
    private String errorMessage;

    /**
     * When payment was created
     */
    private LocalDateTime createdAt;

    /**
     * When payment was last updated
     */
    private LocalDateTime updatedAt;

    /**
     * When payment was completed
     */
    private LocalDateTime completedAt;

    /**
     * Message for the client
     */
    private String message;

    /**
     * Create response from Payment entity
     */
    public static PaymentResponse fromEntity(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .status(payment.getStatus())
                .statusDescription(payment.getStatus().getDescription())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .provider(payment.getProvider())
                .providerReference(payment.getProviderReference())
                .maskedCardNumber(payment.getMaskedCardNumber())
                .upiId(payment.getUpiId())
                .retryCount(payment.getRetryCount())
                .errorCode(payment.getErrorCode())
                .errorMessage(payment.getErrorMessage())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .completedAt(payment.getCompletedAt())
                .build();
    }

    /**
     * Create a simple success response
     */
    public static PaymentResponse success(Payment payment, String message) {
        PaymentResponse response = fromEntity(payment);
        response.setMessage(message);
        return response;
    }

    /**
     * Create a processing response
     */
    public static PaymentResponse processing(Payment payment) {
        PaymentResponse response = fromEntity(payment);
        response.setMessage("Payment is being processed");
        return response;
    }

    /**
     * Create a failed response
     */
    public static PaymentResponse failed(Payment payment) {
        PaymentResponse response = fromEntity(payment);
        response.setMessage("Payment failed: " + payment.getErrorMessage());
        return response;
    }
}

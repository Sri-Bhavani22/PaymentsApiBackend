package com.payment.orchestration.enums;

/**
 * Represents the status of a payment throughout its lifecycle.
 */
public enum PaymentStatus {
    
    /**
     * Payment has been created but not yet processed
     */
    PENDING("Payment is pending"),
    
    /**
     * Payment is currently being processed by a provider
     */
    PROCESSING("Payment is being processed"),
    
    /**
     * Payment was successfully completed
     */
    SUCCESS("Payment completed successfully"),
    
    /**
     * Payment failed after all retry attempts
     */
    FAILED("Payment failed"),
    
    /**
     * Payment is being retried due to a transient failure
     */
    RETRY("Payment is being retried"),
    
    /**
     * Payment was refunded
     */
    REFUNDED("Payment was refunded"),
    
    /**
     * Payment was cancelled
     */
    CANCELLED("Payment was cancelled");
    
    private final String description;
    
    PaymentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if the payment is in a terminal state (cannot be modified)
     */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == REFUNDED || this == CANCELLED;
    }
    
    /**
     * Check if the payment can be retried
     */
    public boolean isRetryable() {
        return this == PENDING || this == RETRY || this == PROCESSING;
    }
}

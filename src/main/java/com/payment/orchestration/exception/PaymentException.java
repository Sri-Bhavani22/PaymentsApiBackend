package com.payment.orchestration.exception;

import lombok.Getter;

/**
 * Exception thrown when payment processing fails.
 */
@Getter
public class PaymentException extends RuntimeException {

    private final String errorCode;
    private final String details;

    public PaymentException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public PaymentException(String errorCode, String message, String details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public PaymentException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = cause.getMessage();
    }

    /**
     * Create a validation exception
     */
    public static PaymentException validation(String message) {
        return new PaymentException("PAY_001", message);
    }

    /**
     * Create a not found exception
     */
    public static PaymentException notFound(String resource, String identifier) {
        return new PaymentException("PAY_002", 
                resource + " not found: " + identifier);
    }

    /**
     * Create a provider unavailable exception
     */
    public static PaymentException providerUnavailable(String provider) {
        return new PaymentException("PAY_004", 
                "Provider unavailable: " + provider);
    }

    /**
     * Create a processing error exception
     */
    public static PaymentException processingError(String message) {
        return new PaymentException("PAY_005", message);
    }

    /**
     * Create an invalid payment method exception
     */
    public static PaymentException invalidPaymentMethod(String method) {
        return new PaymentException("PAY_006", 
                "Invalid payment method: " + method);
    }
}

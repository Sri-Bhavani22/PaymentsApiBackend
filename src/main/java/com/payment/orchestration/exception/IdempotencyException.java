package com.payment.orchestration.exception;

import lombok.Getter;

/**
 * Exception thrown when there's an idempotency conflict.
 * This occurs when a different request is made with the same idempotency key.
 */
@Getter
public class IdempotencyException extends RuntimeException {

    private static final String ERROR_CODE = "PAY_003";
    private final String idempotencyKey;

    public IdempotencyException(String message) {
        super(message);
        this.idempotencyKey = null;
    }

    public IdempotencyException(String message, String idempotencyKey) {
        super(message);
        this.idempotencyKey = idempotencyKey;
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }

    /**
     * Create a conflict exception for mismatched requests
     */
    public static IdempotencyException conflict(String idempotencyKey) {
        return new IdempotencyException(
                "A different request was already processed with idempotency key: " + idempotencyKey,
                idempotencyKey
        );
    }

    /**
     * Create an exception for concurrent processing
     */
    public static IdempotencyException inProgress(String idempotencyKey) {
        return new IdempotencyException(
                "Request is currently being processed with idempotency key: " + idempotencyKey,
                idempotencyKey
        );
    }
}

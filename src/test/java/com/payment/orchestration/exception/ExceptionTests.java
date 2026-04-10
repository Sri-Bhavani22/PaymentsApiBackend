package com.payment.orchestration.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for exception classes.
 */
class ExceptionTests {

    @Nested
    @DisplayName("PaymentException Tests")
    class PaymentExceptionTests {

        @Test
        @DisplayName("Should create exception with error code and message")
        void shouldCreateExceptionWithErrorCodeAndMessage() {
            PaymentException exception = new PaymentException("PAY_001", "Insufficient funds");

            assertThat(exception.getErrorCode()).isEqualTo("PAY_001");
            assertThat(exception.getMessage()).isEqualTo("Insufficient funds");
        }

        @Test
        @DisplayName("Should create exception with error code, message, and details")
        void shouldCreateExceptionWithDetails() {
            PaymentException exception = new PaymentException("PAY_002", "Payment failed", "Card declined");

            assertThat(exception.getErrorCode()).isEqualTo("PAY_002");
            assertThat(exception.getMessage()).isEqualTo("Payment failed");
            assertThat(exception.getDetails()).isEqualTo("Card declined");
        }

        @Test
        @DisplayName("Should create exception with cause")
        void shouldCreateExceptionWithCause() {
            RuntimeException cause = new RuntimeException("Root cause");
            PaymentException exception = new PaymentException("PAY_003", "Error", cause);

            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getDetails()).isEqualTo("Root cause");
        }

        @Test
        @DisplayName("Should create validation exception via static factory")
        void shouldCreateValidationException() {
            PaymentException exception = PaymentException.validation("Invalid amount");

            assertThat(exception.getErrorCode()).isEqualTo("PAY_001");
            assertThat(exception.getMessage()).isEqualTo("Invalid amount");
        }

        @Test
        @DisplayName("Should create not found exception via static factory")
        void shouldCreateNotFoundException() {
            PaymentException exception = PaymentException.notFound("Payment", "PAY_123");

            assertThat(exception.getErrorCode()).isEqualTo("PAY_002");
            assertThat(exception.getMessage()).contains("Payment");
            assertThat(exception.getMessage()).contains("PAY_123");
        }

        @Test
        @DisplayName("Should create provider unavailable exception via static factory")
        void shouldCreateProviderUnavailableException() {
            PaymentException exception = PaymentException.providerUnavailable("PROVIDER_A");

            assertThat(exception.getErrorCode()).isEqualTo("PAY_004");
            assertThat(exception.getMessage()).contains("PROVIDER_A");
        }

        @Test
        @DisplayName("Should create processing error exception via static factory")
        void shouldCreateProcessingErrorException() {
            PaymentException exception = PaymentException.processingError("Timeout");

            assertThat(exception.getErrorCode()).isEqualTo("PAY_005");
            assertThat(exception.getMessage()).isEqualTo("Timeout");
        }
    }

    @Nested
    @DisplayName("IdempotencyException Tests")
    class IdempotencyExceptionTests {

        @Test
        @DisplayName("Should create exception with message")
        void shouldCreateExceptionWithMessage() {
            IdempotencyException exception = new IdempotencyException("Duplicate request");

            assertThat(exception.getMessage()).isEqualTo("Duplicate request");
            assertThat(exception.getIdempotencyKey()).isNull();
        }

        @Test
        @DisplayName("Should create exception with message and idempotency key")
        void shouldCreateExceptionWithIdempotencyKey() {
            IdempotencyException exception = new IdempotencyException(
                    "Duplicate request detected",
                    "IDEMP_KEY_001"
            );

            assertThat(exception.getIdempotencyKey()).isEqualTo("IDEMP_KEY_001");
            assertThat(exception.getMessage()).isEqualTo("Duplicate request detected");
        }

        @Test
        @DisplayName("Should return correct error code")
        void shouldReturnCorrectErrorCode() {
            IdempotencyException exception = new IdempotencyException("Test");

            assertThat(exception.getErrorCode()).isEqualTo("PAY_003");
        }

        @Test
        @DisplayName("Should create conflict exception via static factory")
        void shouldCreateConflictException() {
            IdempotencyException exception = IdempotencyException.conflict("IDEMP_KEY_002");

            assertThat(exception.getIdempotencyKey()).isEqualTo("IDEMP_KEY_002");
            assertThat(exception.getMessage()).contains("IDEMP_KEY_002");
        }

        @Test
        @DisplayName("Should create in-progress exception via static factory")
        void shouldCreateInProgressException() {
            IdempotencyException exception = IdempotencyException.inProgress("IDEMP_KEY_003");

            assertThat(exception.getIdempotencyKey()).isEqualTo("IDEMP_KEY_003");
            assertThat(exception.getMessage()).contains("currently being processed");
        }
    }

    @Nested
    @DisplayName("Exception Inheritance Tests")
    class ExceptionInheritanceTests {

        @Test
        @DisplayName("PaymentException should extend RuntimeException")
        void paymentExceptionShouldExtendRuntimeException() {
            PaymentException exception = new PaymentException("ERR", "Test");

            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("IdempotencyException should extend RuntimeException")
        void idempotencyExceptionShouldExtendRuntimeException() {
            IdempotencyException exception = new IdempotencyException("Test");

            assertThat(exception).isInstanceOf(RuntimeException.class);
        }
    }
}

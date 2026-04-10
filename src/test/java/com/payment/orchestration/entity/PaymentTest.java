package com.payment.orchestration.entity;

import com.payment.orchestration.enums.PaymentMethod;
import com.payment.orchestration.enums.PaymentStatus;
import com.payment.orchestration.enums.ProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Payment entity.
 */
class PaymentTest {

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .paymentId("PAY_TEST_123")
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .paymentMethod(PaymentMethod.CARD)
                .status(PaymentStatus.PENDING)
                .idempotencyKey("IDEMP_123")
                .build();
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create payment with builder")
        void shouldCreatePaymentWithBuilder() {
            Payment newPayment = Payment.builder()
                    .paymentId("PAY_NEW_001")
                    .amount(new BigDecimal("500.00"))
                    .currency("USD")
                    .paymentMethod(PaymentMethod.UPI)
                    .status(PaymentStatus.PENDING)
                    .idempotencyKey("IDEMP_NEW")
                    .upiId("user@bank")
                    .build();

            assertThat(newPayment.getPaymentId()).isEqualTo("PAY_NEW_001");
            assertThat(newPayment.getAmount()).isEqualByComparingTo("500.00");
            assertThat(newPayment.getCurrency()).isEqualTo("USD");
            assertThat(newPayment.getPaymentMethod()).isEqualTo(PaymentMethod.UPI);
            assertThat(newPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(newPayment.getUpiId()).isEqualTo("user@bank");
        }

        @Test
        @DisplayName("Should handle all payment fields")
        void shouldHandleAllPaymentFields() {
            LocalDateTime now = LocalDateTime.now();
            
            Payment fullPayment = Payment.builder()
                    .id(1L)
                    .paymentId("PAY_FULL_001")
                    .amount(new BigDecimal("2500.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .status(PaymentStatus.SUCCESS)
                    .idempotencyKey("IDEMP_FULL")
                    .maskedCardNumber("**** **** **** 4242")
                    .provider(ProviderType.PROVIDER_A)
                    .providerReference("PROV_REF_001")
                    .retryCount(2)
                    .providerResponseCode("00")
                    .providerResponseMessage("Success")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            assertThat(fullPayment.getId()).isEqualTo(1L);
            assertThat(fullPayment.getProvider()).isEqualTo(ProviderType.PROVIDER_A);
            assertThat(fullPayment.getRetryCount()).isEqualTo(2);
            assertThat(fullPayment.getProviderResponseCode()).isEqualTo("00");
        }
    }

    @Nested
    @DisplayName("Card Masking Tests")
    class CardMaskingTests {

        @Test
        @DisplayName("Should mask 16 digit card number")
        void shouldMask16DigitCardNumber() {
            String masked = Payment.maskCardNumber("4111111111111111");

            assertThat(masked).isEqualTo("**** **** **** 1111");
        }

        @Test
        @DisplayName("Should return masked format for null card number")
        void shouldReturnMaskedFormatForNullCardNumber() {
            String masked = Payment.maskCardNumber(null);

            assertThat(masked).isEqualTo("****");
        }

        @Test
        @DisplayName("Should return masked format for short card number")
        void shouldReturnMaskedFormatForShortCardNumber() {
            String masked = Payment.maskCardNumber("123");

            assertThat(masked).isEqualTo("****");
        }

        @Test
        @DisplayName("Should handle 4 digit card number")
        void shouldHandle4DigitCardNumber() {
            String masked = Payment.maskCardNumber("1234");

            assertThat(masked).isEqualTo("**** **** **** 1234");
        }

        @ParameterizedTest
        @DisplayName("Should correctly mask various card formats")
        @MethodSource("cardMaskingDataProvider")
        void shouldCorrectlyMaskVariousFormats(String input, String expected) {
            String masked = Payment.maskCardNumber(input);

            assertThat(masked).isEqualTo(expected);
        }

        static Stream<Arguments> cardMaskingDataProvider() {
            return Stream.of(
                    Arguments.of("4111111111111111", "**** **** **** 1111"),
                    Arguments.of("5555555555554444", "**** **** **** 4444"),
                    Arguments.of("378282246310005", "**** **** **** 0005"),
                    Arguments.of("6011111111111117", "**** **** **** 1117")
            );
        }
    }

    @Nested
    @DisplayName("Can Retry Tests")
    class CanRetryTests {

        @Test
        @DisplayName("Should allow retry for pending payment under limit")
        void shouldAllowRetryForPendingPaymentUnderLimit() {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setRetryCount(1);
            payment.setMaxRetries(3);

            assertThat(payment.canRetry()).isTrue();
        }

        @Test
        @DisplayName("Should allow retry for RETRY status")
        void shouldAllowRetryForRetryStatus() {
            payment.setStatus(PaymentStatus.RETRY);
            payment.setRetryCount(1);
            payment.setMaxRetries(3);

            assertThat(payment.canRetry()).isTrue();
        }

        @Test
        @DisplayName("Should not allow retry when at max attempts")
        void shouldNotAllowRetryWhenAtMaxAttempts() {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setRetryCount(3);
            payment.setMaxRetries(3);

            assertThat(payment.canRetry()).isFalse();
        }

        @Test
        @DisplayName("Should not allow retry for successful payment")
        void shouldNotAllowRetryForSuccessfulPayment() {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setRetryCount(0);
            payment.setMaxRetries(3);

            assertThat(payment.canRetry()).isFalse();
        }

        @Test
        @DisplayName("Should not allow retry for failed payment")
        void shouldNotAllowRetryForFailedPayment() {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setRetryCount(0);
            payment.setMaxRetries(3);

            assertThat(payment.canRetry()).isFalse();
        }
    }

    @Nested
    @DisplayName("Increment Retry Tests")
    class IncrementRetryTests {

        @Test
        @DisplayName("Should increment retry count from 0")
        void shouldIncrementRetryCountFromZero() {
            payment.setRetryCount(0);

            payment.incrementRetryCount();

            assertThat(payment.getRetryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should increment retry count from existing value")
        void shouldIncrementRetryCountFromExistingValue() {
            payment.setRetryCount(2);

            payment.incrementRetryCount();

            assertThat(payment.getRetryCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Mark Completed Tests")
    class MarkCompletedTests {

        @Test
        @DisplayName("Should mark payment as completed with success")
        void shouldMarkPaymentAsCompletedWithSuccess() {
            payment.markCompleted(PaymentStatus.SUCCESS);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should mark payment as completed with failure")
        void shouldMarkPaymentAsCompletedWithFailure() {
            payment.markCompleted(PaymentStatus.FAILED);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getCompletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Add Status History Tests")
    class AddStatusHistoryTests {

        @Test
        @DisplayName("Should add status history entry")
        void shouldAddStatusHistoryEntry() {
            payment.addStatusHistory(PaymentStatus.PROCESSING, "Processing started");

            assertThat(payment.getStatusHistory()).hasSize(1);
            assertThat(payment.getStatusHistory().get(0).getStatus()).isEqualTo(PaymentStatus.PROCESSING);
            assertThat(payment.getStatusHistory().get(0).getMessage()).isEqualTo("Processing started");
        }

        @Test
        @DisplayName("Should add multiple status history entries")
        void shouldAddMultipleStatusHistoryEntries() {
            payment.addStatusHistory(PaymentStatus.PROCESSING, "Processing started");
            payment.addStatusHistory(PaymentStatus.SUCCESS, "Payment completed");

            assertThat(payment.getStatusHistory()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("UPI Payment Tests")
    class UpiPaymentTests {

        @Test
        @DisplayName("Should store UPI ID")
        void shouldStoreUpiId() {
            Payment upiPayment = Payment.builder()
                    .paymentId("PAY_UPI_001")
                    .paymentMethod(PaymentMethod.UPI)
                    .idempotencyKey("IDEMP_UPI")
                    .upiId("user@oksbi")
                    .build();

            assertThat(upiPayment.getUpiId()).isEqualTo("user@oksbi");
        }
    }

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should have default retry count of 0")
        void shouldHaveDefaultRetryCountOfZero() {
            Payment newPayment = Payment.builder()
                    .paymentId("PAY_DEFAULT")
                    .idempotencyKey("IDEMP_DEFAULT")
                    .build();

            assertThat(newPayment.getRetryCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should have default max retries of 3")
        void shouldHaveDefaultMaxRetriesOfThree() {
            Payment newPayment = Payment.builder()
                    .paymentId("PAY_DEFAULT")
                    .idempotencyKey("IDEMP_DEFAULT")
                    .build();

            assertThat(newPayment.getMaxRetries()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("PrePersist Tests")
    class PrePersistTests {

        @Test
        @DisplayName("Should generate payment ID if null")
        void shouldGeneratePaymentIdIfNull() {
            Payment newPayment = Payment.builder()
                    .idempotencyKey("IDEMP_PRE")
                    .build();

            newPayment.prePersist();

            assertThat(newPayment.getPaymentId()).startsWith("PAY_");
            assertThat(newPayment.getPaymentId()).hasSize(20); // PAY_ + 16 chars
        }

        @Test
        @DisplayName("Should set default status if null")
        void shouldSetDefaultStatusIfNull() {
            Payment newPayment = Payment.builder()
                    .paymentId("PAY_PRE_001")
                    .idempotencyKey("IDEMP_PRE")
                    .build();

            newPayment.prePersist();

            assertThat(newPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("Should not override existing payment ID")
        void shouldNotOverrideExistingPaymentId() {
            Payment newPayment = Payment.builder()
                    .paymentId("PAY_EXISTING")
                    .idempotencyKey("IDEMP_PRE")
                    .build();

            newPayment.prePersist();

            assertThat(newPayment.getPaymentId()).isEqualTo("PAY_EXISTING");
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should have toString implementation")
        void shouldHaveToStringImplementation() {
            String str = payment.toString();

            assertThat(str).isNotBlank();
        }
    }
}

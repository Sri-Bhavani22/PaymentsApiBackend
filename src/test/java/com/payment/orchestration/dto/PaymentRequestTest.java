package com.payment.orchestration.dto;

import com.payment.orchestration.enums.PaymentMethod;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PaymentRequest DTO validation.
 */
class PaymentRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Valid Request Tests")
    class ValidRequestTests {

        @Test
        @DisplayName("Should validate correct card payment request")
        void shouldValidateCorrectCardPaymentRequest() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("1000.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should validate correct UPI payment request")
        void shouldValidateCorrectUpiPaymentRequest() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("500.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.UPI)
                    .upiId("user@oksbi")
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should validate correct net banking request")
        void shouldValidateCorrectNetBankingRequest() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("2000.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.NET_BANKING)
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should validate correct wallet request")
        void shouldValidateCorrectWalletRequest() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("200.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.WALLET)
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Amount Validation Tests")
    class AmountValidationTests {

        @Test
        @DisplayName("Should reject null amount")
        void shouldRejectNullAmount() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(null)
                    .currency("INR")
                    .paymentMethod(PaymentMethod.UPI)
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            assertThat(violations).isNotEmpty();
            assertThat(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("amount")))
                    .isTrue();
        }

        @Test
        @DisplayName("Should reject zero amount")
        void shouldRejectZeroAmount() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(BigDecimal.ZERO)
                    .currency("INR")
                    .paymentMethod(PaymentMethod.UPI)
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Should accept positive amount")
        void shouldAcceptPositiveAmount() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("0.01"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.UPI)
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            boolean hasAmountViolation = violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
            assertThat(hasAmountViolation).isFalse();
        }
    }

    @Nested
    @DisplayName("Currency Validation Tests")
    class CurrencyValidationTests {

        @Test
        @DisplayName("Should reject null currency")
        void shouldRejectNullCurrency() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .currency(null)
                    .paymentMethod(PaymentMethod.UPI)
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Should reject empty currency")
        void shouldRejectEmptyCurrency() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .currency("")
                    .paymentMethod(PaymentMethod.UPI)
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            assertThat(violations).isNotEmpty();
        }

        @ParameterizedTest
        @DisplayName("Should accept valid currency codes")
        @ValueSource(strings = {"INR", "USD", "EUR", "GBP"})
        void shouldAcceptValidCurrencyCodes(String currency) {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .currency(currency)
                    .paymentMethod(PaymentMethod.UPI)
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            boolean hasCurrencyViolation = violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("currency"));
            assertThat(hasCurrencyViolation).isFalse();
        }
    }

    @Nested
    @DisplayName("Payment Method Validation Tests")
    class PaymentMethodValidationTests {

        @Test
        @DisplayName("Should reject null payment method")
        void shouldRejectNullPaymentMethod() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .currency("INR")
                    .paymentMethod(null)
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Card Details Validation Tests")
    class CardDetailsValidationTests {

        @Test
        @DisplayName("Should validate card number length")
        void shouldValidateCardNumberLength() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            boolean hasCardViolation = violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("cardNumber"));
            assertThat(hasCardViolation).isFalse();
        }

        @Test
        @DisplayName("Should validate expiry date format")
        void shouldValidateExpiryDateFormat() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            boolean hasExpiryViolation = violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("expiryDate"));
            assertThat(hasExpiryViolation).isFalse();
        }

        @Test
        @DisplayName("Should reject invalid expiry date format")
        void shouldRejectInvalidExpiryDateFormat() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .cardNumber("4111111111111111")
                    .expiryDate("1225")  // Invalid format
                    .cvv("123")
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            boolean hasExpiryViolation = violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("expiryDate"));
            assertThat(hasExpiryViolation).isTrue();
        }

        @Test
        @DisplayName("Should validate CVV format")
        void shouldValidateCvvFormat() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            boolean hasCvvViolation = violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("cvv"));
            assertThat(hasCvvViolation).isFalse();
        }
    }

    @Nested
    @DisplayName("UPI ID Validation Tests")
    class UpiIdValidationTests {

        @Test
        @DisplayName("Should accept valid UPI ID")
        void shouldAcceptValidUpiId() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.UPI)
                    .upiId("user@oksbi")
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            boolean hasUpiViolation = violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("upiId"));
            assertThat(hasUpiViolation).isFalse();
        }

        @Test
        @DisplayName("Should reject invalid UPI ID format")
        void shouldRejectInvalidUpiIdFormat() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.UPI)
                    .upiId("invalid-upi-id")
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            boolean hasUpiViolation = violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("upiId"));
            assertThat(hasUpiViolation).isTrue();
        }
    }

    @Nested
    @DisplayName("Contact Details Validation Tests")
    class ContactDetailsValidationTests {

        @Test
        @DisplayName("Should accept valid email")
        void shouldAcceptValidEmail() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.WALLET)
                    .customerEmail("user@example.com")
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            boolean hasEmailViolation = violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("customerEmail"));
            assertThat(hasEmailViolation).isFalse();
        }

        @Test
        @DisplayName("Should reject invalid email")
        void shouldRejectInvalidEmail() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.WALLET)
                    .customerEmail("invalid-email")
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            boolean hasEmailViolation = violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("customerEmail"));
            assertThat(hasEmailViolation).isTrue();
        }

        @Test
        @DisplayName("Should accept valid phone number")
        void shouldAcceptValidPhoneNumber() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.WALLET)
                    .customerPhone("+919876543210")
                    .build();

            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            boolean hasPhoneViolation = violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("customerPhone"));
            assertThat(hasPhoneViolation).isFalse();
        }
    }

    @Nested
    @DisplayName("No Args Constructor Tests")
    class NoArgsConstructorTests {

        @Test
        @DisplayName("Should create empty request with no-args constructor")
        void shouldCreateEmptyRequest() {
            PaymentRequest request = new PaymentRequest();

            assertThat(request).isNotNull();
            assertThat(request.getAmount()).isNull();
            assertThat(request.getCurrency()).isNull();
        }
    }
}

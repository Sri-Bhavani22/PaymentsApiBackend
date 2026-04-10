package com.payment.orchestration.provider;

import com.payment.orchestration.entity.Payment;
import com.payment.orchestration.enums.PaymentMethod;
import com.payment.orchestration.enums.PaymentStatus;
import com.payment.orchestration.enums.ProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ProviderA.
 */
class ProviderATest {

    private ProviderA providerA;
    private Payment cardPayment;

    @BeforeEach
    void setUp() {
        providerA = new ProviderA();
        ReflectionTestUtils.setField(providerA, "timeoutMs", 5000L);
        ReflectionTestUtils.setField(providerA, "enabled", true);

        cardPayment = Payment.builder()
                .paymentId("PAY_TEST_123")
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .paymentMethod(PaymentMethod.CARD)
                .status(PaymentStatus.PENDING)
                .idempotencyKey("IDEMP_123")
                .maskedCardNumber("**** **** **** 1111")
                .build();
    }

    @Nested
    @DisplayName("Provider Type Tests")
    class ProviderTypeTests {

        @Test
        @DisplayName("Should return correct provider type")
        void shouldReturnCorrectProviderType() {
            assertThat(providerA.getProviderType()).isEqualTo(ProviderType.PROVIDER_A);
        }

        @Test
        @DisplayName("Should return correct provider name")
        void shouldReturnCorrectProviderName() {
            assertThat(providerA.getProviderName()).isEqualTo("Provider A - Card & Net Banking Gateway");
        }
    }

    @Nested
    @DisplayName("Supported Payment Methods Tests")
    class SupportedPaymentMethodsTests {

        @Test
        @DisplayName("Should support CARD payments")
        void shouldSupportCardPayments() {
            Set<PaymentMethod> supported = providerA.getSupportedPaymentMethods();
            assertThat(supported).contains(PaymentMethod.CARD);
        }

        @Test
        @DisplayName("Should support NET_BANKING payments")
        void shouldSupportNetBankingPayments() {
            Set<PaymentMethod> supported = providerA.getSupportedPaymentMethods();
            assertThat(supported).contains(PaymentMethod.NET_BANKING);
        }

        @Test
        @DisplayName("Should NOT support UPI payments")
        void shouldNotSupportUpiPayments() {
            Set<PaymentMethod> supported = providerA.getSupportedPaymentMethods();
            assertThat(supported).doesNotContain(PaymentMethod.UPI);
        }

        @Test
        @DisplayName("Should NOT support WALLET payments")
        void shouldNotSupportWalletPayments() {
            Set<PaymentMethod> supported = providerA.getSupportedPaymentMethods();
            assertThat(supported).doesNotContain(PaymentMethod.WALLET);
        }

        @Test
        @DisplayName("Should return correct supported methods set")
        void shouldReturnCorrectSupportedMethodsSet() {
            Set<PaymentMethod> supported = providerA.getSupportedPaymentMethods();

            assertThat(supported).containsExactlyInAnyOrder(
                    PaymentMethod.CARD, PaymentMethod.NET_BANKING);
        }
    }

    @Nested
    @DisplayName("Process Payment Tests")
    class ProcessPaymentTests {

        @Test
        @DisplayName("Should process payment and return response")
        void shouldProcessPaymentAndReturnResponse() {
            ProviderResponse response = providerA.processPayment(cardPayment);

            assertThat(response).isNotNull();
            assertThat(response.getProviderType()).isEqualTo(ProviderType.PROVIDER_A);
        }

        @Test
        @DisplayName("Should return unavailable when provider disabled")
        void shouldReturnUnavailableWhenDisabled() {
            ReflectionTestUtils.setField(providerA, "enabled", false);

            ProviderResponse response = providerA.processPayment(cardPayment);

            assertThat(response.isSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("Check Payment Status Tests")
    class CheckPaymentStatusTests {

        @Test
        @DisplayName("Should check payment status")
        void shouldCheckPaymentStatus() {
            ProviderResponse response = providerA.checkPaymentStatus("PROV_REF_123");

            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getProviderReference()).isEqualTo("PROV_REF_123");
        }
    }

    @Nested
    @DisplayName("Refund Payment Tests")
    class RefundPaymentTests {

        @Test
        @DisplayName("Should process refund")
        void shouldProcessRefund() {
            ProviderResponse response = providerA.refundPayment(cardPayment);

            assertThat(response).isNotNull();
            assertThat(response.getProviderType()).isEqualTo(ProviderType.PROVIDER_A);
        }
    }

    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return healthy when enabled")
        void shouldReturnHealthyWhenEnabled() {
            assertThat(providerA.isHealthy()).isTrue();
        }

        @Test
        @DisplayName("Should return unhealthy when disabled")
        void shouldReturnUnhealthyWhenDisabled() {
            ReflectionTestUtils.setField(providerA, "enabled", false);

            assertThat(providerA.isHealthy()).isFalse();
        }
    }
}

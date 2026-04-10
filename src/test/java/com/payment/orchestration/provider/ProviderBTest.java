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
 * Unit tests for ProviderB.
 */
class ProviderBTest {

    private ProviderB providerB;
    private Payment upiPayment;

    @BeforeEach
    void setUp() {
        providerB = new ProviderB();
        ReflectionTestUtils.setField(providerB, "timeoutMs", 3000L);
        ReflectionTestUtils.setField(providerB, "enabled", true);

        upiPayment = Payment.builder()
                .paymentId("PAY_UPI_123")
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .paymentMethod(PaymentMethod.UPI)
                .status(PaymentStatus.PENDING)
                .idempotencyKey("IDEMP_UPI_123")
                .upiId("user@bank")
                .build();
    }

    @Nested
    @DisplayName("Provider Type Tests")
    class ProviderTypeTests {

        @Test
        @DisplayName("Should return correct provider type")
        void shouldReturnCorrectProviderType() {
            assertThat(providerB.getProviderType()).isEqualTo(ProviderType.PROVIDER_B);
        }

        @Test
        @DisplayName("Should return correct provider name")
        void shouldReturnCorrectProviderName() {
            assertThat(providerB.getProviderName()).isEqualTo("Provider B - UPI & Wallet Gateway");
        }
    }

    @Nested
    @DisplayName("Supported Payment Methods Tests")
    class SupportedPaymentMethodsTests {

        @Test
        @DisplayName("Should support UPI payments")
        void shouldSupportUpiPayments() {
            Set<PaymentMethod> supported = providerB.getSupportedPaymentMethods();
            assertThat(supported).contains(PaymentMethod.UPI);
        }

        @Test
        @DisplayName("Should support WALLET payments")
        void shouldSupportWalletPayments() {
            Set<PaymentMethod> supported = providerB.getSupportedPaymentMethods();
            assertThat(supported).contains(PaymentMethod.WALLET);
        }

        @Test
        @DisplayName("Should NOT support CARD payments")
        void shouldNotSupportCardPayments() {
            Set<PaymentMethod> supported = providerB.getSupportedPaymentMethods();
            assertThat(supported).doesNotContain(PaymentMethod.CARD);
        }

        @Test
        @DisplayName("Should NOT support NET_BANKING payments")
        void shouldNotSupportNetBankingPayments() {
            Set<PaymentMethod> supported = providerB.getSupportedPaymentMethods();
            assertThat(supported).doesNotContain(PaymentMethod.NET_BANKING);
        }

        @Test
        @DisplayName("Should return correct supported methods set")
        void shouldReturnCorrectSupportedMethodsSet() {
            Set<PaymentMethod> supported = providerB.getSupportedPaymentMethods();

            assertThat(supported).containsExactlyInAnyOrder(
                    PaymentMethod.UPI, PaymentMethod.WALLET);
        }
    }

    @Nested
    @DisplayName("Process Payment Tests")
    class ProcessPaymentTests {

        @Test
        @DisplayName("Should process UPI payment and return response")
        void shouldProcessUpiPaymentAndReturnResponse() {
            ProviderResponse response = providerB.processPayment(upiPayment);

            assertThat(response).isNotNull();
            assertThat(response.getProviderType()).isEqualTo(ProviderType.PROVIDER_B);
        }

        @Test
        @DisplayName("Should return unavailable when provider disabled")
        void shouldReturnUnavailableWhenDisabled() {
            ReflectionTestUtils.setField(providerB, "enabled", false);

            ProviderResponse response = providerB.processPayment(upiPayment);

            assertThat(response.isSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("Check Payment Status Tests")
    class CheckPaymentStatusTests {

        @Test
        @DisplayName("Should check payment status")
        void shouldCheckPaymentStatus() {
            ProviderResponse response = providerB.checkPaymentStatus("PROV_B_REF_789");

            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getProviderReference()).isEqualTo("PROV_B_REF_789");
        }
    }

    @Nested
    @DisplayName("Refund Payment Tests")
    class RefundPaymentTests {

        @Test
        @DisplayName("Should process UPI refund")
        void shouldProcessUpiRefund() {
            ProviderResponse response = providerB.refundPayment(upiPayment);

            assertThat(response).isNotNull();
            assertThat(response.getProviderType()).isEqualTo(ProviderType.PROVIDER_B);
        }
    }

    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return healthy when enabled")
        void shouldReturnHealthyWhenEnabled() {
            assertThat(providerB.isHealthy()).isTrue();
        }

        @Test
        @DisplayName("Should return unhealthy when disabled")
        void shouldReturnUnhealthyWhenDisabled() {
            ReflectionTestUtils.setField(providerB, "enabled", false);

            assertThat(providerB.isHealthy()).isFalse();
        }
    }
}

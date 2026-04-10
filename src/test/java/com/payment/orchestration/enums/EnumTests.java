package com.payment.orchestration.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for enum classes.
 */
class EnumTests {

    @Nested
    @DisplayName("PaymentStatus Tests")
    class PaymentStatusTests {

        @Test
        @DisplayName("Should have all expected statuses")
        void shouldHaveAllExpectedStatuses() {
            assertThat(PaymentStatus.values())
                    .containsExactlyInAnyOrder(
                            PaymentStatus.PENDING,
                            PaymentStatus.PROCESSING,
                            PaymentStatus.SUCCESS,
                            PaymentStatus.FAILED,
                            PaymentStatus.RETRY,
                            PaymentStatus.REFUNDED,
                            PaymentStatus.CANCELLED
                    );
        }

        @Test
        @DisplayName("SUCCESS should be terminal")
        void successShouldBeTerminal() {
            assertThat(PaymentStatus.SUCCESS.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("FAILED should be terminal")
        void failedShouldBeTerminal() {
            assertThat(PaymentStatus.FAILED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("REFUNDED should be terminal")
        void refundedShouldBeTerminal() {
            assertThat(PaymentStatus.REFUNDED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("CANCELLED should be terminal")
        void cancelledShouldBeTerminal() {
            assertThat(PaymentStatus.CANCELLED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("PENDING should not be terminal")
        void pendingShouldNotBeTerminal() {
            assertThat(PaymentStatus.PENDING.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("PROCESSING should not be terminal")
        void processingShouldNotBeTerminal() {
            assertThat(PaymentStatus.PROCESSING.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("RETRY should not be terminal")
        void retryShouldNotBeTerminal() {
            assertThat(PaymentStatus.RETRY.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("Should check if status is retryable")
        void shouldCheckIfStatusIsRetryable() {
            assertThat(PaymentStatus.PENDING.isRetryable()).isTrue();
            assertThat(PaymentStatus.RETRY.isRetryable()).isTrue();
            assertThat(PaymentStatus.PROCESSING.isRetryable()).isTrue();
            assertThat(PaymentStatus.SUCCESS.isRetryable()).isFalse();
            assertThat(PaymentStatus.FAILED.isRetryable()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(PaymentStatus.class)
        @DisplayName("Each status should have a description")
        void eachStatusShouldHaveADescription(PaymentStatus status) {
            assertThat(status.getDescription()).isNotBlank();
        }

        @Test
        @DisplayName("Should parse from string")
        void shouldParseFromString() {
            assertThat(PaymentStatus.valueOf("SUCCESS")).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(PaymentStatus.valueOf("FAILED")).isEqualTo(PaymentStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("PaymentMethod Tests")
    class PaymentMethodTests {

        @Test
        @DisplayName("Should have all expected methods")
        void shouldHaveAllExpectedMethods() {
            assertThat(PaymentMethod.values())
                    .containsExactlyInAnyOrder(
                            PaymentMethod.CARD,
                            PaymentMethod.UPI,
                            PaymentMethod.NET_BANKING,
                            PaymentMethod.WALLET
                    );
        }

        @Test
        @DisplayName("CARD should have correct display name")
        void cardShouldHaveCorrectDisplayName() {
            assertThat(PaymentMethod.CARD.getDisplayName()).containsIgnoringCase("Card");
        }

        @Test
        @DisplayName("UPI should have correct display name")
        void upiShouldHaveCorrectDisplayName() {
            assertThat(PaymentMethod.UPI.getDisplayName()).containsIgnoringCase("UPI");
        }

        @Test
        @DisplayName("NET_BANKING should have correct display name")
        void netBankingShouldHaveCorrectDisplayName() {
            assertThat(PaymentMethod.NET_BANKING.getDisplayName()).containsIgnoringCase("Banking");
        }

        @Test
        @DisplayName("WALLET should have correct display name")
        void walletShouldHaveCorrectDisplayName() {
            assertThat(PaymentMethod.WALLET.getDisplayName()).containsIgnoringCase("Wallet");
        }

        @Test
        @DisplayName("CARD should have primary provider set")
        void cardShouldHavePrimaryProvider() {
            assertThat(PaymentMethod.CARD.getPrimaryProvider()).isEqualTo("PROVIDER_A");
        }

        @Test
        @DisplayName("UPI should have primary provider set")
        void upiShouldHavePrimaryProvider() {
            assertThat(PaymentMethod.UPI.getPrimaryProvider()).isEqualTo("PROVIDER_B");
        }

        @Test
        @DisplayName("CARD should require card details")
        void cardShouldRequireCardDetails() {
            assertThat(PaymentMethod.CARD.requiresCardDetails()).isTrue();
        }

        @Test
        @DisplayName("UPI should not require card details")
        void upiShouldNotRequireCardDetails() {
            assertThat(PaymentMethod.UPI.requiresCardDetails()).isFalse();
        }

        @Test
        @DisplayName("Should parse from string")
        void shouldParseFromString() {
            assertThat(PaymentMethod.valueOf("CARD")).isEqualTo(PaymentMethod.CARD);
            assertThat(PaymentMethod.valueOf("UPI")).isEqualTo(PaymentMethod.UPI);
        }
    }

    @Nested
    @DisplayName("ProviderType Tests")
    class ProviderTypeTests {

        @Test
        @DisplayName("Should have all expected providers")
        void shouldHaveAllExpectedProviders() {
            assertThat(ProviderType.values())
                    .containsExactlyInAnyOrder(
                            ProviderType.PROVIDER_A,
                            ProviderType.PROVIDER_B
                    );
        }

        @ParameterizedTest
        @EnumSource(ProviderType.class)
        @DisplayName("Each provider should have a display name")
        void eachProviderShouldHaveADisplayName(ProviderType provider) {
            assertThat(provider.getDisplayName()).isNotBlank();
        }

        @Test
        @DisplayName("PROVIDER_A should be enabled")
        void providerAShouldBeEnabled() {
            assertThat(ProviderType.PROVIDER_A.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("PROVIDER_B should be enabled")
        void providerBShouldBeEnabled() {
            assertThat(ProviderType.PROVIDER_B.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("PROVIDER_A failover should be PROVIDER_B")
        void providerAFailoverShouldBeProviderB() {
            assertThat(ProviderType.PROVIDER_A.getFailoverProvider())
                    .isEqualTo(ProviderType.PROVIDER_B);
        }

        @Test
        @DisplayName("PROVIDER_B failover should be PROVIDER_A")
        void providerBFailoverShouldBeProviderA() {
            assertThat(ProviderType.PROVIDER_B.getFailoverProvider())
                    .isEqualTo(ProviderType.PROVIDER_A);
        }
    }

    @Nested
    @DisplayName("Enum Ordinal Tests")
    class EnumOrdinalTests {

        @Test
        @DisplayName("PaymentStatus ordinals should be stable for database storage")
        void paymentStatusOrdinalsShouldBeStable() {
            assertThat(PaymentStatus.PENDING.ordinal()).isGreaterThanOrEqualTo(0);
            assertThat(PaymentStatus.SUCCESS.ordinal()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("PaymentMethod ordinals should be stable")
        void paymentMethodOrdinalsShouldBeStable() {
            assertThat(PaymentMethod.CARD.ordinal()).isGreaterThanOrEqualTo(0);
            assertThat(PaymentMethod.UPI.ordinal()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Enum Name Tests")
    class EnumNameTests {

        @Test
        @DisplayName("PaymentStatus names should be uppercase")
        void paymentStatusNamesShouldBeUppercase() {
            for (PaymentStatus status : PaymentStatus.values()) {
                assertThat(status.name()).isEqualTo(status.name().toUpperCase());
            }
        }

        @Test
        @DisplayName("PaymentMethod names should be uppercase")
        void paymentMethodNamesShouldBeUppercase() {
            for (PaymentMethod method : PaymentMethod.values()) {
                assertThat(method.name()).isEqualTo(method.name().toUpperCase());
            }
        }

        @Test
        @DisplayName("ProviderType names should be uppercase")
        void providerTypeNamesShouldBeUppercase() {
            for (ProviderType type : ProviderType.values()) {
                assertThat(type.name()).isEqualTo(type.name().toUpperCase());
            }
        }
    }
}

package com.payment.orchestration.provider;

import com.payment.orchestration.enums.ProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ProviderResponse.
 */
class ProviderResponseTest {

    @Nested
    @DisplayName("Success Response Tests")
    class SuccessResponseTests {

        @Test
        @DisplayName("Should create success response with builder")
        void shouldCreateSuccessResponseWithBuilder() {
            LocalDateTime timestamp = LocalDateTime.now();

            ProviderResponse response = ProviderResponse.builder()
                    .success(true)
                    .providerType(ProviderType.PROVIDER_A)
                    .providerReference("PROV_REF_001")
                    .responseCode("00")
                    .responseMessage("Transaction approved")
                    .processingTimeMs(150L)
                    .timestamp(timestamp)
                    .build();

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getProviderType()).isEqualTo(ProviderType.PROVIDER_A);
            assertThat(response.getProviderReference()).isEqualTo("PROV_REF_001");
            assertThat(response.getResponseCode()).isEqualTo("00");
            assertThat(response.getResponseMessage()).isEqualTo("Transaction approved");
            assertThat(response.getProcessingTimeMs()).isEqualTo(150L);
            assertThat(response.getTimestamp()).isEqualTo(timestamp);
        }

        @Test
        @DisplayName("Should create success response using static factory method")
        void shouldCreateSuccessResponseUsingStaticMethod() {
            ProviderResponse response = ProviderResponse.success(
                    ProviderType.PROVIDER_B,
                    "PROV_B_REF_123",
                    "Successfully processed"
            );

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getProviderType()).isEqualTo(ProviderType.PROVIDER_B);
            assertThat(response.getProviderReference()).isEqualTo("PROV_B_REF_123");
        }
    }

    @Nested
    @DisplayName("Failure Response Tests")
    class FailureResponseTests {

        @Test
        @DisplayName("Should create failure response with builder")
        void shouldCreateFailureResponseWithBuilder() {
            ProviderResponse response = ProviderResponse.builder()
                    .success(false)
                    .providerType(ProviderType.PROVIDER_A)
                    .responseCode("51")
                    .responseMessage("Insufficient funds")
                    .retryable(false)
                    .build();

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getResponseCode()).isEqualTo("51");
            assertThat(response.getResponseMessage()).isEqualTo("Insufficient funds");
            assertThat(response.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("Should create failure response using static factory method")
        void shouldCreateFailureResponseUsingStaticMethod() {
            ProviderResponse response = ProviderResponse.failure(
                    ProviderType.PROVIDER_A,
                    "500",
                    "Internal server error"
            );

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getResponseCode()).isEqualTo("500");
        }
    }

    @Nested
    @DisplayName("Retriable Response Tests")
    class RetriableResponseTests {

        @Test
        @DisplayName("Should mark timeout as retriable")
        void shouldMarkTimeoutAsRetriable() {
            ProviderResponse response = ProviderResponse.builder()
                    .success(false)
                    .providerType(ProviderType.PROVIDER_A)
                    .responseCode("408")
                    .responseMessage("Request timeout")
                    .retryable(true)
                    .build();

            assertThat(response.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("Should mark declined as not retriable")
        void shouldMarkDeclinedAsNotRetriable() {
            ProviderResponse response = ProviderResponse.builder()
                    .success(false)
                    .providerType(ProviderType.PROVIDER_B)
                    .responseCode("05")
                    .responseMessage("Do not honor")
                    .retryable(false)
                    .build();

            assertThat(response.isRetryable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should have timestamp field")
        void shouldHaveTimestampField() {
            ProviderResponse response = ProviderResponse.builder()
                    .success(true)
                    .providerType(ProviderType.PROVIDER_A)
                    .build();

            assertThat(response.getProviderType()).isEqualTo(ProviderType.PROVIDER_A);
            assertThat(response.getTimestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("No Args Constructor Tests")
    class NoArgsConstructorTests {

        @Test
        @DisplayName("Should create empty response")
        void shouldCreateEmptyResponse() {
            ProviderResponse response = new ProviderResponse();

            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("Additional Data Tests")
    class AdditionalDataTests {

        @Test
        @DisplayName("Should store additional data string")
        void shouldStoreAdditionalDataString() {
            ProviderResponse response = ProviderResponse.builder()
                    .success(true)
                    .providerType(ProviderType.PROVIDER_A)
                    .additionalData("{\"authCode\":\"ABC123\"}")
                    .build();

            assertThat(response.getAdditionalData()).contains("authCode");
        }

        @Test
        @DisplayName("Should handle null additional data")
        void shouldHandleNullAdditionalData() {
            ProviderResponse response = ProviderResponse.builder()
                    .success(true)
                    .providerType(ProviderType.PROVIDER_B)
                    .additionalData(null)
                    .build();

            assertThat(response.getAdditionalData()).isNull();
        }
    }

    @Nested
    @DisplayName("Retry After Tests")
    class RetryAfterTests {

        @Test
        @DisplayName("Should store retry after value")
        void shouldStoreRetryAfterValue() {
            ProviderResponse response = ProviderResponse.builder()
                    .success(false)
                    .providerType(ProviderType.PROVIDER_A)
                    .retryable(true)
                    .retryAfterMs(5000L)
                    .build();

            assertThat(response.getRetryAfterMs()).isEqualTo(5000L);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should have readable toString")
        void shouldHaveReadableToString() {
            ProviderResponse response = ProviderResponse.builder()
                    .success(true)
                    .providerType(ProviderType.PROVIDER_A)
                    .providerReference("REF_123")
                    .build();

            String str = response.toString();

            assertThat(str).contains("success");
            assertThat(str).contains("PROVIDER_A");
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal for same values")
        void shouldBeEqualForSameValues() {
            LocalDateTime timestamp = LocalDateTime.now();

            ProviderResponse response1 = ProviderResponse.builder()
                    .success(true)
                    .providerType(ProviderType.PROVIDER_A)
                    .providerReference("REF_001")
                    .timestamp(timestamp)
                    .build();

            ProviderResponse response2 = ProviderResponse.builder()
                    .success(true)
                    .providerType(ProviderType.PROVIDER_A)
                    .providerReference("REF_001")
                    .timestamp(timestamp)
                    .build();

            assertThat(response1).isEqualTo(response2);
            assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal for different success values")
        void shouldNotBeEqualForDifferentSuccessValues() {
            ProviderResponse response1 = ProviderResponse.builder()
                    .success(true)
                    .providerType(ProviderType.PROVIDER_A)
                    .build();

            ProviderResponse response2 = ProviderResponse.builder()
                    .success(false)
                    .providerType(ProviderType.PROVIDER_A)
                    .build();

            assertThat(response1).isNotEqualTo(response2);
        }
    }
}

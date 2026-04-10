package com.payment.orchestration.provider;

import com.payment.orchestration.enums.ProviderType;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Response from a payment provider.
 * Contains all information about the payment processing result.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderResponse {

    /**
     * Whether the payment was successful
     */
    private boolean success;

    /**
     * Provider type that processed the payment
     */
    private ProviderType providerType;

    /**
     * Provider's reference ID for this transaction
     */
    private String providerReference;

    /**
     * Response code from the provider
     */
    private String responseCode;

    /**
     * Response message from the provider
     */
    private String responseMessage;

    /**
     * Whether this error is retryable
     */
    private boolean retryable;

    /**
     * Suggested wait time before retry (in milliseconds)
     */
    private Long retryAfterMs;

    /**
     * Processing time for the transaction (in milliseconds)
     */
    private Long processingTimeMs;

    /**
     * Timestamp of the response
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Additional provider-specific data
     */
    private String additionalData;

    /**
     * Create a successful response
     */
    public static ProviderResponse success(ProviderType providerType, String reference, String message) {
        return ProviderResponse.builder()
                .success(true)
                .providerType(providerType)
                .providerReference(reference)
                .responseCode("00")
                .responseMessage(message)
                .retryable(false)
                .build();
    }

    /**
     * Create a failed response (non-retryable)
     */
    public static ProviderResponse failure(ProviderType providerType, String code, String message) {
        return ProviderResponse.builder()
                .success(false)
                .providerType(providerType)
                .responseCode(code)
                .responseMessage(message)
                .retryable(false)
                .build();
    }

    /**
     * Create a retryable failure response
     */
    public static ProviderResponse retryableFailure(ProviderType providerType, String code, String message, long retryAfterMs) {
        return ProviderResponse.builder()
                .success(false)
                .providerType(providerType)
                .responseCode(code)
                .responseMessage(message)
                .retryable(true)
                .retryAfterMs(retryAfterMs)
                .build();
    }

    /**
     * Create a timeout response
     */
    public static ProviderResponse timeout(ProviderType providerType) {
        return ProviderResponse.builder()
                .success(false)
                .providerType(providerType)
                .responseCode("TIMEOUT")
                .responseMessage("Provider connection timeout")
                .retryable(true)
                .retryAfterMs(2000L)
                .build();
    }

    /**
     * Create a provider unavailable response
     */
    public static ProviderResponse unavailable(ProviderType providerType) {
        return ProviderResponse.builder()
                .success(false)
                .providerType(providerType)
                .responseCode("503")
                .responseMessage("Provider service unavailable")
                .retryable(true)
                .retryAfterMs(5000L)
                .build();
    }
}

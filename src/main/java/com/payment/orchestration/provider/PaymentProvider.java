package com.payment.orchestration.provider;

import com.payment.orchestration.entity.Payment;
import com.payment.orchestration.enums.PaymentMethod;
import com.payment.orchestration.enums.ProviderType;

import java.util.Set;

/**
 * Interface for payment provider connectors.
 * Each provider implementation handles communication with a specific payment gateway.
 */
public interface PaymentProvider {

    /**
     * Get the provider type identifier
     */
    ProviderType getProviderType();

    /**
     * Get the display name of the provider
     */
    String getProviderName();

    /**
     * Process a payment through this provider
     * 
     * @param payment The payment entity to process
     * @return ProviderResponse containing the result
     */
    ProviderResponse processPayment(Payment payment);

    /**
     * Check the status of a payment with the provider
     * 
     * @param providerReference The provider's reference ID
     * @return ProviderResponse with current status
     */
    ProviderResponse checkPaymentStatus(String providerReference);

    /**
     * Refund a payment
     * 
     * @param payment The payment to refund
     * @return ProviderResponse with refund result
     */
    ProviderResponse refundPayment(Payment payment);

    /**
     * Get the payment methods supported by this provider
     */
    Set<PaymentMethod> getSupportedPaymentMethods();

    /**
     * Check if this provider supports a specific payment method
     */
    default boolean supportsPaymentMethod(PaymentMethod method) {
        return getSupportedPaymentMethods().contains(method);
    }

    /**
     * Check if the provider is currently healthy/available
     */
    boolean isHealthy();

    /**
     * Get the provider's configured timeout in milliseconds
     */
    long getTimeoutMs();

    /**
     * Get success rate of this provider (for routing decisions)
     */
    default double getSuccessRate() {
        return 1.0; // Default to 100% if not tracked
    }
}

package com.payment.orchestration.routing;

import com.payment.orchestration.enums.PaymentMethod;
import com.payment.orchestration.enums.ProviderType;
import com.payment.orchestration.provider.PaymentProvider;

import java.util.List;
import java.util.Optional;

/**
 * Interface for routing payment requests to appropriate providers.
 * Implements routing rules based on payment method and provider availability.
 */
public interface RoutingEngine {

    /**
     * Get the primary provider for a payment method
     * 
     * @param paymentMethod The payment method
     * @return The primary provider for this payment method
     */
    PaymentProvider getPrimaryProvider(PaymentMethod paymentMethod);

    /**
     * Get the failover provider for a given provider
     * 
     * @param providerType The provider that failed
     * @return The failover provider, if available
     */
    Optional<PaymentProvider> getFailoverProvider(ProviderType providerType);

    /**
     * Get the next available provider for a payment method
     * Takes into account provider health and previous attempts
     * 
     * @param paymentMethod The payment method
     * @param excludedProviders Providers that have already been tried
     * @return Next available provider, if any
     */
    Optional<PaymentProvider> getNextAvailableProvider(PaymentMethod paymentMethod, List<ProviderType> excludedProviders);

    /**
     * Get all providers that support a payment method
     * 
     * @param paymentMethod The payment method
     * @return List of all providers supporting this method
     */
    List<PaymentProvider> getAllProvidersForMethod(PaymentMethod paymentMethod);

    /**
     * Get provider by type
     * 
     * @param providerType The provider type
     * @return The provider, if found
     */
    Optional<PaymentProvider> getProvider(ProviderType providerType);

    /**
     * Check if a provider is healthy
     * 
     * @param providerType The provider to check
     * @return true if provider is healthy
     */
    boolean isProviderHealthy(ProviderType providerType);

    /**
     * Get all available providers
     * 
     * @return List of all configured providers
     */
    List<PaymentProvider> getAllProviders();

    /**
     * Get provider with best success rate for a payment method
     * 
     * @param paymentMethod The payment method
     * @return Provider with highest success rate
     */
    PaymentProvider getBestProviderForMethod(PaymentMethod paymentMethod);
}

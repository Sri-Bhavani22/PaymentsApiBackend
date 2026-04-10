package com.payment.orchestration.routing;

import com.payment.orchestration.enums.PaymentMethod;
import com.payment.orchestration.enums.ProviderType;
import com.payment.orchestration.provider.PaymentProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of the RoutingEngine.
 * Routes payments based on configured rules:
 * - CARD, NET_BANKING → Provider A
 * - UPI, WALLET → Provider B
 * 
 * Supports failover to alternate provider when primary fails.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultRoutingEngine implements RoutingEngine {

    private final List<PaymentProvider> providers;

    // Provider lookup map for quick access
    private Map<ProviderType, PaymentProvider> providerMap;

    // Routing rules: PaymentMethod → Primary Provider
    private static final Map<PaymentMethod, ProviderType> ROUTING_RULES = Map.of(
            PaymentMethod.CARD, ProviderType.PROVIDER_A,
            PaymentMethod.NET_BANKING, ProviderType.PROVIDER_A,
            PaymentMethod.UPI, ProviderType.PROVIDER_B,
            PaymentMethod.WALLET, ProviderType.PROVIDER_B
    );

    // Failover mapping: Provider → Failover Provider
    private static final Map<ProviderType, ProviderType> FAILOVER_RULES = Map.of(
            ProviderType.PROVIDER_A, ProviderType.PROVIDER_B,
            ProviderType.PROVIDER_B, ProviderType.PROVIDER_A
    );

    @PostConstruct
    public void init() {
        // Build provider lookup map
        providerMap = providers.stream()
                .collect(Collectors.toMap(
                        PaymentProvider::getProviderType,
                        provider -> provider
                ));

        log.info("RoutingEngine initialized with {} providers: {}",
                providers.size(),
                providers.stream()
                        .map(PaymentProvider::getProviderName)
                        .collect(Collectors.joining(", ")));

        // Log routing rules
        ROUTING_RULES.forEach((method, provider) ->
                log.info("Routing rule: {} → {}", method, provider));
    }

    @Override
    public PaymentProvider getPrimaryProvider(PaymentMethod paymentMethod) {
        ProviderType primaryType = ROUTING_RULES.get(paymentMethod);
        
        if (primaryType == null) {
            log.warn("No routing rule defined for payment method: {}. Using default provider.", paymentMethod);
            primaryType = ProviderType.PROVIDER_A;
        }

        PaymentProvider provider = providerMap.get(primaryType);
        
        if (provider == null) {
            throw new IllegalStateException("Provider not found for type: " + primaryType);
        }

        log.debug("Primary provider for {}: {}", paymentMethod, provider.getProviderName());
        return provider;
    }

    @Override
    public Optional<PaymentProvider> getFailoverProvider(ProviderType providerType) {
        ProviderType failoverType = FAILOVER_RULES.get(providerType);
        
        if (failoverType == null) {
            log.warn("No failover provider configured for: {}", providerType);
            return Optional.empty();
        }

        PaymentProvider failoverProvider = providerMap.get(failoverType);
        
        if (failoverProvider == null || !failoverProvider.isHealthy()) {
            log.warn("Failover provider {} is not available", failoverType);
            return Optional.empty();
        }

        log.info("Failover from {} to {}", providerType, failoverType);
        return Optional.of(failoverProvider);
    }

    @Override
    public Optional<PaymentProvider> getNextAvailableProvider(PaymentMethod paymentMethod, List<ProviderType> excludedProviders) {
        // Get all providers that support this payment method
        List<PaymentProvider> availableProviders = getAllProvidersForMethod(paymentMethod);

        // Filter out excluded and unhealthy providers
        Optional<PaymentProvider> nextProvider = availableProviders.stream()
                .filter(p -> !excludedProviders.contains(p.getProviderType()))
                .filter(PaymentProvider::isHealthy)
                .findFirst();

        if (nextProvider.isPresent()) {
            log.debug("Next available provider for {} (excluding {}): {}",
                    paymentMethod, excludedProviders, nextProvider.get().getProviderName());
        } else {
            log.warn("No available provider for {} after excluding {}", paymentMethod, excludedProviders);
        }

        return nextProvider;
    }

    @Override
    public List<PaymentProvider> getAllProvidersForMethod(PaymentMethod paymentMethod) {
        // Get primary provider
        ProviderType primaryType = ROUTING_RULES.get(paymentMethod);
        ProviderType failoverType = FAILOVER_RULES.get(primaryType);

        List<PaymentProvider> result = new ArrayList<>();

        // Add primary provider first
        if (primaryType != null && providerMap.containsKey(primaryType)) {
            result.add(providerMap.get(primaryType));
        }

        // Add failover provider
        if (failoverType != null && providerMap.containsKey(failoverType)) {
            PaymentProvider failover = providerMap.get(failoverType);
            // Only add if it supports this payment method
            if (failover.supportsPaymentMethod(paymentMethod)) {
                result.add(failover);
            }
        }

        return result;
    }

    @Override
    public Optional<PaymentProvider> getProvider(ProviderType providerType) {
        return Optional.ofNullable(providerMap.get(providerType));
    }

    @Override
    public boolean isProviderHealthy(ProviderType providerType) {
        PaymentProvider provider = providerMap.get(providerType);
        return provider != null && provider.isHealthy();
    }

    @Override
    public List<PaymentProvider> getAllProviders() {
        return new ArrayList<>(providers);
    }

    @Override
    public PaymentProvider getBestProviderForMethod(PaymentMethod paymentMethod) {
        List<PaymentProvider> availableProviders = getAllProvidersForMethod(paymentMethod);

        // Find provider with best success rate
        return availableProviders.stream()
                .filter(PaymentProvider::isHealthy)
                .max(Comparator.comparingDouble(PaymentProvider::getSuccessRate))
                .orElseGet(() -> getPrimaryProvider(paymentMethod));
    }

    /**
     * Get routing statistics for monitoring
     */
    public Map<String, Object> getRoutingStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalProviders", providers.size());
        stats.put("healthyProviders", providers.stream()
                .filter(PaymentProvider::isHealthy)
                .count());

        Map<String, Double> successRates = providers.stream()
                .collect(Collectors.toMap(
                        PaymentProvider::getProviderName,
                        PaymentProvider::getSuccessRate
                ));
        stats.put("successRates", successRates);

        stats.put("routingRules", ROUTING_RULES);
        stats.put("failoverRules", FAILOVER_RULES);

        return stats;
    }

    /**
     * Check if any provider is available for a payment method
     */
    public boolean hasAvailableProvider(PaymentMethod paymentMethod) {
        return getAllProvidersForMethod(paymentMethod).stream()
                .anyMatch(PaymentProvider::isHealthy);
    }
}

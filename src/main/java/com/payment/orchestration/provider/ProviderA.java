package com.payment.orchestration.provider;

import com.payment.orchestration.entity.Payment;
import com.payment.orchestration.enums.PaymentMethod;
import com.payment.orchestration.enums.ProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provider A implementation - Primary provider for CARD and NET_BANKING payments.
 * This is a simulated provider for demonstration purposes.
 * In production, this would integrate with an actual payment gateway.
 */
@Slf4j
@Component
public class ProviderA implements PaymentProvider {

    private static final Set<PaymentMethod> SUPPORTED_METHODS = Set.of(
            PaymentMethod.CARD,
            PaymentMethod.NET_BANKING
    );

    @Value("${payment.providers.provider-a.timeout-ms:5000}")
    private long timeoutMs;

    @Value("${payment.providers.provider-a.enabled:true}")
    private boolean enabled;

    // For simulating random failures and success tracking
    private final Random random = new Random();
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);

    // Simulated failure rate (5% failure rate for demo)
    private static final double FAILURE_RATE = 0.05;
    // Simulated timeout rate (2% timeout rate for demo)
    private static final double TIMEOUT_RATE = 0.02;
    // Simulated transient failure rate (3% - these are retryable)
    private static final double TRANSIENT_FAILURE_RATE = 0.03;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.PROVIDER_A;
    }

    @Override
    public String getProviderName() {
        return "Provider A - Card & Net Banking Gateway";
    }

    @Override
    public ProviderResponse processPayment(Payment payment) {
        long startTime = System.currentTimeMillis();
        totalRequests.incrementAndGet();

        log.info("Provider A processing payment: {} for amount: {} {}",
                payment.getPaymentId(), payment.getAmount(), payment.getCurrency());

        try {
            // Simulate processing time (100-500ms)
            simulateProcessingDelay();

            // Check if provider is enabled
            if (!enabled) {
                return ProviderResponse.unavailable(ProviderType.PROVIDER_A);
            }

            // Simulate various outcomes
            double outcome = random.nextDouble();

            // Simulate timeout
            if (outcome < TIMEOUT_RATE) {
                log.warn("Provider A: Simulating timeout for payment: {}", payment.getPaymentId());
                return ProviderResponse.timeout(ProviderType.PROVIDER_A);
            }

            // Simulate transient failure (retryable)
            if (outcome < TIMEOUT_RATE + TRANSIENT_FAILURE_RATE) {
                log.warn("Provider A: Simulating transient failure for payment: {}", payment.getPaymentId());
                return ProviderResponse.retryableFailure(
                        ProviderType.PROVIDER_A,
                        "TF001",
                        "Temporary processing error. Please retry.",
                        2000L
                );
            }

            // Simulate permanent failure
            if (outcome < TIMEOUT_RATE + TRANSIENT_FAILURE_RATE + FAILURE_RATE) {
                log.warn("Provider A: Simulating permanent failure for payment: {}", payment.getPaymentId());
                return simulateFailure(payment);
            }

            // Success case
            String reference = generateProviderReference();
            successfulRequests.incrementAndGet();

            long processingTime = System.currentTimeMillis() - startTime;
            totalProcessingTime.addAndGet(processingTime);

            log.info("Provider A: Payment successful - Reference: {}, Processing time: {}ms",
                    reference, processingTime);

            ProviderResponse response = ProviderResponse.success(
                    ProviderType.PROVIDER_A,
                    reference,
                    "Payment processed successfully by Provider A"
            );
            response.setProcessingTimeMs(processingTime);
            return response;

        } catch (Exception e) {
            log.error("Provider A: Unexpected error processing payment: {}", payment.getPaymentId(), e);
            return ProviderResponse.retryableFailure(
                    ProviderType.PROVIDER_A,
                    "ERR001",
                    "Unexpected error: " + e.getMessage(),
                    3000L
            );
        }
    }

    @Override
    public ProviderResponse checkPaymentStatus(String providerReference) {
        log.info("Provider A: Checking status for reference: {}", providerReference);

        // Simulate status check
        simulateProcessingDelay();

        // In a real implementation, this would query the provider's API
        return ProviderResponse.success(
                ProviderType.PROVIDER_A,
                providerReference,
                "Payment found and confirmed"
        );
    }

    @Override
    public ProviderResponse refundPayment(Payment payment) {
        log.info("Provider A: Processing refund for payment: {}", payment.getPaymentId());

        simulateProcessingDelay();

        // Simulate refund (90% success rate)
        if (random.nextDouble() < 0.10) {
            return ProviderResponse.failure(
                    ProviderType.PROVIDER_A,
                    "REF001",
                    "Refund failed - transaction not eligible"
            );
        }

        String refundReference = "REF_" + generateProviderReference();
        return ProviderResponse.success(
                ProviderType.PROVIDER_A,
                refundReference,
                "Refund processed successfully"
        );
    }

    @Override
    public Set<PaymentMethod> getSupportedPaymentMethods() {
        return SUPPORTED_METHODS;
    }

    @Override
    public boolean isHealthy() {
        return enabled && getSuccessRate() > 0.5;
    }

    @Override
    public long getTimeoutMs() {
        return timeoutMs;
    }

    @Override
    public double getSuccessRate() {
        int total = totalRequests.get();
        if (total == 0) return 1.0;
        return (double) successfulRequests.get() / total;
    }

    /**
     * Get average processing time in milliseconds
     */
    public long getAverageProcessingTime() {
        int total = successfulRequests.get();
        if (total == 0) return 0;
        return totalProcessingTime.get() / total;
    }

    /**
     * Generate a unique provider reference
     */
    private String generateProviderReference() {
        return "PROV_A_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    /**
     * Simulate processing delay
     */
    private void simulateProcessingDelay() {
        try {
            // Random delay between 100-500ms
            Thread.sleep(100 + random.nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Simulate various failure scenarios
     */
    private ProviderResponse simulateFailure(Payment payment) {
        // Simulate different failure types based on payment details
        if (payment.getPaymentMethod() == PaymentMethod.CARD) {
            // Card-specific failures
            int failureType = random.nextInt(3);
            return switch (failureType) {
                case 0 -> ProviderResponse.failure(
                        ProviderType.PROVIDER_A,
                        "CARD_DECLINED",
                        "Card was declined by the issuing bank"
                );
                case 1 -> ProviderResponse.failure(
                        ProviderType.PROVIDER_A,
                        "INSUFFICIENT_FUNDS",
                        "Insufficient funds in the account"
                );
                default -> ProviderResponse.failure(
                        ProviderType.PROVIDER_A,
                        "CARD_EXPIRED",
                        "Card has expired"
                );
            };
        }

        // Generic failure
        return ProviderResponse.failure(
                ProviderType.PROVIDER_A,
                "PROCESSING_ERROR",
                "Payment could not be processed"
        );
    }

    /**
     * Reset statistics (useful for testing)
     */
    public void resetStatistics() {
        totalRequests.set(0);
        successfulRequests.set(0);
        totalProcessingTime.set(0);
    }
}

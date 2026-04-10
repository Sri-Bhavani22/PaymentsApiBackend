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
 * Provider B implementation - Primary provider for UPI and WALLET payments.
 * This is a simulated provider for demonstration purposes.
 * In production, this would integrate with an actual UPI/Wallet payment gateway.
 */
@Slf4j
@Component
public class ProviderB implements PaymentProvider {

    private static final Set<PaymentMethod> SUPPORTED_METHODS = Set.of(
            PaymentMethod.UPI,
            PaymentMethod.WALLET
    );

    @Value("${payment.providers.provider-b.timeout-ms:5000}")
    private long timeoutMs;

    @Value("${payment.providers.provider-b.enabled:true}")
    private boolean enabled;

    // For simulating random failures and success tracking
    private final Random random = new Random();
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);

    // Simulated failure rate (4% failure rate for demo - slightly better than Provider A)
    private static final double FAILURE_RATE = 0.04;
    // Simulated timeout rate (2% timeout rate for demo)
    private static final double TIMEOUT_RATE = 0.02;
    // Simulated transient failure rate (3% - these are retryable)
    private static final double TRANSIENT_FAILURE_RATE = 0.03;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.PROVIDER_B;
    }

    @Override
    public String getProviderName() {
        return "Provider B - UPI & Wallet Gateway";
    }

    @Override
    public ProviderResponse processPayment(Payment payment) {
        long startTime = System.currentTimeMillis();
        totalRequests.incrementAndGet();

        log.info("Provider B processing payment: {} for amount: {} {}",
                payment.getPaymentId(), payment.getAmount(), payment.getCurrency());

        try {
            // Simulate processing time (50-300ms - UPI is typically faster)
            simulateProcessingDelay();

            // Check if provider is enabled
            if (!enabled) {
                return ProviderResponse.unavailable(ProviderType.PROVIDER_B);
            }

            // Simulate various outcomes
            double outcome = random.nextDouble();

            // Simulate timeout
            if (outcome < TIMEOUT_RATE) {
                log.warn("Provider B: Simulating timeout for payment: {}", payment.getPaymentId());
                return ProviderResponse.timeout(ProviderType.PROVIDER_B);
            }

            // Simulate transient failure (retryable)
            if (outcome < TIMEOUT_RATE + TRANSIENT_FAILURE_RATE) {
                log.warn("Provider B: Simulating transient failure for payment: {}", payment.getPaymentId());
                return ProviderResponse.retryableFailure(
                        ProviderType.PROVIDER_B,
                        "TF002",
                        "UPI service temporarily unavailable. Please retry.",
                        1500L
                );
            }

            // Simulate permanent failure
            if (outcome < TIMEOUT_RATE + TRANSIENT_FAILURE_RATE + FAILURE_RATE) {
                log.warn("Provider B: Simulating permanent failure for payment: {}", payment.getPaymentId());
                return simulateFailure(payment);
            }

            // Success case
            String reference = generateProviderReference();
            successfulRequests.incrementAndGet();

            long processingTime = System.currentTimeMillis() - startTime;
            totalProcessingTime.addAndGet(processingTime);

            log.info("Provider B: Payment successful - Reference: {}, Processing time: {}ms",
                    reference, processingTime);

            ProviderResponse response = ProviderResponse.success(
                    ProviderType.PROVIDER_B,
                    reference,
                    "Payment processed successfully by Provider B"
            );
            response.setProcessingTimeMs(processingTime);
            return response;

        } catch (Exception e) {
            log.error("Provider B: Unexpected error processing payment: {}", payment.getPaymentId(), e);
            return ProviderResponse.retryableFailure(
                    ProviderType.PROVIDER_B,
                    "ERR002",
                    "Unexpected error: " + e.getMessage(),
                    3000L
            );
        }
    }

    @Override
    public ProviderResponse checkPaymentStatus(String providerReference) {
        log.info("Provider B: Checking status for reference: {}", providerReference);

        // Simulate status check
        simulateProcessingDelay();

        // In a real implementation, this would query the provider's API
        return ProviderResponse.success(
                ProviderType.PROVIDER_B,
                providerReference,
                "Payment found and confirmed"
        );
    }

    @Override
    public ProviderResponse refundPayment(Payment payment) {
        log.info("Provider B: Processing refund for payment: {}", payment.getPaymentId());

        simulateProcessingDelay();

        // Simulate refund (92% success rate - UPI refunds are generally reliable)
        if (random.nextDouble() < 0.08) {
            return ProviderResponse.failure(
                    ProviderType.PROVIDER_B,
                    "REF002",
                    "Refund failed - VPA not valid or transaction not found"
            );
        }

        String refundReference = "REF_" + generateProviderReference();
        return ProviderResponse.success(
                ProviderType.PROVIDER_B,
                refundReference,
                "Refund processed successfully via UPI"
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
        return "PROV_B_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    /**
     * Simulate processing delay (UPI is typically faster)
     */
    private void simulateProcessingDelay() {
        try {
            // Random delay between 50-300ms
            Thread.sleep(50 + random.nextInt(250));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Simulate various failure scenarios
     */
    private ProviderResponse simulateFailure(Payment payment) {
        // Simulate different failure types based on payment method
        if (payment.getPaymentMethod() == PaymentMethod.UPI) {
            // UPI-specific failures
            int failureType = random.nextInt(4);
            return switch (failureType) {
                case 0 -> ProviderResponse.failure(
                        ProviderType.PROVIDER_B,
                        "UPI_INVALID_VPA",
                        "Invalid UPI ID / VPA"
                );
                case 1 -> ProviderResponse.failure(
                        ProviderType.PROVIDER_B,
                        "UPI_DECLINED",
                        "Transaction declined by user's bank"
                );
                case 2 -> ProviderResponse.failure(
                        ProviderType.PROVIDER_B,
                        "UPI_LIMIT_EXCEEDED",
                        "Daily transaction limit exceeded"
                );
                default -> ProviderResponse.failure(
                        ProviderType.PROVIDER_B,
                        "UPI_TIMEOUT",
                        "User did not respond to UPI request"
                );
            };
        }

        if (payment.getPaymentMethod() == PaymentMethod.WALLET) {
            // Wallet-specific failures
            int failureType = random.nextInt(2);
            return switch (failureType) {
                case 0 -> ProviderResponse.failure(
                        ProviderType.PROVIDER_B,
                        "WALLET_INSUFFICIENT",
                        "Insufficient wallet balance"
                );
                default -> ProviderResponse.failure(
                        ProviderType.PROVIDER_B,
                        "WALLET_LOCKED",
                        "Wallet account is locked"
                );
            };
        }

        // Generic failure
        return ProviderResponse.failure(
                ProviderType.PROVIDER_B,
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

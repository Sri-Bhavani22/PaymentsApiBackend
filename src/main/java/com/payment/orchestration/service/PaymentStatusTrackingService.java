package com.payment.orchestration.service;

import com.payment.orchestration.dto.PaymentStatusResponse;
import com.payment.orchestration.entity.Payment;
import com.payment.orchestration.entity.PaymentStatusHistory;
import com.payment.orchestration.enums.PaymentStatus;
import com.payment.orchestration.enums.ProviderType;
import com.payment.orchestration.repository.PaymentRepository;
import com.payment.orchestration.repository.PaymentStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for tracking and managing payment status changes.
 * Maintains a complete audit trail of all status transitions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentStatusTrackingService {

    private final PaymentRepository paymentRepository;
    private final PaymentStatusHistoryRepository statusHistoryRepository;

    /**
     * Record a status change for a payment
     * 
     * @param payment The payment entity
     * @param newStatus The new status
     * @param message Optional message describing the change
     * @param responseCode Provider response code (if applicable)
     */
    @Transactional
    public void recordStatusChange(Payment payment, PaymentStatus newStatus, 
                                   String message, String responseCode) {
        log.debug("Recording status change for payment {}: {} -> {}",
                payment.getPaymentId(), payment.getStatus(), newStatus);

        PaymentStatusHistory history = PaymentStatusHistory.builder()
                .payment(payment)
                .status(newStatus)
                .message(message)
                .provider(payment.getProvider())
                .responseCode(responseCode)
                .retryAttempt(payment.getRetryCount())
                .build();

        statusHistoryRepository.save(history);
        
        // Update payment status
        payment.setStatus(newStatus);
        
        // Mark completed if terminal status
        if (newStatus.isTerminal()) {
            payment.markCompleted(newStatus);
        }
        
        paymentRepository.save(payment);

        log.info("Recorded status change for payment {}: {}",
                payment.getPaymentId(), newStatus);
    }

    /**
     * Record a retry attempt
     * 
     * @param payment The payment being retried
     * @param attemptNumber The retry attempt number
     * @param reason Reason for retry
     */
    @Transactional
    public void recordRetryAttempt(Payment payment, int attemptNumber, String reason) {
        log.debug("Recording retry attempt {} for payment {}: {}",
                attemptNumber, payment.getPaymentId(), reason);

        PaymentStatusHistory history = PaymentStatusHistory.builder()
                .payment(payment)
                .status(PaymentStatus.RETRY)
                .message("Retry attempt " + attemptNumber + ": " + reason)
                .provider(payment.getProvider())
                .retryAttempt(attemptNumber)
                .build();

        statusHistoryRepository.save(history);

        log.info("Recorded retry attempt {} for payment {}",
                attemptNumber, payment.getPaymentId());
    }

    /**
     * Get full status history for a payment
     * 
     * @param paymentId The payment ID
     * @return PaymentStatusResponse with full history
     */
    public Optional<PaymentStatusResponse> getPaymentStatus(String paymentId) {
        log.debug("Getting status for payment: {}", paymentId);

        return paymentRepository.findByPaymentIdWithHistory(paymentId)
                .map(payment -> {
                    List<PaymentStatusHistory> history = 
                            statusHistoryRepository.findByPaymentPaymentIdOrderByTimestampAsc(paymentId);
                    
                    return PaymentStatusResponse.fromPaymentWithHistory(
                            payment.getPaymentId(),
                            payment.getStatus(),
                            payment.getProvider(),
                            payment.getRetryCount(),
                            history
                    );
                });
    }

    /**
     * Get the latest status for a payment
     * 
     * @param paymentId The payment ID
     * @return Latest payment status
     */
    public Optional<PaymentStatus> getLatestStatus(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .map(Payment::getStatus);
    }

    /**
     * Count retry attempts for a payment
     * 
     * @param paymentId The payment ID
     * @return Number of retry attempts
     */
    public long countRetryAttempts(String paymentId) {
        return statusHistoryRepository.countRetryAttemptsForPayment(paymentId);
    }

    /**
     * Record processing started
     * 
     * @param payment The payment
     * @param provider The provider processing the payment
     */
    @Transactional
    public void recordProcessingStarted(Payment payment, ProviderType provider) {
        payment.setProvider(provider);
        recordStatusChange(payment, PaymentStatus.PROCESSING,
                "Processing started with " + provider.getDisplayName(), null);
    }

    /**
     * Record successful payment
     * 
     * @param payment The payment
     * @param providerReference Provider reference ID
     * @param responseCode Provider response code
     */
    @Transactional
    public void recordSuccess(Payment payment, String providerReference, String responseCode) {
        payment.setProviderReference(providerReference);
        payment.setProviderResponseCode(responseCode);
        recordStatusChange(payment, PaymentStatus.SUCCESS,
                "Payment completed successfully", responseCode);
    }

    /**
     * Record failed payment
     * 
     * @param payment The payment
     * @param errorCode Error code
     * @param errorMessage Error message
     */
    @Transactional
    public void recordFailure(Payment payment, String errorCode, String errorMessage) {
        payment.setErrorCode(errorCode);
        payment.setErrorMessage(errorMessage);
        recordStatusChange(payment, PaymentStatus.FAILED,
                "Payment failed: " + errorMessage, errorCode);
    }

    /**
     * Get status statistics
     */
    public StatusStatistics getStatistics() {
        long pending = paymentRepository.countByStatus(PaymentStatus.PENDING);
        long processing = paymentRepository.countByStatus(PaymentStatus.PROCESSING);
        long success = paymentRepository.countByStatus(PaymentStatus.SUCCESS);
        long failed = paymentRepository.countByStatus(PaymentStatus.FAILED);
        long retry = paymentRepository.countByStatus(PaymentStatus.RETRY);

        return new StatusStatistics(pending, processing, success, failed, retry);
    }

    /**
     * Record for status statistics
     */
    public record StatusStatistics(
            long pending, 
            long processing, 
            long success, 
            long failed, 
            long retry
    ) {
        public long total() {
            return pending + processing + success + failed + retry;
        }

        public double successRate() {
            long terminal = success + failed;
            if (terminal == 0) return 0.0;
            return (double) success / terminal * 100;
        }
    }
}

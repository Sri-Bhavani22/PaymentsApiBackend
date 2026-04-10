package com.payment.orchestration.repository;

import com.payment.orchestration.entity.Payment;
import com.payment.orchestration.enums.PaymentMethod;
import com.payment.orchestration.enums.PaymentStatus;
import com.payment.orchestration.enums.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Payment entity operations.
 * Provides CRUD operations and custom queries for payment management.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by unique payment ID
     */
    Optional<Payment> findByPaymentId(String paymentId);

    /**
     * Find payment by idempotency key
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find all payments by status
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Find all payments by payment method
     */
    List<Payment> findByPaymentMethod(PaymentMethod paymentMethod);

    /**
     * Find all payments by provider
     */
    List<Payment> findByProvider(ProviderType provider);

    /**
     * Find all payments by customer email
     */
    List<Payment> findByCustomerEmail(String customerEmail);

    /**
     * Find payments created within a date range
     */
    List<Payment> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find payments that need retry (failed but can still retry)
     */
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.retryCount < p.maxRetries")
    List<Payment> findPaymentsEligibleForRetry(@Param("status") PaymentStatus status);

    /**
     * Find all pending payments older than specified time (for cleanup/monitoring)
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :cutoffTime")
    List<Payment> findStalePendingPayments(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count payments by status
     */
    long countByStatus(PaymentStatus status);

    /**
     * Count payments by provider and status
     */
    long countByProviderAndStatus(ProviderType provider, PaymentStatus status);

    /**
     * Check if payment exists by payment ID
     */
    boolean existsByPaymentId(String paymentId);

    /**
     * Check if payment exists by idempotency key
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Find payments by status and payment method
     */
    List<Payment> findByStatusAndPaymentMethod(PaymentStatus status, PaymentMethod paymentMethod);

    /**
     * Get total amount by status
     */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = :status")
    Double getTotalAmountByStatus(@Param("status") PaymentStatus status);

    /**
     * Get payments with status history (for detailed tracking)
     */
    @Query("SELECT DISTINCT p FROM Payment p LEFT JOIN FETCH p.statusHistory WHERE p.paymentId = :paymentId")
    Optional<Payment> findByPaymentIdWithHistory(@Param("paymentId") String paymentId);
}

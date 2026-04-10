package com.payment.orchestration.repository;

import com.payment.orchestration.entity.PaymentStatusHistory;
import com.payment.orchestration.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for PaymentStatusHistory entity operations.
 * Provides queries for payment status tracking and audit trail.
 */
@Repository
public interface PaymentStatusHistoryRepository extends JpaRepository<PaymentStatusHistory, Long> {

    /**
     * Find all status history for a payment
     */
    List<PaymentStatusHistory> findByPaymentIdOrderByTimestampAsc(Long paymentId);

    /**
     * Find status history by payment's paymentId (external ID)
     */
    @Query("SELECT h FROM PaymentStatusHistory h WHERE h.payment.paymentId = :paymentId ORDER BY h.timestamp ASC")
    List<PaymentStatusHistory> findByPaymentPaymentIdOrderByTimestampAsc(@Param("paymentId") String paymentId);

    /**
     * Find the latest status for a payment
     */
    @Query("SELECT h FROM PaymentStatusHistory h WHERE h.payment.paymentId = :paymentId ORDER BY h.timestamp DESC LIMIT 1")
    PaymentStatusHistory findLatestStatusByPaymentId(@Param("paymentId") String paymentId);

    /**
     * Find all status changes of a specific type
     */
    List<PaymentStatusHistory> findByStatus(PaymentStatus status);

    /**
     * Find status changes within a time range
     */
    List<PaymentStatusHistory> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Count status changes by type
     */
    long countByStatus(PaymentStatus status);

    /**
     * Find retry attempts for a payment
     */
    @Query("SELECT h FROM PaymentStatusHistory h WHERE h.payment.paymentId = :paymentId AND h.status = 'RETRY' ORDER BY h.timestamp ASC")
    List<PaymentStatusHistory> findRetryAttemptsForPayment(@Param("paymentId") String paymentId);

    /**
     * Count retry attempts for a payment
     */
    @Query("SELECT COUNT(h) FROM PaymentStatusHistory h WHERE h.payment.paymentId = :paymentId AND h.status = 'RETRY'")
    long countRetryAttemptsForPayment(@Param("paymentId") String paymentId);
}

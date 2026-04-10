package com.payment.orchestration.repository;

import com.payment.orchestration.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for IdempotencyRecord entity operations.
 * Manages idempotency keys and their associated responses.
 */
@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    /**
     * Find idempotency record by key
     */
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    /**
     * Check if idempotency key exists
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Find idempotency record by payment ID
     */
    Optional<IdempotencyRecord> findByPaymentId(String paymentId);

    /**
     * Find all expired records
     */
    List<IdempotencyRecord> findByExpiresAtBefore(LocalDateTime dateTime);

    /**
     * Find records by status
     */
    List<IdempotencyRecord> findByStatus(IdempotencyRecord.IdempotencyStatus status);

    /**
     * Delete expired records
     */
    @Modifying
    @Query("DELETE FROM IdempotencyRecord i WHERE i.expiresAt < :cutoffTime")
    int deleteExpiredRecords(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find processing records older than threshold (for stuck request detection)
     */
    @Query("SELECT i FROM IdempotencyRecord i WHERE i.status = 'PROCESSING' AND i.createdAt < :cutoffTime")
    List<IdempotencyRecord> findStuckProcessingRecords(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Update record status by idempotency key
     */
    @Modifying
    @Query("UPDATE IdempotencyRecord i SET i.status = :status, i.responseBody = :responseBody, " +
           "i.responseStatus = :responseStatus, i.paymentId = :paymentId WHERE i.idempotencyKey = :key")
    int updateRecordStatus(@Param("key") String idempotencyKey,
                          @Param("status") IdempotencyRecord.IdempotencyStatus status,
                          @Param("responseBody") String responseBody,
                          @Param("responseStatus") Integer responseStatus,
                          @Param("paymentId") String paymentId);

    /**
     * Count records by status
     */
    long countByStatus(IdempotencyRecord.IdempotencyStatus status);
}

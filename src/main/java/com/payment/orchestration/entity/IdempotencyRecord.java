package com.payment.orchestration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity to store idempotency records for preventing duplicate payment processing.
 * Each idempotency key is unique and stores the response for duplicate requests.
 */
@Entity
@Table(name = "idempotency_records", indexes = {
    @Index(name = "idx_idempotency_key_unique", columnList = "idempotencyKey", unique = true),
    @Index(name = "idx_idempotency_expires", columnList = "expiresAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique idempotency key provided by the client
     */
    @Column(nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    /**
     * Associated payment ID
     */
    @Column(length = 50)
    private String paymentId;

    /**
     * HTTP status code of the original response
     */
    private Integer responseStatus;

    /**
     * JSON response body to return for duplicate requests
     */
    @Column(columnDefinition = "TEXT")
    private String responseBody;

    /**
     * Request hash to verify the request is identical
     */
    @Column(length = 64)
    private String requestHash;

    /**
     * Status of the idempotency record
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private IdempotencyStatus status = IdempotencyStatus.PROCESSING;

    /**
     * Timestamp when this record was created
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this record expires
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Pre-persist callback to set expiry time
     */
    @PrePersist
    public void prePersist() {
        if (this.expiresAt == null) {
            this.expiresAt = LocalDateTime.now().plusHours(24);
        }
    }

    /**
     * Check if this record has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * Check if this record is still processing
     */
    public boolean isProcessing() {
        return this.status == IdempotencyStatus.PROCESSING;
    }

    /**
     * Check if this record is completed
     */
    public boolean isCompleted() {
        return this.status == IdempotencyStatus.COMPLETED;
    }

    /**
     * Mark record as completed with response
     */
    public void complete(String paymentId, int statusCode, String responseBody) {
        this.paymentId = paymentId;
        this.responseStatus = statusCode;
        this.responseBody = responseBody;
        this.status = IdempotencyStatus.COMPLETED;
    }

    /**
     * Idempotency record status
     */
    public enum IdempotencyStatus {
        /**
         * Request is currently being processed
         */
        PROCESSING,
        
        /**
         * Request has been completed
         */
        COMPLETED,
        
        /**
         * Request failed
         */
        FAILED
    }
}

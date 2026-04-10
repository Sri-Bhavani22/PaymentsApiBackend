package com.payment.orchestration.entity;

import com.payment.orchestration.enums.PaymentStatus;
import com.payment.orchestration.enums.ProviderType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity to track payment status changes over time.
 * Provides an audit trail of all status transitions.
 */
@Entity
@Table(name = "payment_status_history", indexes = {
    @Index(name = "idx_payment_history", columnList = "payment_id"),
    @Index(name = "idx_history_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the parent payment
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    /**
     * Status at this point in time
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    /**
     * Provider handling the payment at this status
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProviderType provider;

    /**
     * Optional message describing the status change
     */
    @Column(length = 500)
    private String message;

    /**
     * Response code from provider (if applicable)
     */
    @Column(length = 50)
    private String responseCode;

    /**
     * Retry attempt number (if this is a retry)
     */
    private Integer retryAttempt;

    /**
     * Timestamp of this status change
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
}

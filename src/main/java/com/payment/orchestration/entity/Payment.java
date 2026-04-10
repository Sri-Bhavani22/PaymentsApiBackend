package com.payment.orchestration.entity;

import com.payment.orchestration.enums.PaymentMethod;
import com.payment.orchestration.enums.PaymentStatus;
import com.payment.orchestration.enums.ProviderType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a payment transaction in the system.
 * Contains all payment details, status, and tracking information.
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_id", columnList = "paymentId"),
    @Index(name = "idx_idempotency_key", columnList = "idempotencyKey"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique payment identifier exposed to external systems
     */
    @Column(nullable = false, unique = true, length = 50)
    private String paymentId;

    /**
     * Idempotency key provided by the client
     */
    @Column(nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    /**
     * Payment amount
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /**
     * Currency code (e.g., INR, USD)
     */
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * Payment method used (CARD, UPI, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    /**
     * Current payment status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    /**
     * Provider handling this payment
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProviderType provider;

    /**
     * Reference ID from the provider
     */
    @Column(length = 100)
    private String providerReference;

    /**
     * Provider's response code
     */
    @Column(length = 50)
    private String providerResponseCode;

    /**
     * Provider's response message
     */
    @Column(length = 500)
    private String providerResponseMessage;

    /**
     * Number of retry attempts made
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Maximum retry attempts allowed
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * Masked card number (last 4 digits)
     */
    @Column(length = 20)
    private String maskedCardNumber;

    /**
     * UPI ID for UPI payments
     */
    @Column(length = 100)
    private String upiId;

    /**
     * Customer email
     */
    @Column(length = 255)
    private String customerEmail;

    /**
     * Customer phone
     */
    @Column(length = 20)
    private String customerPhone;

    /**
     * Payment description
     */
    @Column(length = 500)
    private String description;

    /**
     * Additional metadata as JSON
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /**
     * Error code if payment failed
     */
    @Column(length = 50)
    private String errorCode;

    /**
     * Error message if payment failed
     */
    @Column(length = 500)
    private String errorMessage;

    /**
     * Status history for tracking
     */
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PaymentStatusHistory> statusHistory = new ArrayList<>();

    /**
     * Timestamp when payment was created
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when payment was last updated
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Timestamp when payment was completed (success or failure)
     */
    private LocalDateTime completedAt;

    /**
     * Pre-persist callback to generate payment ID
     */
    @PrePersist
    public void prePersist() {
        if (this.paymentId == null) {
            this.paymentId = "PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        }
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
    }

    /**
     * Add a status history entry
     */
    public void addStatusHistory(PaymentStatus status, String message) {
        PaymentStatusHistory history = PaymentStatusHistory.builder()
                .payment(this)
                .status(status)
                .message(message)
                .provider(this.provider)
                .build();
        this.statusHistory.add(history);
    }

    /**
     * Increment retry count
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * Check if more retries are allowed
     */
    public boolean canRetry() {
        return this.retryCount < this.maxRetries && this.status.isRetryable();
    }

    /**
     * Mark payment as completed
     */
    public void markCompleted(PaymentStatus finalStatus) {
        this.status = finalStatus;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mask card number for storage
     */
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}

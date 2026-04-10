package com.payment.orchestration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.payment.orchestration.entity.PaymentStatusHistory;
import com.payment.orchestration.enums.PaymentStatus;
import com.payment.orchestration.enums.ProviderType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for payment status tracking.
 * Contains current status and complete status history.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentStatusResponse {

    /**
     * Payment identifier
     */
    private String paymentId;

    /**
     * Current payment status
     */
    private PaymentStatus currentStatus;

    /**
     * Status description
     */
    private String statusDescription;

    /**
     * Provider handling the payment
     */
    private ProviderType provider;

    /**
     * Number of retry attempts
     */
    private Integer retryCount;

    /**
     * Is payment in terminal state
     */
    private Boolean isTerminal;

    /**
     * Complete status history
     */
    private List<StatusHistoryEntry> statusHistory;

    /**
     * Last updated timestamp
     */
    private LocalDateTime lastUpdatedAt;

    /**
     * Inner class for status history entries
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusHistoryEntry {
        private PaymentStatus status;
        private String message;
        private ProviderType provider;
        private String responseCode;
        private Integer retryAttempt;
        private LocalDateTime timestamp;

        public static StatusHistoryEntry fromEntity(PaymentStatusHistory history) {
            return StatusHistoryEntry.builder()
                    .status(history.getStatus())
                    .message(history.getMessage())
                    .provider(history.getProvider())
                    .responseCode(history.getResponseCode())
                    .retryAttempt(history.getRetryAttempt())
                    .timestamp(history.getTimestamp())
                    .build();
        }
    }

    /**
     * Build response from payment entity with status history
     */
    public static PaymentStatusResponse fromPaymentWithHistory(
            String paymentId,
            PaymentStatus currentStatus,
            ProviderType provider,
            Integer retryCount,
            List<PaymentStatusHistory> history) {
        
        List<StatusHistoryEntry> historyEntries = history.stream()
                .map(StatusHistoryEntry::fromEntity)
                .collect(Collectors.toList());

        LocalDateTime lastUpdated = historyEntries.isEmpty() ? null :
                historyEntries.get(historyEntries.size() - 1).getTimestamp();

        return PaymentStatusResponse.builder()
                .paymentId(paymentId)
                .currentStatus(currentStatus)
                .statusDescription(currentStatus.getDescription())
                .provider(provider)
                .retryCount(retryCount)
                .isTerminal(currentStatus.isTerminal())
                .statusHistory(historyEntries)
                .lastUpdatedAt(lastUpdated)
                .build();
    }
}

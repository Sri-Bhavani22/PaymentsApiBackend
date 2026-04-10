package com.payment.orchestration.service;

import com.payment.orchestration.dto.PaymentStatusResponse;
import com.payment.orchestration.entity.Payment;
import com.payment.orchestration.entity.PaymentStatusHistory;
import com.payment.orchestration.enums.PaymentMethod;
import com.payment.orchestration.enums.PaymentStatus;
import com.payment.orchestration.enums.ProviderType;
import com.payment.orchestration.repository.PaymentRepository;
import com.payment.orchestration.repository.PaymentStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentStatusTrackingService.
 */
@ExtendWith(MockitoExtension.class)
class PaymentStatusTrackingServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentStatusHistoryRepository statusHistoryRepository;

    @InjectMocks
    private PaymentStatusTrackingService statusTrackingService;

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .id(1L)
                .paymentId("PAY_123456")
                .idempotencyKey("idem-key-123")
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .paymentMethod(PaymentMethod.CARD)
                .status(PaymentStatus.PENDING)
                .provider(ProviderType.PROVIDER_A)
                .retryCount(0)
                .maxRetries(3)
                .build();
    }

    @Nested
    @DisplayName("Record Status Change Tests")
    class RecordStatusChangeTests {

        @Test
        @DisplayName("Should record status change and update payment")
        void shouldRecordStatusChange() {
            when(statusHistoryRepository.save(any(PaymentStatusHistory.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(paymentRepository.save(any(Payment.class)))
                    .thenReturn(payment);

            statusTrackingService.recordStatusChange(
                    payment, PaymentStatus.PROCESSING, "Processing started", null);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
            verify(statusHistoryRepository).save(any(PaymentStatusHistory.class));
            verify(paymentRepository).save(payment);
        }

        @Test
        @DisplayName("Should mark payment completed for terminal status")
        void shouldMarkPaymentCompletedForTerminalStatus() {
            when(statusHistoryRepository.save(any(PaymentStatusHistory.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(paymentRepository.save(any(Payment.class)))
                    .thenReturn(payment);

            statusTrackingService.recordStatusChange(
                    payment, PaymentStatus.SUCCESS, "Payment successful", "00");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.getCompletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Record Retry Attempt Tests")
    class RecordRetryAttemptTests {

        @Test
        @DisplayName("Should record retry attempt")
        void shouldRecordRetryAttempt() {
            when(statusHistoryRepository.save(any(PaymentStatusHistory.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            statusTrackingService.recordRetryAttempt(payment, 1, "Connection timeout");

            verify(statusHistoryRepository).save(argThat(history ->
                    history.getStatus() == PaymentStatus.RETRY &&
                    history.getRetryAttempt() == 1));
        }
    }

    @Nested
    @DisplayName("Get Payment Status Tests")
    class GetPaymentStatusTests {

        @Test
        @DisplayName("Should return payment status with history")
        void shouldReturnPaymentStatusWithHistory() {
            List<PaymentStatusHistory> history = Arrays.asList(
                    PaymentStatusHistory.builder()
                            .status(PaymentStatus.PENDING)
                            .timestamp(LocalDateTime.now().minusMinutes(5))
                            .build(),
                    PaymentStatusHistory.builder()
                            .status(PaymentStatus.PROCESSING)
                            .timestamp(LocalDateTime.now().minusMinutes(3))
                            .build(),
                    PaymentStatusHistory.builder()
                            .status(PaymentStatus.SUCCESS)
                            .timestamp(LocalDateTime.now())
                            .build()
            );

            when(paymentRepository.findByPaymentIdWithHistory("PAY_123456"))
                    .thenReturn(Optional.of(payment));
            when(statusHistoryRepository.findByPaymentPaymentIdOrderByTimestampAsc("PAY_123456"))
                    .thenReturn(history);

            Optional<PaymentStatusResponse> result = statusTrackingService.getPaymentStatus("PAY_123456");

            assertThat(result).isPresent();
            assertThat(result.get().getPaymentId()).isEqualTo("PAY_123456");
            assertThat(result.get().getStatusHistory()).hasSize(3);
        }

        @Test
        @DisplayName("Should return empty when payment not found")
        void shouldReturnEmptyWhenPaymentNotFound() {
            when(paymentRepository.findByPaymentIdWithHistory("PAY_INVALID"))
                    .thenReturn(Optional.empty());

            Optional<PaymentStatusResponse> result = statusTrackingService.getPaymentStatus("PAY_INVALID");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Latest Status Tests")
    class GetLatestStatusTests {

        @Test
        @DisplayName("Should return latest status")
        void shouldReturnLatestStatus() {
            payment.setStatus(PaymentStatus.SUCCESS);
            when(paymentRepository.findByPaymentId("PAY_123456"))
                    .thenReturn(Optional.of(payment));

            Optional<PaymentStatus> result = statusTrackingService.getLatestStatus("PAY_123456");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        @DisplayName("Should return empty when payment not found")
        void shouldReturnEmptyWhenNotFound() {
            when(paymentRepository.findByPaymentId("PAY_INVALID"))
                    .thenReturn(Optional.empty());

            Optional<PaymentStatus> result = statusTrackingService.getLatestStatus("PAY_INVALID");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Count Retry Attempts Tests")
    class CountRetryAttemptsTests {

        @Test
        @DisplayName("Should count retry attempts")
        void shouldCountRetryAttempts() {
            when(statusHistoryRepository.countRetryAttemptsForPayment("PAY_123456"))
                    .thenReturn(3L);

            long count = statusTrackingService.countRetryAttempts("PAY_123456");

            assertThat(count).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("Record Processing Started Tests")
    class RecordProcessingStartedTests {

        @Test
        @DisplayName("Should record processing started")
        void shouldRecordProcessingStarted() {
            when(statusHistoryRepository.save(any(PaymentStatusHistory.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(paymentRepository.save(any(Payment.class)))
                    .thenReturn(payment);

            statusTrackingService.recordProcessingStarted(payment, ProviderType.PROVIDER_A);

            assertThat(payment.getProvider()).isEqualTo(ProviderType.PROVIDER_A);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        }
    }

    @Nested
    @DisplayName("Record Success Tests")
    class RecordSuccessTests {

        @Test
        @DisplayName("Should record successful payment")
        void shouldRecordSuccessfulPayment() {
            when(statusHistoryRepository.save(any(PaymentStatusHistory.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(paymentRepository.save(any(Payment.class)))
                    .thenReturn(payment);

            statusTrackingService.recordSuccess(payment, "PROV_REF_123", "00");

            assertThat(payment.getProviderReference()).isEqualTo("PROV_REF_123");
            assertThat(payment.getProviderResponseCode()).isEqualTo("00");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        }
    }

    @Nested
    @DisplayName("Record Failure Tests")
    class RecordFailureTests {

        @Test
        @DisplayName("Should record failed payment")
        void shouldRecordFailedPayment() {
            when(statusHistoryRepository.save(any(PaymentStatusHistory.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(paymentRepository.save(any(Payment.class)))
                    .thenReturn(payment);

            statusTrackingService.recordFailure(payment, "CARD_DECLINED", "Card was declined");

            assertThat(payment.getErrorCode()).isEqualTo("CARD_DECLINED");
            assertThat(payment.getErrorMessage()).isEqualTo("Card was declined");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("Get Statistics Tests")
    class GetStatisticsTests {

        @Test
        @DisplayName("Should return status statistics")
        void shouldReturnStatusStatistics() {
            when(paymentRepository.countByStatus(PaymentStatus.PENDING)).thenReturn(10L);
            when(paymentRepository.countByStatus(PaymentStatus.PROCESSING)).thenReturn(5L);
            when(paymentRepository.countByStatus(PaymentStatus.SUCCESS)).thenReturn(80L);
            when(paymentRepository.countByStatus(PaymentStatus.FAILED)).thenReturn(5L);
            when(paymentRepository.countByStatus(PaymentStatus.RETRY)).thenReturn(0L);

            PaymentStatusTrackingService.StatusStatistics stats = statusTrackingService.getStatistics();

            assertThat(stats.pending()).isEqualTo(10L);
            assertThat(stats.processing()).isEqualTo(5L);
            assertThat(stats.success()).isEqualTo(80L);
            assertThat(stats.failed()).isEqualTo(5L);
            assertThat(stats.total()).isEqualTo(100L);
            assertThat(stats.successRate()).isCloseTo(94.12, within(0.1));
        }

        @Test
        @DisplayName("Should return zero success rate when no terminal payments")
        void shouldReturnZeroSuccessRateWhenNoTerminal() {
            when(paymentRepository.countByStatus(any())).thenReturn(0L);

            PaymentStatusTrackingService.StatusStatistics stats = statusTrackingService.getStatistics();

            assertThat(stats.successRate()).isEqualTo(0.0);
        }
    }
}

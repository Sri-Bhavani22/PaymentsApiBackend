package com.payment.orchestration.service;

import com.payment.orchestration.entity.IdempotencyRecord;
import com.payment.orchestration.repository.IdempotencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IdempotencyService.
 */
@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @InjectMocks
    private IdempotencyService idempotencyService;

    private String idempotencyKey;
    private IdempotencyRecord existingRecord;

    @BeforeEach
    void setUp() {
        idempotencyKey = "test-idempotency-key-123";
        existingRecord = IdempotencyRecord.builder()
                .id(1L)
                .idempotencyKey(idempotencyKey)
                .paymentId("PAY_123456")
                .status(IdempotencyRecord.IdempotencyStatus.COMPLETED)
                .responseBody("{\"paymentId\":\"PAY_123456\"}")
                .responseStatus(201)
                .requestHash("hash123")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
    }

    @Nested
    @DisplayName("Check Idempotency Key Tests")
    class CheckIdempotencyKeyTests {

        @Test
        @DisplayName("Should return existing record when key exists")
        void shouldReturnExistingRecordWhenKeyExists() {
            when(idempotencyRepository.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.of(existingRecord));

            Optional<IdempotencyRecord> result = idempotencyService.checkIdempotencyKey(idempotencyKey);

            assertThat(result).isPresent();
            assertThat(result.get().getPaymentId()).isEqualTo("PAY_123456");
            verify(idempotencyRepository).findByIdempotencyKey(idempotencyKey);
        }

        @Test
        @DisplayName("Should return empty when key does not exist")
        void shouldReturnEmptyWhenKeyDoesNotExist() {
            when(idempotencyRepository.findByIdempotencyKey(anyString()))
                    .thenReturn(Optional.empty());

            Optional<IdempotencyRecord> result = idempotencyService.checkIdempotencyKey("non-existent-key");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when record is expired")
        void shouldReturnEmptyWhenRecordExpired() {
            IdempotencyRecord expiredRecord = IdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey)
                    .expiresAt(LocalDateTime.now().minusHours(1)) // Expired
                    .status(IdempotencyRecord.IdempotencyStatus.COMPLETED)
                    .build();

            when(idempotencyRepository.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.of(expiredRecord));

            Optional<IdempotencyRecord> result = idempotencyService.checkIdempotencyKey(idempotencyKey);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Create Idempotency Record Tests")
    class CreateIdempotencyRecordTests {

        @Test
        @DisplayName("Should create new idempotency record")
        void shouldCreateNewIdempotencyRecord() {
            IdempotencyRecord savedRecord = IdempotencyRecord.builder()
                    .id(1L)
                    .idempotencyKey(idempotencyKey)
                    .requestHash("hash123")
                    .status(IdempotencyRecord.IdempotencyStatus.PROCESSING)
                    .build();

            when(idempotencyRepository.save(any(IdempotencyRecord.class)))
                    .thenReturn(savedRecord);

            IdempotencyRecord result = idempotencyService.createIdempotencyRecord(idempotencyKey, "hash123");

            assertThat(result).isNotNull();
            assertThat(result.getIdempotencyKey()).isEqualTo(idempotencyKey);
            verify(idempotencyRepository).save(any(IdempotencyRecord.class));
        }
    }

    @Nested
    @DisplayName("Complete Idempotency Record Tests")
    class CompleteIdempotencyRecordTests {

        @Test
        @DisplayName("Should complete idempotency record with response")
        void shouldCompleteIdempotencyRecord() {
            when(idempotencyRepository.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.of(existingRecord));
            when(idempotencyRepository.save(any(IdempotencyRecord.class)))
                    .thenReturn(existingRecord);

            idempotencyService.completeIdempotencyRecord(
                    idempotencyKey, "PAY_123456", 201, "{\"status\":\"SUCCESS\"}");

            verify(idempotencyRepository).findByIdempotencyKey(idempotencyKey);
            verify(idempotencyRepository).save(any(IdempotencyRecord.class));
        }

        @Test
        @DisplayName("Should handle non-existent record gracefully")
        void shouldHandleNonExistentRecord() {
            when(idempotencyRepository.findByIdempotencyKey(anyString()))
                    .thenReturn(Optional.empty());

            // Should not throw exception
            idempotencyService.completeIdempotencyRecord(
                    "non-existent", "PAY_123456", 201, "{}");

            verify(idempotencyRepository).findByIdempotencyKey("non-existent");
            verify(idempotencyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Fail Idempotency Record Tests")
    class FailIdempotencyRecordTests {

        @Test
        @DisplayName("Should mark record as failed")
        void shouldMarkRecordAsFailed() {
            when(idempotencyRepository.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.of(existingRecord));
            when(idempotencyRepository.save(any(IdempotencyRecord.class)))
                    .thenReturn(existingRecord);

            idempotencyService.failIdempotencyRecord(idempotencyKey);

            verify(idempotencyRepository).save(argThat(record ->
                    record.getStatus() == IdempotencyRecord.IdempotencyStatus.FAILED));
        }
    }

    @Nested
    @DisplayName("Request Hash Tests")
    class RequestHashTests {

        @Test
        @DisplayName("Should generate consistent hash for same input")
        void shouldGenerateConsistentHash() {
            String input = "{\"amount\":100,\"currency\":\"INR\"}";

            String hash1 = idempotencyService.generateRequestHash(input);
            String hash2 = idempotencyService.generateRequestHash(input);

            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("Should generate different hash for different input")
        void shouldGenerateDifferentHashForDifferentInput() {
            String input1 = "{\"amount\":100}";
            String input2 = "{\"amount\":200}";

            String hash1 = idempotencyService.generateRequestHash(input1);
            String hash2 = idempotencyService.generateRequestHash(input2);

            assertThat(hash1).isNotEqualTo(hash2);
        }
    }

    @Nested
    @DisplayName("Verify Request Match Tests")
    class VerifyRequestMatchTests {

        @Test
        @DisplayName("Should return true when hashes match")
        void shouldReturnTrueWhenHashesMatch() {
            existingRecord.setRequestHash("hash123");

            boolean result = idempotencyService.verifyRequestMatch(existingRecord, "hash123");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when hashes do not match")
        void shouldReturnFalseWhenHashesDoNotMatch() {
            existingRecord.setRequestHash("hash123");

            boolean result = idempotencyService.verifyRequestMatch(existingRecord, "differentHash");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when record has no hash")
        void shouldReturnTrueWhenRecordHasNoHash() {
            existingRecord.setRequestHash(null);

            boolean result = idempotencyService.verifyRequestMatch(existingRecord, "anyHash");

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Has Conflict Tests")
    class HasConflictTests {

        @Test
        @DisplayName("Should return false when no existing record")
        void shouldReturnFalseWhenNoExistingRecord() {
            when(idempotencyRepository.findByIdempotencyKey(anyString()))
                    .thenReturn(Optional.empty());

            boolean result = idempotencyService.hasConflict(idempotencyKey, "hash123");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when request hash differs")
        void shouldReturnTrueWhenRequestHashDiffers() {
            existingRecord.setRequestHash("originalHash");
            when(idempotencyRepository.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.of(existingRecord));

            boolean result = idempotencyService.hasConflict(idempotencyKey, "differentHash");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when request hash matches")
        void shouldReturnFalseWhenRequestHashMatches() {
            existingRecord.setRequestHash("sameHash");
            when(idempotencyRepository.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.of(existingRecord));

            boolean result = idempotencyService.hasConflict(idempotencyKey, "sameHash");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Cleanup Expired Records Tests")
    class CleanupExpiredRecordsTests {

        @Test
        @DisplayName("Should delete expired records")
        void shouldDeleteExpiredRecords() {
            when(idempotencyRepository.deleteExpiredRecords(any(LocalDateTime.class)))
                    .thenReturn(5);

            idempotencyService.cleanupExpiredRecords();

            verify(idempotencyRepository).deleteExpiredRecords(any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Get Stats Tests")
    class GetStatsTests {

        @Test
        @DisplayName("Should return idempotency statistics")
        void shouldReturnIdempotencyStatistics() {
            when(idempotencyRepository.countByStatus(IdempotencyRecord.IdempotencyStatus.PROCESSING))
                    .thenReturn(5L);
            when(idempotencyRepository.countByStatus(IdempotencyRecord.IdempotencyStatus.COMPLETED))
                    .thenReturn(100L);
            when(idempotencyRepository.countByStatus(IdempotencyRecord.IdempotencyStatus.FAILED))
                    .thenReturn(3L);

            IdempotencyService.IdempotencyStats stats = idempotencyService.getStats();

            assertThat(stats.processing()).isEqualTo(5L);
            assertThat(stats.completed()).isEqualTo(100L);
            assertThat(stats.failed()).isEqualTo(3L);
            assertThat(stats.total()).isEqualTo(108L);
        }
    }
}

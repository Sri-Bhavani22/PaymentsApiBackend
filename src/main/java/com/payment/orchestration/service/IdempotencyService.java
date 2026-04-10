package com.payment.orchestration.service;

import com.payment.orchestration.entity.IdempotencyRecord;
import com.payment.orchestration.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Service for managing idempotency of payment requests.
 * Ensures that duplicate requests with the same idempotency key return the same response.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;

    // TTL for idempotency records (24 hours)
    private static final int TTL_HOURS = 24;

    /**
     * Check if an idempotency key exists and return the existing record
     * 
     * @param idempotencyKey The idempotency key to check
     * @return Optional containing the existing record if found
     */
    @Cacheable(value = "idempotency", key = "#idempotencyKey", unless = "#result == null")
    public Optional<IdempotencyRecord> checkIdempotencyKey(String idempotencyKey) {
        log.debug("Checking idempotency key: {}", idempotencyKey);
        
        Optional<IdempotencyRecord> record = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
        
        if (record.isPresent()) {
            IdempotencyRecord existingRecord = record.get();
            
            // Check if record has expired
            if (existingRecord.isExpired()) {
                log.info("Idempotency record expired for key: {}", idempotencyKey);
                return Optional.empty();
            }
            
            log.info("Found existing idempotency record for key: {}, status: {}", 
                    idempotencyKey, existingRecord.getStatus());
        }
        
        return record;
    }

    /**
     * Create a new idempotency record for a request
     * 
     * @param idempotencyKey The idempotency key
     * @param requestHash Hash of the request for verification
     * @return The created idempotency record
     */
    @Transactional
    public IdempotencyRecord createIdempotencyRecord(String idempotencyKey, String requestHash) {
        log.debug("Creating idempotency record for key: {}", idempotencyKey);

        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .status(IdempotencyRecord.IdempotencyStatus.PROCESSING)
                .expiresAt(LocalDateTime.now().plusHours(TTL_HOURS))
                .build();

        IdempotencyRecord savedRecord = idempotencyRepository.save(record);
        log.info("Created idempotency record: {}", savedRecord.getId());
        
        return savedRecord;
    }

    /**
     * Update idempotency record with response
     * 
     * @param idempotencyKey The idempotency key
     * @param paymentId The payment ID
     * @param responseStatus HTTP status code
     * @param responseBody JSON response body
     */
    @Transactional
    @CacheEvict(value = "idempotency", key = "#idempotencyKey")
    public void completeIdempotencyRecord(String idempotencyKey, String paymentId, 
                                          int responseStatus, String responseBody) {
        log.debug("Completing idempotency record for key: {}", idempotencyKey);

        idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(record -> {
                    record.complete(paymentId, responseStatus, responseBody);
                    idempotencyRepository.save(record);
                    log.info("Completed idempotency record for key: {}", idempotencyKey);
                });
    }

    /**
     * Mark idempotency record as failed
     * 
     * @param idempotencyKey The idempotency key
     */
    @Transactional
    @CacheEvict(value = "idempotency", key = "#idempotencyKey")
    public void failIdempotencyRecord(String idempotencyKey) {
        log.debug("Failing idempotency record for key: {}", idempotencyKey);

        idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(record -> {
                    record.setStatus(IdempotencyRecord.IdempotencyStatus.FAILED);
                    idempotencyRepository.save(record);
                    log.info("Failed idempotency record for key: {}", idempotencyKey);
                });
    }

    /**
     * Generate a hash of the request for verification
     * 
     * @param requestBody JSON request body
     * @return SHA-256 hash of the request
     */
    public String generateRequestHash(String requestBody) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(requestBody.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate request hash", e);
            return requestBody.hashCode() + "";
        }
    }

    /**
     * Verify that the current request matches the original request
     * 
     * @param record The existing idempotency record
     * @param currentRequestHash Hash of the current request
     * @return true if requests match
     */
    public boolean verifyRequestMatch(IdempotencyRecord record, String currentRequestHash) {
        if (record.getRequestHash() == null) {
            return true; // No hash to compare
        }
        return record.getRequestHash().equals(currentRequestHash);
    }

    /**
     * Check if there's a conflict (different request with same key)
     * 
     * @param idempotencyKey The idempotency key
     * @param currentRequestHash Hash of the current request
     * @return true if there's a conflict
     */
    public boolean hasConflict(String idempotencyKey, String currentRequestHash) {
        Optional<IdempotencyRecord> existing = checkIdempotencyKey(idempotencyKey);
        
        if (existing.isEmpty()) {
            return false;
        }
        
        return !verifyRequestMatch(existing.get(), currentRequestHash);
    }

    /**
     * Scheduled task to clean up expired idempotency records
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void cleanupExpiredRecords() {
        log.info("Starting cleanup of expired idempotency records");
        
        LocalDateTime cutoff = LocalDateTime.now();
        int deleted = idempotencyRepository.deleteExpiredRecords(cutoff);
        
        log.info("Cleaned up {} expired idempotency records", deleted);
    }

    /**
     * Get statistics about idempotency records
     */
    public IdempotencyStats getStats() {
        long processing = idempotencyRepository.countByStatus(IdempotencyRecord.IdempotencyStatus.PROCESSING);
        long completed = idempotencyRepository.countByStatus(IdempotencyRecord.IdempotencyStatus.COMPLETED);
        long failed = idempotencyRepository.countByStatus(IdempotencyRecord.IdempotencyStatus.FAILED);
        
        return new IdempotencyStats(processing, completed, failed);
    }

    /**
     * Record for idempotency statistics
     */
    public record IdempotencyStats(long processing, long completed, long failed) {
        public long total() {
            return processing + completed + failed;
        }
    }
}

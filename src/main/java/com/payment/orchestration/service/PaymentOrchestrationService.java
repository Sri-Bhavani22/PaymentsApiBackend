package com.payment.orchestration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.orchestration.dto.PaymentRequest;
import com.payment.orchestration.dto.PaymentResponse;
import com.payment.orchestration.dto.PaymentStatusResponse;
import com.payment.orchestration.entity.IdempotencyRecord;
import com.payment.orchestration.entity.Payment;
import com.payment.orchestration.enums.PaymentStatus;
import com.payment.orchestration.enums.ProviderType;
import com.payment.orchestration.exception.IdempotencyException;
import com.payment.orchestration.exception.PaymentException;
import com.payment.orchestration.provider.PaymentProvider;
import com.payment.orchestration.provider.ProviderResponse;
import com.payment.orchestration.repository.PaymentRepository;
import com.payment.orchestration.routing.RoutingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Main orchestration service for payment processing.
 * Coordinates between routing, providers, idempotency, and status tracking.
 * 
 * Handles:
 * - Payment creation with idempotency
 * - Routing to appropriate provider
 * - Retry with failover
 * - Status tracking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrchestrationService {

    private final PaymentRepository paymentRepository;
    private final RoutingEngine routingEngine;
    private final IdempotencyService idempotencyService;
    private final PaymentStatusTrackingService statusTrackingService;
    private final ObjectMapper objectMapper;

    @Value("${payment.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${payment.retry.initial-delay-ms:1000}")
    private long initialDelayMs;

    @Value("${payment.retry.multiplier:2.0}")
    private double backoffMultiplier;

    @Value("${payment.retry.max-delay-ms:10000}")
    private long maxDelayMs;

    /**
     * Create and process a new payment
     * 
     * @param request The payment request
     * @param idempotencyKey Unique idempotency key
     * @return Payment response
     */
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request, String idempotencyKey) {
        log.info("Creating payment with idempotency key: {}", idempotencyKey);

        // Generate request hash for idempotency verification
        String requestHash = generateRequestHash(request);

        // Check idempotency - return existing response if found
        Optional<IdempotencyRecord> existingRecord = idempotencyService.checkIdempotencyKey(idempotencyKey);
        if (existingRecord.isPresent()) {
            return handleExistingIdempotencyRecord(existingRecord.get(), requestHash, idempotencyKey);
        }

        // Create new idempotency record
        idempotencyService.createIdempotencyRecord(idempotencyKey, requestHash);

        try {
            // Create payment entity
            Payment payment = createPaymentEntity(request, idempotencyKey);
            payment = paymentRepository.save(payment);

            log.info("Created payment: {}", payment.getPaymentId());

            // Process payment with routing and retry
            PaymentResponse response = processPaymentWithRetry(payment);

            // Store response in idempotency record
            String responseJson = serializeResponse(response);
            int statusCode = response.getStatus() == PaymentStatus.SUCCESS ? 201 : 200;
            idempotencyService.completeIdempotencyRecord(
                    idempotencyKey, payment.getPaymentId(), statusCode, responseJson);

            return response;

        } catch (Exception e) {
            log.error("Payment processing failed for idempotency key: {}", idempotencyKey, e);
            idempotencyService.failIdempotencyRecord(idempotencyKey);
            throw e;
        }
    }

    /**
     * Get payment by ID
     * 
     * @param paymentId The payment ID
     * @return Payment response
     */
    public PaymentResponse getPayment(String paymentId) {
        log.debug("Fetching payment: {}", paymentId);

        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException("PAY_002", "Payment not found: " + paymentId));

        return PaymentResponse.fromEntity(payment);
    }

    /**
     * Get payment status with history
     * 
     * @param paymentId The payment ID
     * @return Payment status response
     */
    public PaymentStatusResponse getPaymentStatus(String paymentId) {
        log.debug("Fetching payment status: {}", paymentId);

        return statusTrackingService.getPaymentStatus(paymentId)
                .orElseThrow(() -> new PaymentException("PAY_002", "Payment not found: " + paymentId));
    }

    /**
     * Process payment with retry and failover logic
     */
    private PaymentResponse processPaymentWithRetry(Payment payment) {
        List<ProviderType> triedProviders = new ArrayList<>();
        int attemptNumber = 0;
        long currentDelay = initialDelayMs;

        while (attemptNumber <= maxRetryAttempts) {
            attemptNumber++;

            // Get provider (primary or failover)
            PaymentProvider provider;
            if (triedProviders.isEmpty()) {
                provider = routingEngine.getPrimaryProvider(payment.getPaymentMethod());
            } else {
                Optional<PaymentProvider> nextProvider = routingEngine.getNextAvailableProvider(
                        payment.getPaymentMethod(), triedProviders);
                
                if (nextProvider.isEmpty()) {
                    log.warn("No more providers available for payment: {}", payment.getPaymentId());
                    break;
                }
                provider = nextProvider.get();
            }

            log.info("Attempt {} for payment {} using provider {}",
                    attemptNumber, payment.getPaymentId(), provider.getProviderType());

            // Record processing started
            statusTrackingService.recordProcessingStarted(payment, provider.getProviderType());

            // Process with provider
            ProviderResponse providerResponse = provider.processPayment(payment);

            if (providerResponse.isSuccess()) {
                // Success!
                statusTrackingService.recordSuccess(
                        payment,
                        providerResponse.getProviderReference(),
                        providerResponse.getResponseCode()
                );

                log.info("Payment {} succeeded with provider {}",
                        payment.getPaymentId(), provider.getProviderType());

                return PaymentResponse.success(paymentRepository.save(payment), 
                        "Payment processed successfully");
            }

            // Handle failure
            triedProviders.add(provider.getProviderType());
            log.warn("Payment {} failed with provider {}: {} - {}",
                    payment.getPaymentId(),
                    provider.getProviderType(),
                    providerResponse.getResponseCode(),
                    providerResponse.getResponseMessage());

            if (!providerResponse.isRetryable()) {
                // Non-retryable failure
                statusTrackingService.recordFailure(
                        payment,
                        providerResponse.getResponseCode(),
                        providerResponse.getResponseMessage()
                );

                log.error("Payment {} failed permanently: {}",
                        payment.getPaymentId(), providerResponse.getResponseMessage());

                return PaymentResponse.failed(paymentRepository.save(payment));
            }

            // Retryable failure - record and continue
            if (payment.canRetry()) {
                payment.incrementRetryCount();
                statusTrackingService.recordRetryAttempt(
                        payment,
                        payment.getRetryCount(),
                        providerResponse.getResponseMessage()
                );

                // Apply backoff delay
                applyBackoffDelay(currentDelay);
                currentDelay = Math.min((long) (currentDelay * backoffMultiplier), maxDelayMs);
            } else {
                break;
            }
        }

        // All retries exhausted
        statusTrackingService.recordFailure(
                payment,
                "MAX_RETRIES_EXCEEDED",
                "Payment failed after " + maxRetryAttempts + " retry attempts"
        );

        log.error("Payment {} failed after exhausting all retries", payment.getPaymentId());
        return PaymentResponse.failed(paymentRepository.save(payment));
    }

    /**
     * Handle existing idempotency record
     */
    private PaymentResponse handleExistingIdempotencyRecord(
            IdempotencyRecord record, String currentRequestHash, String idempotencyKey) {
        
        // Verify request matches
        if (!idempotencyService.verifyRequestMatch(record, currentRequestHash)) {
            log.warn("Idempotency conflict: different request with same key: {}", idempotencyKey);
            throw new IdempotencyException(
                    "Idempotency key conflict: a different request was already processed with this key"
            );
        }

        // If still processing, return processing status
        if (record.isProcessing()) {
            log.info("Request still processing for idempotency key: {}", idempotencyKey);
            
            // Try to find the associated payment
            if (record.getPaymentId() != null) {
                return paymentRepository.findByPaymentId(record.getPaymentId())
                        .map(PaymentResponse::processing)
                        .orElse(PaymentResponse.builder()
                                .status(PaymentStatus.PROCESSING)
                                .message("Payment is being processed")
                                .build());
            }
            
            return PaymentResponse.builder()
                    .status(PaymentStatus.PROCESSING)
                    .message("Payment is being processed")
                    .build();
        }

        // Return cached response
        if (record.isCompleted() && record.getResponseBody() != null) {
            log.info("Returning cached response for idempotency key: {}", idempotencyKey);
            return deserializeResponse(record.getResponseBody());
        }

        // Fallback: fetch the payment
        if (record.getPaymentId() != null) {
            return getPayment(record.getPaymentId());
        }

        throw new PaymentException("PAY_005", "Unable to retrieve payment for idempotency key");
    }

    /**
     * Create payment entity from request
     */
    private Payment createPaymentEntity(PaymentRequest request, String idempotencyKey) {
        return Payment.builder()
                .idempotencyKey(idempotencyKey)
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .maskedCardNumber(request.getCardNumber() != null ? 
                        Payment.maskCardNumber(request.getCardNumber()) : null)
                .upiId(request.getUpiId())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .description(request.getDescription())
                .metadata(request.getMetadata())
                .retryCount(0)
                .maxRetries(maxRetryAttempts)
                .build();
    }

    /**
     * Generate hash for request
     */
    private String generateRequestHash(PaymentRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            return idempotencyService.generateRequestHash(json);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize request for hashing", e);
            return String.valueOf(request.hashCode());
        }
    }

    /**
     * Serialize response to JSON
     */
    private String serializeResponse(PaymentResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response", e);
            return null;
        }
    }

    /**
     * Deserialize response from JSON
     */
    private PaymentResponse deserializeResponse(String json) {
        try {
            return objectMapper.readValue(json, PaymentResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize response", e);
            throw new PaymentException("PAY_005", "Failed to retrieve cached response");
        }
    }

    /**
     * Apply backoff delay between retries
     */
    private void applyBackoffDelay(long delayMs) {
        try {
            log.debug("Applying backoff delay: {}ms", delayMs);
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Backoff delay interrupted");
        }
    }
}

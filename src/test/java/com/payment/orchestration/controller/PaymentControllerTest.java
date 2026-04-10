package com.payment.orchestration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.orchestration.dto.PaymentRequest;
import com.payment.orchestration.dto.PaymentResponse;
import com.payment.orchestration.dto.PaymentStatusResponse;
import com.payment.orchestration.enums.PaymentMethod;
import com.payment.orchestration.enums.PaymentStatus;
import com.payment.orchestration.enums.ProviderType;
import com.payment.orchestration.exception.IdempotencyException;
import com.payment.orchestration.exception.PaymentException;
import com.payment.orchestration.service.PaymentOrchestrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PaymentController.
 * Tests all API endpoints with various scenarios.
 */
@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentOrchestrationService paymentOrchestrationService;

    private PaymentRequest validCardRequest;
    private PaymentRequest validUpiRequest;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        idempotencyKey = UUID.randomUUID().toString();

        validCardRequest = PaymentRequest.builder()
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .paymentMethod(PaymentMethod.CARD)
                .cardNumber("4111111111111111")
                .expiryDate("12/25")
                .cvv("123")
                .customerEmail("test@example.com")
                .build();

        validUpiRequest = PaymentRequest.builder()
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .paymentMethod(PaymentMethod.UPI)
                .upiId("test@upi")
                .customerEmail("test@example.com")
                .build();
    }

    @Nested
    @DisplayName("Create Payment Tests")
    class CreatePaymentTests {

        @Test
        @DisplayName("Should create payment successfully with CARD")
        void shouldCreatePaymentWithCard() throws Exception {
            PaymentResponse response = PaymentResponse.builder()
                    .paymentId("PAY_123456")
                    .status(PaymentStatus.SUCCESS)
                    .amount(validCardRequest.getAmount())
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .provider(ProviderType.PROVIDER_A)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(paymentOrchestrationService.createPayment(any(), anyString()))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCardRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paymentId").value("PAY_123456"))
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.provider").value("PROVIDER_A"));
        }

        @Test
        @DisplayName("Should create payment successfully with UPI")
        void shouldCreatePaymentWithUpi() throws Exception {
            PaymentResponse response = PaymentResponse.builder()
                    .paymentId("PAY_789012")
                    .status(PaymentStatus.SUCCESS)
                    .amount(validUpiRequest.getAmount())
                    .currency("INR")
                    .paymentMethod(PaymentMethod.UPI)
                    .provider(ProviderType.PROVIDER_B)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(paymentOrchestrationService.createPayment(any(), anyString()))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpiRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paymentId").value("PAY_789012"))
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.provider").value("PROVIDER_B"));
        }

        @Test
        @DisplayName("Should return 400 when Idempotency-Key header is missing")
        void shouldReturn400WhenIdempotencyKeyMissing() throws Exception {
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCardRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when amount is missing")
        void shouldReturn400WhenAmountMissing() throws Exception {
            PaymentRequest invalidRequest = PaymentRequest.builder()
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();

            mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when amount is negative")
        void shouldReturn400WhenAmountNegative() throws Exception {
            PaymentRequest invalidRequest = PaymentRequest.builder()
                    .amount(new BigDecimal("-100.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();

            mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 409 on idempotency conflict")
        void shouldReturn409OnIdempotencyConflict() throws Exception {
            when(paymentOrchestrationService.createPayment(any(), anyString()))
                    .thenThrow(new IdempotencyException("Conflict detected"));

            mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCardRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("PAY_003"));
        }

        @Test
        @DisplayName("Should return 202 when payment is processing")
        void shouldReturn202WhenPaymentProcessing() throws Exception {
            PaymentResponse response = PaymentResponse.builder()
                    .paymentId("PAY_123456")
                    .status(PaymentStatus.PROCESSING)
                    .amount(validCardRequest.getAmount())
                    .currency("INR")
                    .build();

            when(paymentOrchestrationService.createPayment(any(), anyString()))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCardRequest)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.status").value("PROCESSING"));
        }
    }

    @Nested
    @DisplayName("Get Payment Tests")
    class GetPaymentTests {

        @Test
        @DisplayName("Should get payment successfully")
        void shouldGetPaymentSuccessfully() throws Exception {
            PaymentResponse response = PaymentResponse.builder()
                    .paymentId("PAY_123456")
                    .status(PaymentStatus.SUCCESS)
                    .amount(new BigDecimal("1000.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .provider(ProviderType.PROVIDER_A)
                    .build();

            when(paymentOrchestrationService.getPayment("PAY_123456"))
                    .thenReturn(response);

            mockMvc.perform(get("/api/v1/payments/PAY_123456"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentId").value("PAY_123456"))
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @DisplayName("Should return 404 when payment not found")
        void shouldReturn404WhenPaymentNotFound() throws Exception {
            when(paymentOrchestrationService.getPayment("PAY_INVALID"))
                    .thenThrow(new PaymentException("PAY_002", "Payment not found"));

            mockMvc.perform(get("/api/v1/payments/PAY_INVALID"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("PAY_002"));
        }
    }

    @Nested
    @DisplayName("Get Payment Status Tests")
    class GetPaymentStatusTests {

        @Test
        @DisplayName("Should get payment status with history")
        void shouldGetPaymentStatusWithHistory() throws Exception {
            PaymentStatusResponse response = PaymentStatusResponse.builder()
                    .paymentId("PAY_123456")
                    .currentStatus(PaymentStatus.SUCCESS)
                    .provider(ProviderType.PROVIDER_A)
                    .retryCount(0)
                    .isTerminal(true)
                    .statusHistory(List.of(
                            PaymentStatusResponse.StatusHistoryEntry.builder()
                                    .status(PaymentStatus.PENDING)
                                    .timestamp(LocalDateTime.now().minusMinutes(5))
                                    .build(),
                            PaymentStatusResponse.StatusHistoryEntry.builder()
                                    .status(PaymentStatus.PROCESSING)
                                    .timestamp(LocalDateTime.now().minusMinutes(4))
                                    .build(),
                            PaymentStatusResponse.StatusHistoryEntry.builder()
                                    .status(PaymentStatus.SUCCESS)
                                    .timestamp(LocalDateTime.now())
                                    .build()
                    ))
                    .build();

            when(paymentOrchestrationService.getPaymentStatus("PAY_123456"))
                    .thenReturn(response);

            mockMvc.perform(get("/api/v1/payments/PAY_123456/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentId").value("PAY_123456"))
                    .andExpect(jsonPath("$.currentStatus").value("SUCCESS"))
                    .andExpect(jsonPath("$.isTerminal").value(true))
                    .andExpect(jsonPath("$.statusHistory").isArray())
                    .andExpect(jsonPath("$.statusHistory.length()").value(3));
        }
    }

    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return OK for health check")
        void shouldReturnOkForHealthCheck() throws Exception {
            mockMvc.perform(get("/api/v1/payments/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("OK"));
        }
    }
}

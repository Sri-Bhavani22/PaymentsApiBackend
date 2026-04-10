package com.payment.orchestration.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.orchestration.dto.PaymentRequest;
import com.payment.orchestration.dto.PaymentResponse;
import com.payment.orchestration.enums.PaymentMethod;
import com.payment.orchestration.enums.PaymentStatus;
import com.payment.orchestration.repository.PaymentRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Payment Orchestration System.
 * Tests end-to-end payment flows including:
 * - Sanity tests for basic functionality
 * - Regression tests for critical paths
 * - Negative tests for error handling
 * - Idempotency tests for duplicate prevention
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        paymentRepository.deleteAll();
    }

    // ============================================
    // SANITY TESTS - Basic functionality checks
    // ============================================

    @Nested
    @DisplayName("Sanity Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class SanityTests {

        @Test
        @Order(1)
        @DisplayName("SANITY-001: Health check should return OK")
        void healthCheckShouldReturnOk() throws Exception {
            mockMvc.perform(get("/api/v1/payments/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("OK"));
        }

        @Test
        @Order(2)
        @DisplayName("SANITY-002: Create CARD payment should succeed")
        void createCardPaymentShouldSucceed() throws Exception {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("1000.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv("123")
                    .customerEmail("test@example.com")
                    .build();

            MvcResult result = mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paymentId").exists())
                    .andExpect(jsonPath("$.status").exists())
                    .andReturn();

            PaymentResponse response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    PaymentResponse.class
            );

            assertThat(response.getPaymentId()).startsWith("PAY_");
        }

        @Test
        @Order(4)
        @DisplayName("SANITY-004: Get payment by ID should succeed")
        void getPaymentByIdShouldSucceed() throws Exception {
            // First create a payment
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("1000.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();

            MvcResult createResult = mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();

            PaymentResponse createResponse = objectMapper.readValue(
                    createResult.getResponse().getContentAsString(),
                    PaymentResponse.class
            );

            // Then fetch it
            mockMvc.perform(get("/api/v1/payments/" + createResponse.getPaymentId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentId").value(createResponse.getPaymentId()));
        }
    }

    // ============================================
    // REGRESSION TESTS - Critical path testing
    // ============================================

    @Nested
    @DisplayName("Regression Tests")
    class RegressionTests {

        @Test
        @DisplayName("REG-001: Payment status tracking should work")
        void paymentStatusTrackingShouldWork() throws Exception {
            // Create payment
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("1000.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();

            MvcResult createResult = mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            PaymentResponse response = objectMapper.readValue(
                    createResult.getResponse().getContentAsString(),
                    PaymentResponse.class
            );

            // Get status with history
            mockMvc.perform(get("/api/v1/payments/" + response.getPaymentId() + "/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentId").value(response.getPaymentId()))
                    .andExpect(jsonPath("$.currentStatus").exists())
                    .andExpect(jsonPath("$.statusHistory").isArray());
        }

        @Test
        @DisplayName("REG-002: Idempotency should prevent duplicate payments")
        void idempotencyShouldPreventDuplicates() throws Exception {
            String idempotencyKey = UUID.randomUUID().toString();

            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("1000.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();

            // First request
            MvcResult firstResult = mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            PaymentResponse firstResponse = objectMapper.readValue(
                    firstResult.getResponse().getContentAsString(),
                    PaymentResponse.class
            );

            // Second request with same key should return same payment
            MvcResult secondResult = mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn();

            PaymentResponse secondResponse = objectMapper.readValue(
                    secondResult.getResponse().getContentAsString(),
                    PaymentResponse.class
            );

            assertThat(firstResponse.getPaymentId()).isEqualTo(secondResponse.getPaymentId());
        }

        @Test
        @DisplayName("REG-003: Multiple payment methods should route correctly")
        void multiplePaymentMethodsShouldRouteCorrectly() throws Exception {
            // CARD payment
            PaymentRequest cardRequest = PaymentRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();

            MvcResult cardResult = mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cardRequest)))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();

            PaymentResponse cardResponse = objectMapper.readValue(
                    cardResult.getResponse().getContentAsString(),
                    PaymentResponse.class
            );

            // UPI payment
            PaymentRequest upiRequest = PaymentRequest.builder()
                    .amount(new BigDecimal("200.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.UPI)
                    .upiId("test@upi")
                    .build();

            MvcResult upiResult = mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(upiRequest)))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();

            PaymentResponse upiResponse = objectMapper.readValue(
                    upiResult.getResponse().getContentAsString(),
                    PaymentResponse.class
            );

            // Verify different payment IDs
            assertThat(cardResponse.getPaymentId()).isNotEqualTo(upiResponse.getPaymentId());
        }
    }

    // ============================================
    // NEGATIVE TESTS - Error handling
    // ============================================

    @Nested
    @DisplayName("Negative Tests")
    class NegativeTests {

        @Test
        @DisplayName("NEG-001: Missing Idempotency-Key should return 400")
        void missingIdempotencyKeyShouldReturn400() throws Exception {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("1000.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("NEG-002: Missing amount should return 400")
        void missingAmountShouldReturn400() throws Exception {
            PaymentRequest request = PaymentRequest.builder()
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();

            mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("NEG-003: Negative amount should return 400")
        void negativeAmountShouldReturn400() throws Exception {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("-100.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();

            mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("NEG-004: Invalid payment ID should return 404")
        void invalidPaymentIdShouldReturn404() throws Exception {
            mockMvc.perform(get("/api/v1/payments/PAY_INVALID_12345"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("PAY_002"));
        }

        @Test
        @DisplayName("NEG-005: Missing card details for CARD payment should return 400")
        void missingCardDetailsShouldReturn400() throws Exception {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("1000.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    // Missing card details
                    .build();

            mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("NEG-006: Missing UPI ID for UPI payment should return 400")
        void missingUpiIdShouldReturn400() throws Exception {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("500.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.UPI)
                    // Missing UPI ID
                    .build();

            mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("NEG-007: Invalid email format should return 400")
        void invalidEmailFormatShouldReturn400() throws Exception {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("1000.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv("123")
                    .customerEmail("invalid-email")
                    .build();

            mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ============================================
    // CONCURRENT REQUEST TESTS - Idempotency under load
    // ============================================

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("CONC-001: Concurrent requests with same idempotency key should create only one payment")
        void concurrentRequestsShouldCreateOnlyOnePayment() throws Exception {
            String idempotencyKey = UUID.randomUUID().toString();
            int numThreads = 5;

            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("1000.00"))
                    .currency("INR")
                    .paymentMethod(PaymentMethod.CARD)
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch latch = new CountDownLatch(numThreads);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < numThreads; i++) {
                executor.submit(() -> {
                    try {
                        mockMvc.perform(post("/api/v1/payments")
                                        .header("Idempotency-Key", idempotencyKey)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // Some requests may return cached response or conflict
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // Verify only one payment was created
            long paymentCount = paymentRepository.count();
            assertThat(paymentCount).isEqualTo(1);
        }
    }
}

package com.payment.orchestration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Main entry point for the Payment Orchestration System.
 * 
 * This application provides:
 * - Create Payment API with routing to appropriate providers
 * - Fetch Payment API for retrieving payment details
 * - Intelligent routing (CARD → Provider A, UPI → Provider B)
 * - Retry mechanism with exponential backoff and failover
 * - Idempotency support to prevent duplicate transactions
 * - Payment status tracking throughout the lifecycle
 */
@SpringBootApplication
@EnableCaching
@EnableRetry
public class PaymentOrchestrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentOrchestrationApplication.class, args);
    }
}

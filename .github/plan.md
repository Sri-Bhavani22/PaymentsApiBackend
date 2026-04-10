# Payment Orchestration System - Development Plan

## Project Overview
A simplified payment orchestration system inspired by real-world platforms like Yuno. This backend system handles payment processing with intelligent routing, retry mechanisms, and idempotency support.

---

## Architecture Overview

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────────┐
│  Controller Layer   │  ← REST API endpoints
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   Service Layer     │  ← Orchestration Engine (Business Logic)
│ (Orchestration)     │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   Routing Engine    │  ← Payment Method → Provider Mapping
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Provider Connectors │  ← Provider A (CARD), Provider B (UPI)
│      (A / B)        │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Persistence Layer  │  ← H2/PostgreSQL Database
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Idempotency Store  │  ← Redis / In-Memory Cache
└─────────────────────┘
```

---

## Development Phases

### Phase 1: Project Setup (Day 1)
- [x] Initialize Spring Boot project with required dependencies
- [x] Configure application properties (H2 for dev, PostgreSQL for prod)
- [x] Set up project structure following clean architecture
- [x] Configure Redis/In-memory cache for idempotency

### Phase 2: Domain Layer (Day 1-2)
- [x] Create Payment entity with all required fields
- [x] Create PaymentStatus enum (PENDING, PROCESSING, SUCCESS, FAILED, RETRY)
- [x] Create PaymentMethod enum (CARD, UPI, NET_BANKING, WALLET)
- [x] Create IdempotencyRecord entity
- [x] Create Provider entity

### Phase 3: Persistence Layer (Day 2)
- [x] Implement PaymentRepository
- [x] Implement IdempotencyRepository
- [x] Implement ProviderRepository
- [x] Configure JPA/Hibernate settings

### Phase 4: Provider Connectors (Day 2-3)
- [x] Create PaymentProvider interface
- [x] Implement ProviderA (handles CARD payments)
- [x] Implement ProviderB (handles UPI payments)
- [x] Add simulated latency and failure scenarios
- [x] Implement provider response handling

### Phase 5: Routing Engine (Day 3)
- [x] Create RoutingEngine interface
- [x] Implement DefaultRoutingEngine
- [x] Configure routing rules (CARD → A, UPI → B)
- [x] Implement failover routing logic
- [x] Add provider health checking

### Phase 6: Service Layer (Day 3-4)
- [x] Implement PaymentOrchestrationService
- [x] Add idempotency check logic
- [x] Implement retry mechanism with exponential backoff
- [x] Add payment status tracking
- [x] Implement transaction management

### Phase 7: Controller Layer (Day 4)
- [x] Create PaymentController with REST endpoints
- [x] Implement request validation
- [x] Add proper error handling
- [x] Configure idempotency key header processing

### Phase 8: Testing (Day 5)
- [x] Write unit tests for all components
- [x] Write integration tests
- [x] Write sanity tests
- [x] Add negative test cases
- [x] Performance testing

### Phase 9: Documentation (Day 5)
- [x] Complete README with installation/execution guide
- [x] API documentation
- [x] Test case documentation

---

## API Endpoints

### 1. Create Payment
```
POST /api/v1/payments
Headers:
  - Idempotency-Key: <unique-key>
  - Content-Type: application/json

Request Body:
{
  "amount": 1000.00,
  "currency": "INR",
  "paymentMethod": "CARD",
  "cardNumber": "4111111111111111",
  "expiryDate": "12/25",
  "cvv": "123",
  "customerEmail": "customer@example.com",
  "description": "Order #12345"
}

Response (201 Created):
{
  "paymentId": "PAY_123456789",
  "status": "PROCESSING",
  "amount": 1000.00,
  "currency": "INR",
  "providerReference": "PROV_A_REF_001",
  "createdAt": "2026-04-10T10:30:00Z"
}
```

### 2. Fetch Payment
```
GET /api/v1/payments/{paymentId}

Response (200 OK):
{
  "paymentId": "PAY_123456789",
  "status": "SUCCESS",
  "amount": 1000.00,
  "currency": "INR",
  "paymentMethod": "CARD",
  "provider": "PROVIDER_A",
  "providerReference": "PROV_A_REF_001",
  "retryCount": 0,
  "createdAt": "2026-04-10T10:30:00Z",
  "updatedAt": "2026-04-10T10:30:05Z"
}
```

### 3. Get Payment Status
```
GET /api/v1/payments/{paymentId}/status

Response (200 OK):
{
  "paymentId": "PAY_123456789",
  "status": "SUCCESS",
  "statusHistory": [
    {"status": "PENDING", "timestamp": "2026-04-10T10:30:00Z"},
    {"status": "PROCESSING", "timestamp": "2026-04-10T10:30:01Z"},
    {"status": "SUCCESS", "timestamp": "2026-04-10T10:30:05Z"}
  ]
}
```

---

## Routing Rules

| Payment Method | Primary Provider | Failover Provider |
|----------------|------------------|-------------------|
| CARD           | Provider A       | Provider B        |
| UPI            | Provider B       | Provider A        |
| NET_BANKING    | Provider A       | Provider B        |
| WALLET         | Provider B       | Provider A        |

---

## Retry Strategy

- **Max Retries**: 3
- **Backoff Strategy**: Exponential backoff
- **Initial Delay**: 1 second
- **Max Delay**: 10 seconds
- **Multiplier**: 2.0
- **Retryable Errors**: 
  - Connection timeout
  - Gateway timeout (504)
  - Service unavailable (503)
  - Provider temporary failure

---

## Idempotency Implementation

1. Client sends `Idempotency-Key` header with unique value
2. System checks if key exists in idempotency store
3. If exists: Return cached response
4. If not exists: 
   - Store key with PENDING status
   - Process payment
   - Update with final response
   - Set TTL (24 hours)

---

## Error Handling

| Error Code | Description | HTTP Status |
|------------|-------------|-------------|
| PAY_001    | Invalid payment request | 400 |
| PAY_002    | Payment not found | 404 |
| PAY_003    | Duplicate idempotency key | 409 |
| PAY_004    | Provider unavailable | 503 |
| PAY_005    | Payment processing failed | 500 |
| PAY_006    | Invalid payment method | 400 |

---

## Performance Targets

| Metric | Target |
|--------|--------|
| API Response Time (P95) | < 500ms |
| API Response Time (P99) | < 1000ms |
| Throughput | > 100 TPS |
| Availability | 99.9% |
| Error Rate | < 0.1% |

---

## Technology Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.x
- **Database**: H2 (dev), PostgreSQL (prod)
- **Cache**: Redis / Caffeine (in-memory)
- **Build Tool**: Maven
- **Testing**: JUnit 5, Mockito, Spring Test
- **API Docs**: SpringDoc OpenAPI

---

## Project Structure

```
payment-orchestration/
├── .github/
│   ├── plan.md
│   └── skills.md
├── src/
│   ├── main/
│   │   ├── java/com/payment/orchestration/
│   │   │   ├── PaymentOrchestrationApplication.java
│   │   │   ├── config/
│   │   │   │   ├── CacheConfig.java
│   │   │   │   └── RetryConfig.java
│   │   │   ├── controller/
│   │   │   │   └── PaymentController.java
│   │   │   ├── dto/
│   │   │   │   ├── PaymentRequest.java
│   │   │   │   ├── PaymentResponse.java
│   │   │   │   └── PaymentStatusResponse.java
│   │   │   ├── entity/
│   │   │   │   ├── Payment.java
│   │   │   │   ├── IdempotencyRecord.java
│   │   │   │   └── PaymentStatusHistory.java
│   │   │   ├── enums/
│   │   │   │   ├── PaymentStatus.java
│   │   │   │   ├── PaymentMethod.java
│   │   │   │   └── ProviderType.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── PaymentException.java
│   │   │   │   └── IdempotencyException.java
│   │   │   ├── provider/
│   │   │   │   ├── PaymentProvider.java
│   │   │   │   ├── ProviderA.java
│   │   │   │   ├── ProviderB.java
│   │   │   │   └── ProviderResponse.java
│   │   │   ├── repository/
│   │   │   │   ├── PaymentRepository.java
│   │   │   │   ├── IdempotencyRepository.java
│   │   │   │   └── PaymentStatusHistoryRepository.java
│   │   │   ├── routing/
│   │   │   │   ├── RoutingEngine.java
│   │   │   │   └── DefaultRoutingEngine.java
│   │   │   └── service/
│   │   │       ├── PaymentOrchestrationService.java
│   │   │       ├── IdempotencyService.java
│   │   │       └── PaymentStatusTrackingService.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-test.yml
│   └── test/
│       └── java/com/payment/orchestration/
│           ├── controller/
│           │   └── PaymentControllerTest.java
│           ├── service/
│           │   └── PaymentOrchestrationServiceTest.java
│           ├── routing/
│           │   └── RoutingEngineTest.java
│           └── integration/
│               └── PaymentIntegrationTest.java
├── pom.xml
└── README.md
```

---

## Timeline Summary

| Phase | Description | Duration |
|-------|-------------|----------|
| 1 | Project Setup | 0.5 day |
| 2 | Domain Layer | 0.5 day |
| 3 | Persistence Layer | 0.5 day |
| 4 | Provider Connectors | 1 day |
| 5 | Routing Engine | 0.5 day |
| 6 | Service Layer | 1 day |
| 7 | Controller Layer | 0.5 day |
| 8 | Testing | 1 day |
| 9 | Documentation | 0.5 day |
| **Total** | | **6 days** |

---

## Success Criteria

- [ ] All functional requirements implemented
- [ ] All API endpoints working correctly
- [ ] Idempotency working as expected
- [ ] Retry mechanism functioning properly
- [ ] Routing logic correctly directing payments
- [ ] All tests passing (>80% coverage)
- [ ] Documentation complete
- [ ] No critical bugs

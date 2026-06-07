# Payment Gateway

A production-grade payment gateway built with Java and Spring Boot, featuring real-time fraud detection, API security, and complete payment lifecycle management.

## Live Demo
- API Documentation: http://localhost:8080/swagger-ui/index.html
- Health Check: http://localhost:8080/actuator/health

## Features

- **Payment Authorization** — Card payment processing with state machine (INITIATED → FRAUD_CHECK → AUTHORIZED/DECLINED)
- **Fraud Detection Engine** — Multi-rule scoring system with velocity checks, amount thresholds, BIN analysis, and card testing detection
- **Capture / Void / Refund** — Complete payment lifecycle management
- **Idempotency** — Safe retry support, prevents double charges
- **API Key Authentication** — Secure header-based authentication
- **Input Validation** — Comprehensive request validation with detailed error responses
- **Audit Logging** — Every request logged with timing and status

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| ORM | Hibernate / Spring Data JPA |
| API Docs | Swagger UI (SpringDoc OpenAPI) |
| Containerization | Docker + Docker Compose |
| Build | Maven |

## Architecture

HTTP Request
↓
RequestLoggingFilter    — timing, audit trail
↓
ApiKeyAuthFilter        — X-API-Key authentication
↓
PaymentController       — REST endpoints, input validation
↓
PaymentService          — orchestration, state machine
↓
FraudService            — multi-rule scoring engine
↓
TransactionRepository   — PostgreSQL persistence
## Fraud Detection Rules

| Rule | Trigger | Risk Score |
|---|---|---|
| Velocity Block | 10+ transactions/minute | +60 |
| Velocity Warning | 5+ transactions/minute | +40 |
| Very High Amount | Over ₹2,00,000 | +35 |
| High Amount | Over ₹50,000 | +20 |
| Prepaid Card | Known prepaid BIN | +20 |
| New Merchant | First transaction | +15 |
| Low Volume Merchant | Under 5 transactions | +10 |
| Round Number | Multiples of ₹1000 | +10 |

Scoring: 0-29 = ALLOW, 30-59 = REVIEW, 60+ = BLOCK

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker Desktop

### Run locally

```bash
# Start database and cache
docker compose up -d

# Run the application
mvn spring-boot:run

# API is live at http://localhost:8080
```

### API Authentication
All endpoints require the `X-API-Key` header:
### Example Request

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "X-API-Key: test-api-key-12345" \
  -d '{
    "merchantId": "MERCHANT_001",
    "amount": 1500.00,
    "currency": "INR",
    "idempotencyKey": "unique-key-001",
    "cardNumber": "4111111111111111",
    "cardHolderName": "John Doe",
    "expiryMonth": "12",
    "expiryYear": "2027",
    "cvv": "123"
  }'
```

### Example Response

```json
{
  "transactionId": "6a3183eb-30a8-4cac-8796-fc83cb0dcc17",
  "merchantId": "MERCHANT_001",
  "amount": 1500.00,
  "currency": "INR",
  "status": "AUTHORIZED",
  "cardLastFour": "1111",
  "cardBrand": "VISA",
  "message": "Payment authorized successfully",
  "createdAt": "2026-06-07T14:30:00"
}
```

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/payments` | Initiate a payment |
| GET | `/api/v1/payments/{id}` | Get transaction by ID |
| POST | `/api/v1/payments/{id}/capture` | Capture authorized payment |
| POST | `/api/v1/payments/{id}/refund` | Refund captured payment |
| POST | `/api/v1/payments/{id}/void` | Void authorized payment |
| GET | `/api/v1/payments/merchant/{id}` | List merchant transactions |

## Project Structure
src/main/java/com/payment/gateway/
├── controller/          # REST API endpoints
├── service/             # Business logic
│   ├── PaymentService   # Payment orchestration
│   └── FraudService     # Fraud detection engine
├── entity/              # Database entities
├── repository/          # Data access layer
├── dto/                 # Request/Response objects
├── filter/              # Auth and logging filters
├── exception/           # Global error handling
├── enums/               # Transaction states
└── config/              # App configuration
# Digital Payment Processing Platform – Backlog Understanding

This document summarizes the backlog captured in `payment-platform-backlog.xlsx` and translates it into a practical product and engineering understanding of the platform.

## 1. Product intent

This platform is a modern payment processing system built as a set of cooperating Spring Boot microservices. The business goal is to support secure payment creation, fraud checks, bank authorization/capture, ledger accounting, refund processing, and settlement while keeping the system resilient, observable, and easy for clients to integrate with.

The backlog is designed around real-world payment platform concerns, not just CRUD APIs. It intentionally includes:

- strict money semantics
- idempotency and retries
- hard-to-get-right failure states
- event-driven integration
- strong consistency for accounting
- auditability and traceability

## 2. Platform shape

The repository is organized around multiple services and shared infrastructure:

- api-gateway
- payment-service
- bank-simulator
- fraud-service
- ledger-service
- settlement-service
- notification-service
- common-lib
- infra/

This structure shows that the platform is not a single app. It is a distributed domain system where each service owns a specific concern, with shared contracts and standards provided through common-lib.

## 3. Core backlog themes

The backlog is organized into epics, and the sequence makes technical sense:

1. Foundation first
2. Build the payment domain
3. Add resilience around bank interactions
4. Add event-driven architecture
5. Add fraud detection
6. Add ledger/accounting
7. Add settlements and downstream flows

This is a healthy order because payment domains are dangerous to build without consistent contracts, idempotency, and event safety.

## 4. Epic-by-epic understanding

### Epic 0: Foundation

This is the platform infrastructure and standards layer.

#### PAY-001 – Repo and build skeleton
- Establish the mono-repo structure and parent build.
- Create a service-per-domain layout and standard module boundaries.
- This is the platform foundation that all services depend on.

#### PAY-002 – Local infrastructure with one command
- Provide a Docker Compose setup for local development.
- Start database, Kafka, Redis-compatible cache, Keycloak, Mailpit, and observability tools.
- This removes the need for every developer to hand-configure infrastructure.

#### PAY-003 – Standard error and API contract
- This is a crucial foundational story.
- Standardize API errors so every service returns the same format.
- Ensure all routes are versioned under /api/v1.
- Generate OpenAPI specs for each service.
- Use RFC 9457 Problem Details with shared fields such as:
  - code
  - message
  - correlationId
  - timestamp
  - validation errors with field-level detail

This story is the “contract layer” that keeps the platform consistent and makes API adoption predictable.

## 5. Epic 1: Payment Service

This epic covers the core business flow for creating and managing payments.

### PAY-101 – Create a payment
- Merchant submits amount, currency, customer reference, and payment-method token.
- Valid request creates a payment in CREATED state and returns HTTP 201.
- Validates amount > 0 and supported currency.
- Hides raw payment data from responses.
- Uses UUID/ULID identifiers instead of sequential IDs.

### PAY-102 – Idempotency
- Prevent double charging when a client retries the same request.
- Same Idempotency-Key + same body returns the original result.
- Same key + different body is rejected with a conflict.
- Keys expire after a configurable window.
- This is one of the most important payment concerns.

### PAY-103 – Payment state machine
- Define strict lifecycle states such as:
  - CREATED
  - PENDING_FRAUD_CHECK
  - IN_REVIEW
  - AUTHORIZING
  - AUTHORIZATION_UNKNOWN
  - AUTHORIZED
  - DECLINED
  - FAILED
  - CAPTURED
  - EXPIRED
  - SETTLED
  - PARTIALLY_REFUNDED
  - REFUNDED
- Reject invalid transitions.
- Store transition history in an append-only audit table.
- Add optimistic locking for concurrency protection.

### PAY-104 – Authorize and capture
- Authorize money first; capture later.
- Authorize can move to AUTHORIZED, DECLINED, or FAILED.
- Capture is only valid from AUTHORIZED and only once.
- Expiry job handles un-captured authorizations.

### PAY-105 – Query payments
- Fetch one payment or list payments for a merchant.
- Support filters such as status and date range.
- Use cursor/keyset pagination instead of offset pagination for scale.
- Restrict results to the merchant’s own payment records.

### Overall meaning of Epic 1
The domain is not just “create a payment.” It is a full payment lifecycle system that handles:

- creation
- validation
- idempotency
- fraud gating
- authorization
- capture
- query
- refund and settlement follow-up

## 6. Epic 2: Bank Simulator & Resilience

This epic addresses the fact that payment systems depend on external banks that are slow, unavailable, or inconsistent.

### PAY-201 – Bank simulator
- Fake acquiring bank used for development and testing.
- Supports authorize, capture, refund, and status queries.
- Allows deterministic failure modes such as timeout, decline, and 5xx.
- Important for repeatable testing of failure scenarios.

### PAY-202 – Resilient bank integration
- Protect payment-service from external bank failures.
- Add timeouts, retries with backoff and jitter, circuit breaker, and bulkhead isolation.
- This prevents one dependency outage from cascading through the platform.

### PAY-203 – Unknown-outcome handling
- Handle the classic payment problem: “The bank timed out; did the charge happen?”
- Payment should move to AUTHORIZATION_UNKNOWN rather than simply FAILED.
- Recovery job polls the bank using the same idempotency reference.
- This is a major reliability and financial integrity story.

### Overall meaning of Epic 2
Payment processing is impossible without graceful handling of dependency failures. This epic ensures the system behaves safely even when the bank is flaky or unavailable.

## 7. Epic 3: Event-Driven Backbone

This epic introduces the platform’s asynchronous backbone using Kafka and outbox patterns.

### PAY-301 – Transactional outbox
- Ensure state changes and event publishing are atomic.
- Write the payment update and outbox row in the same database transaction.
- Relay publishes pending events to Kafka and marks them sent.
- Keeps event delivery durable through restarts.

### PAY-302 – Event contracts
- Define event types and schemas for payment state transitions.
- Include eventId, eventType, schemaVersion, paymentId, occurredAt, and correlationId.
- Use paymentId as the Kafka key to preserve ordering per payment.
- Keep event documentation in repo-level docs for visibility.

### PAY-303 – Idempotent consumers, retries and DLQ
- Ensure duplicate messages do not corrupt state.
- Use processed event tracking to avoid duplicate side effects.
- Retried messages with backoff and a dead-letter topic for poison messages.
- This makes Kafka at-least-once delivery safe.

### Overall meaning of Epic 3
This is the “nervous system” of the platform. It allows services like fraud, ledger, notification, and settlement to react to payment events without tight coupling.

## 8. Epic 4: Fraud Service

This epic introduces risk evaluation for payment acceptance.

### PAY-401 – Rule-based risk scoring
- Risk service evaluates payments against rules such as:
  - amount threshold
  - velocity by customer
  - blocklist matches
- Returns APPROVE, REVIEW, or REJECT.
- Stores assessments for audit.

### PAY-402 – Fraud check inside the payment flow
- New payments move to PENDING_FRAUD_CHECK before bank calls.
- REJECT leads to DECLINED with no bank interaction.
- REVIEW requires manual intervention.
- Decision on fail-open vs fail-closed must be explicitly documented.

### Overall meaning of Epic 4
Fraud is treated as a core part of the payment decision pipeline. It is not an afterthought.

## 9. Epic 5: Ledger Service

This epic adds accounting and financial integrity.

### PAY-501 – Double-entry ledger
- Record every money movement as balanced debit/credit entries.
- Use a ledger with accounts for customer funds, merchant payable, fee revenue, and bank clearing.
- Entries are immutable, with reversing entries for corrections only.
- Prevent duplicate processing of the same event.

### PAY-502 – Fees and merchant balance
- Compute platform fees with explicit rounding rules.
- Expose merchant balance from ledger entries.
- Balance is derived, not stored as a mutable counter.

### PAY-503 – Refunds
- Support full and partial refunds.
- Enforce a cap so total refunds cannot exceed captured value.
- Publish refund events as part of the payment lifecycle.

### Overall meaning of Epic 5
The ledger service makes the platform financially trustworthy. Without it, the system can process transactions but cannot prove the books are correct.

## 10. Cross-cutting design rules across the backlog

The stories repeatedly reinforce a few design principles:

### Money handling
- amount is stored as integer minor units, not floating-point numbers
- currency is explicit and validated
- rounding rules are defined and tested

### Idempotency
- same request should not create duplicates
- external bank calls and internal state transitions require idempotent handling

### Safety
- invalid transitions are rejected
- duplicates are prevented with unique constraints and processed-event tables
- retry logic is only used for safe operations

### Auditability
- state history is stored
- ledger entries are immutable
- correlation IDs are propagated across services

### Resilience
- retries, rate limiting, timeouts, circuit breakers, and bulkheads are explicit requirements

## 11. Dependency flow and sequencing

The backlog is intentionally staged:

1. Foundation and shared contracts
2. Payment creation + idempotency + state model
3. Bank interaction and recovery patterns
4. Event backbone and outbox patterns
5. Fraud and ledger services
6. Settlement and downstream financial flows

This creates a sensible build order:

- build the contract and infrastructure first
- then implement payment lifecycle
- then integrate external dependencies safely
- then become asynchronous and event-driven
- then add fraud and accounting correctness

## 12. Why this backlog is realistic and strong

This backlog reflects real payment platform patterns rather than toy examples:

- it accounts for financial correctness
- it handles failure modes realistically
- it treats money as high-risk data
- it includes operational concerns like retries, DLQ, and recovery jobs
- it assumes at-least-once messaging and idempotent consumers
- it recognizes the cost of duplicate work or incorrect accounting

## 13. Recommended implementation strategy

A practical roadmap would be:

1. Complete foundation stories first: PAY-001, PAY-002, PAY-003.
2. Implement core payment domain: PAY-101 through PAY-105.
3. Stand up bank simulator and resilience layer: PAY-201 through PAY-203.
4. Add transactional outbox and event contracts: PAY-301 through PAY-303.
5. Add fraud checks: PAY-401, PAY-402.
6. Add tledger and accounting: PAY-501 through PAY-503.
7. Move into settlement and notification flows once the financial core is solid.

## 14. Bottom line

The central story of the backlog is not simply “build another microservice platform.” The real goal is to build a payment platform that is:

- consistent
- resilient
- auditable
- financially correct
- safe under retries and outages
- ready for asynchronous event-driven growth

This is a classic enterprise-grade payment architecture backlog, with emphasis on correctness over speed.

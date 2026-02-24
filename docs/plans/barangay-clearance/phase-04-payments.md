# Phase 4 — Payments Module

**Status:** Not Started
**Estimated Timeline:** Week 4
**Priority:** High

---

## Goal

Implement stub payment processing with full idempotency logic (client-supplied UUID key, per-user scope, 24h TTL, PENDING→409, SUCCESS/FAILED→cached replay) and cash mark-as-paid for clerks.

---

## Dependencies

**Depends on:**
- Phase 0 (Scaffolding) — database schema, `FeeConfig` table seeded
- Phase 1 (Auth) — JWT security, `UserPrincipal`
- Phase 3 (Clearance) — payment attaches to a `ClearanceRequest`; `release()` requires `paymentStatus == PAID`

**Required by:**
- Phase 5 (PDF) — `release()` requires payment, which is now in Phase 4
- Phase 9 (Testing) — payment idempotency integration tests

**Can run in parallel with:** Phase 5 (PDF) and Phase 6 (Settings) can begin once Phase 3 is done, but Phase 5 needs Phase 4's `release()` guard to be testable end-to-end.

---

## Deliverables

### Backend
**Entity:**
- `payments/entity/Payment.java` — all PRD Section 7.6 fields; `responseBody` stored as `TEXT`/`JSONB`

**Enums:**
- `PaymentMethod`, `PaymentStatus` (`PENDING`, `SUCCESS`, `FAILED`), `PaymentProvider`

**Repository:**
- `payments/repository/PaymentRepository.java`
  - `findByIdempotencyKeyAndInitiatedByUserIdAndIdempotencyExpiresAtAfter(String key, UUID userId, Instant now)`
  - `findByClearanceRequestId(UUID clearanceRequestId)`

**Gateway Abstraction:**
- `payments/gateway/PaymentGateway.java` — interface: `initiate(PaymentRequest)`, `getProviderCode()`
- `payments/gateway/PaymentRequest.java` — Java `record`
- `payments/gateway/PaymentResult.java` — Java `record`
- `payments/gateway/StubPaymentGateway.java` — `@ConditionalOnProperty(name="payment.provider", havingValue="stub", matchIfMissing=true)`

**Service:**
- `payments/service/PaymentService.java` — full idempotency logic

**MapStruct Mapper:**
- `payments/service/mapper/PaymentMapper.java`

**Controller:**
- `payments/controller/PaymentController.java`

**DTOs:**
- `PaymentDTO.java` — includes `idempotent: boolean` flag

### Frontend
- "Pay Now" button on `/portal/requests/[id]` — generates `crypto.randomUUID()` per click
- Payment result feedback (success toast / error message)
- "Mark as Paid" button on `/backoffice/clearances/[id]` for CLERK role

---

## Key Implementation Notes

### Idempotency Logic (Full Flow)
1. Validate `Idempotency-Key` header is present and valid UUID v4 — else 400
2. Look up existing payment by `(idempotencyKey, userId)` within 24h TTL
3. If found and `PENDING` → throw `ConflictException` (409)
4. If found and `SUCCESS` or `FAILED` → deserialize `responseBody`, set `idempotent = true`, return cached result
5. If not found → create `Payment` with status `PENDING` + `saveAndFlush` (triggers unique constraint immediately)
6. Call `PaymentGateway.initiate()` → get `PaymentResult`
7. Update `Payment` status to `SUCCESS` or `FAILED`, serialize response to `responseBody`
8. Update `ClearanceRequest.paymentStatus = PAID` if success

### Concurrent PENDING Protection
`saveAndFlush` fires the composite unique constraint `(idempotency_key, initiated_by_user_id)` before the gateway call. Concurrent duplicate caught by `DataIntegrityViolationException` → handle as 409.

### Fee Resolution (ADR-010)
Fee read from `fee_config` at payment initiation time (not at request submission):
```java
BigDecimal amount = clearance.getUrgency() == Urgency.EXPRESS ? fees.getExpressFee() : fees.getRegularFee();
```
If fee is `0.00` → skip gateway, create `SUCCESS` payment immediately (waived fee edge case).

### `StubPaymentGateway`
Uses `ThreadLocalRandom` for thread safety. Configurable via `payment.stub.always-success` (default: `true`).

### Response Caching in `responseBody`
Serialize `PaymentDTO` to JSON string at update time. On replay, deserialize and set `idempotent = true`. Uses `ObjectMapper`.

### Controller Pattern
```
POST /clearances/{id}/payments → 201 (new) or 200 (idempotent replay)
POST /clearances/{id}/mark-paid → 200 (CLERK cash payment)
GET /clearances/{id}/payments → 200 (status check)
```

### Frontend Idempotency Key
```typescript
const idempotencyKey = crypto.randomUUID(); // browser native, no library
// Generated fresh per click — not persisted between page loads
```

---

## API Endpoints

| Method | Path | Role |
|--------|------|------|
| POST | `/api/v1/me/clearances/{id}/pay` | RESIDENT |
| POST | `/api/v1/clearances/{id}/payments` | RESIDENT, CLERK, ADMIN |
| POST | `/api/v1/clearances/{id}/mark-paid` | CLERK, ADMIN |
| GET | `/api/v1/clearances/{id}/payments` | CLERK, ADMIN |

---

## Definition of Done

- [ ] `POST /clearances/{id}/payments` with valid idempotency key + `APPROVED` clearance → 201 with `status: SUCCESS`
- [ ] Same request with same key (within 24h) → 200 with `idempotent: true`
- [ ] Same request with PENDING existing record → 409
- [ ] Missing `Idempotency-Key` header → 400
- [ ] `POST /clearances/{id}/mark-paid` as CLERK on `APPROVED + UNPAID` → 200; `paymentStatus = PAID`
- [ ] Duplicate `mark-paid` on already-PAID clearance → 200 with existing record (no error)
- [ ] Payment creates audit log entry `PAYMENT_SUCCESS` or `PAYMENT_CASH_RECORDED`

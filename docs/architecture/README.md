# Architecture Documentation — Barangay Clearance System

This directory contains comprehensive architecture and system design documentation for the Barangay Clearance System backend.

## Documents

| Document | Description | Audience |
|----------|-------------|----------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | System architecture, tech stack, design considerations, module structure, database ERD, security architecture, state machine, PDF generation, configuration, and deployment | Architects, senior developers |
| [API_REFERENCE.md](API_REFERENCE.md) | Complete API endpoint reference — request/response schemas, auth requirements, error codes, enumerations | All developers, frontend team |
| [ADR.md](ADR.md) | Architecture Decision Records — 18 decisions covering every major design choice with context, rationale, and consequences | Architects, tech leads, future maintainers |

## Quick Links

### Architecture
- [System Overview](ARCHITECTURE.md#1-system-overview)
- [Tech Stack](ARCHITECTURE.md#2-tech-stack)
- [Design Considerations](ARCHITECTURE.md#3-design-considerations)
- [Module Structure](ARCHITECTURE.md#5-module-structure)
- [Database ERD](ARCHITECTURE.md#6-database-design--entity-relationships)
- [Security Architecture](ARCHITECTURE.md#7-security-architecture)
- [Clearance State Machine](ARCHITECTURE.md#8-clearance-state-machine)
- [Deployment](ARCHITECTURE.md#12-infrastructure--deployment)

### API Reference
- [Authentication Endpoints](API_REFERENCE.md#1-authentication)
- [Resident Management](API_REFERENCE.md#3-residents)
- [Clearance Workflow](API_REFERENCE.md#4-clearances--backoffice)
- [Resident Portal](API_REFERENCE.md#5-clearances--resident-portal)
- [Payments](API_REFERENCE.md#6-payments)
- [Enumerations](API_REFERENCE.md#10-enumerations)

### Key Decisions (ADR)
- [ADR-001: Modular Monolith](ADR.md#adr-001-modular-monolith-over-microservices)
- [ADR-002: JWT Authentication](ADR.md#adr-002-stateless-jwt-authentication)
- [ADR-006: State Machine](ADR.md#adr-006-clearance-state-machine-enforced-in-service-layer)
- [ADR-007: Atomic Clearance Numbers](ADR.md#adr-007-atomic-clearance-number-generation-via-postgresql)
- [ADR-009: Payment Gateway Pattern](ADR.md#adr-009-payment-gateway-strategy-pattern)
- [ADR-010: Idempotent Payments](ADR.md#adr-010-idempotent-payments-via-composite-unique-index)
- [ADR-011: IDOR Prevention](ADR.md#adr-011-resident-identity-from-jwt-not-request-parameters)

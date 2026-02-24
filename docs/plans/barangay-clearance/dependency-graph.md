# Feature Dependency Graph

This document describes the dependency relationships between all phases, identifying which phases must be sequential and which can be implemented in parallel.

---

## Dependency Tree

```
Phase 0: Scaffolding & Infrastructure
└── Phase 1: Auth & JWT
    ├── Phase 2: Residents Module
    │   └── Phase 3: Clearance Module
    │       ├── Phase 4: Payments Module ─────────────────┐
    │       ├── Phase 5: PDF Generation ─────────────────┐ │
    │       └── Phase 7: Reports Module                  │ │
    └── Phase 6: Settings Module ─────────────────────── ┘ │
                                                           │
Phase 8: Frontend Polish ◄─────────────────────────────── ┘
(depends on all feature phases 1–7)

Phase 9: Testing & QA ◄── All phases 0–8

Phase 10: Deployment ◄── Phase 9
```

---

## Sequential Requirements (Must Be In Order)

These phases have hard dependencies and **cannot be parallelized**:

1. **Phase 0 → Phase 1** — Auth requires the database schema, shared exception infrastructure, and Spring Boot skeleton
2. **Phase 1 → Phase 2** — Residents requires the `User` entity, `AuthService` (registration flow), and JWT security
3. **Phase 2 → Phase 3** — Clearance requires the `Resident` entity and the resident registry
4. **Phase 3 → Phase 4** — Payments require a `ClearanceRequest` to attach to
5. **Phase 9 → Phase 10** — Deployment should follow successful QA

---

## Parallel Opportunities

These phases have no dependency on each other and **can be worked concurrently**:

### Group A: After Phase 3 completes (Week 4–5)
| Phase | What it does | Parallel with |
|---|---|---|
| **Phase 4** | Payments | Phase 5, Phase 6 |
| **Phase 5** | PDF Generation | Phase 4, Phase 6 |
| **Phase 6** | Settings Module | Phase 4, Phase 5 |

> Phase 5 has a **soft dependency** on Phase 6 (logo in PDF header) but can be implemented and unit-tested without it. Full end-to-end logo embedding testing requires Phase 6 complete.

### Group B: After Phase 4/5/6 complete (Week 6)
| Phase | What it does | Parallel with |
|---|---|---|
| **Phase 7** | Reports | Phase 8 |
| **Phase 8** | Frontend Polish | Phase 7 |

---

## Recommended Implementation Schedule

### Week 1
- **Phase 0** — Scaffolding (sequential, must complete first)

### Week 2
- **Phase 1** — Auth & JWT (sequential after Phase 0)
- Begin **Phase 2** (Residents) as soon as Phase 1 backend is done

### Week 2–3
- **Phase 2** — Residents (sequential after Phase 1)

### Week 3–4
- **Phase 3** — Clearance (sequential after Phase 2)

### Week 4–5 (Parallel Sprint)
- **Phase 4** — Payments ← can start immediately after Phase 3
- **Phase 5** — PDF Generation ← can start immediately after Phase 3
- **Phase 6** — Settings Module ← can start immediately after Phase 1 (no Phase 3 dependency)

> **Optimal strategy:** Phase 6 (Settings) can actually begin after Phase 1 since it only depends on the `barangay_settings` and `fee_config` tables (seeded in Phase 0 V2 migration) and admin authentication. However, since Phase 4 needs `FeeConfig` at payment time, complete Phase 6 backend before testing Phase 4 end-to-end.

### Week 6 (Parallel Sprint)
- **Phase 7** — Reports
- **Phase 8** — Frontend Polish & Integration

### Week 7
- **Phase 9** — Testing & QA (sequential — needs all features complete)
- Begin **Phase 10** infrastructure work in parallel (Dockerfiles, nginx config)

### Week 7–8
- **Phase 10** — Deployment (QA must pass first for production launch)

---

## Critical Path

The **critical path** (longest sequential chain) is:

**Phase 0 → Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 9 → Phase 10**

Any delay in these phases delays the overall project. Phase 5, 6, 7, and 8 are off the critical path and have schedule flexibility.

---

## Inter-Phase Integration Points

| Consumer Phase | Depends On | Specific Integration |
|---|---|---|
| Phase 1 (Auth) | Phase 0 | `User` entity, DB schema, `ErrorResponse` |
| Phase 2 (Residents) | Phase 1 | `AuthService.createFromRegistration()`, `UserRepository` |
| Phase 3 (Clearance) | Phase 2 | `Resident` entity FK in `ClearanceRequest` |
| Phase 4 (Payments) | Phase 3 | `ClearanceRequest` status check, `FeeConfig` from Phase 6 |
| Phase 5 (PDF) | Phase 3, Phase 6 | `ClearanceRequest`, `Resident`, `BarangaySettings` |
| Phase 6 (Settings) | Phase 0 | V2 seed migration, Phase 1 for admin security |
| Phase 7 (Reports) | Phase 3 | `ClearanceRequest` + `Resident` join query |
| Phase 8 (Frontend) | Phases 1–7 | All API endpoints, `AuthContext`, route guards |
| Phase 9 (Testing) | Phases 0–8 | All services and controllers |
| Phase 10 (Deploy) | Phase 9 | QA-passed application |

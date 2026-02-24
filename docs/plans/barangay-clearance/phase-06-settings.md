# Phase 6 — Settings Module

**Status:** Not Started
**Estimated Timeline:** Week 5
**Priority:** Medium

---

## Goal

Enable the admin to configure barangay profile (name, logo, municipality, province, captain name) and clearance fees, which flow into both the PDF and the payment amount calculation.

---

## Dependencies

**Depends on:**
- Phase 0 (Scaffolding) — `barangay_settings` and `fee_config` tables seeded by V2 migration
- Phase 1 (Auth) — ADMIN-only endpoints need JWT security

**Required by:**
- Phase 4 (Payments) — fee amounts from `fee_config` at payment initiation time
- Phase 5 (PDF) — `BarangaySettings` provides header content and logo

**Can run in parallel with:** Phase 3 (Clearance) and Phase 4 (Payments) — no dependency in either direction. Settings entities are already seeded; Phase 4 just reads `FeeConfig`, which works without the Settings controller being built.

---

## Deliverables

### Backend
**Entities:**
- `settings/entity/BarangaySettings.java` — `@Entity`, `id = 1` singleton row, `logoImage byte[]`
- `settings/entity/FeeConfig.java` — `@Entity`, `id = 1` singleton row, `regularFee`, `expressFee`

**Repositories:**
- `settings/repository/BarangaySettingsRepository.java` — `JpaRepository<BarangaySettings, Integer>`
- `settings/repository/FeeConfigRepository.java` — `JpaRepository<FeeConfig, Integer>`

**Service:**
- `settings/service/SettingsService.java`
  - `getSettings()` — always returns row id=1, throws `IllegalStateException` if not seeded
  - `updateSettings(BarangaySettingsDTO)` — patch fields; do NOT overwrite logo if not provided
  - `uploadLogo(byte[] bytes, String contentType)` — stores raw bytes
  - `getFees()` — returns row id=1
  - `updateFees(FeeConfigDTO)`

**Controller:**
- `settings/controller/SettingsController.java` — all `/api/v1/settings/**` (ADMIN only)

**DTOs:**
- `BarangaySettingsDTO.java` — excludes `logoImage` binary; includes `hasLogo: boolean`
- `FeeConfigDTO.java` — `{ regularFee, expressFee }`

### Frontend
- `src/app/backoffice/admin/settings/page.tsx` — form + logo upload with preview
- `src/app/backoffice/admin/settings/fees/page.tsx` — fee form
- `src/types/settings.ts`

---

## Key Implementation Notes

### Single-Row Pattern
```java
public BarangaySettings getSettings() {
    return settingsRepository.findById(1)
        .orElseThrow(() -> new IllegalStateException("barangay_settings not seeded by Flyway V2"));
}
```
Never allow `POST /settings` — only `PUT`. Row always exists from V2 seed migration.

### Logo Upload Validation
```java
// Accepted types: image/png, image/jpeg, image/gif
// Max size: 2MB (2 * 1024 * 1024 bytes)
```
Configure in `application.yml`:
```yaml
spring.servlet.multipart:
  max-file-size: 2MB
  max-request-size: 3MB
```

### Logo Retrieval
Provide separate `GET /settings/logo` endpoint returning raw bytes with stored `Content-Type`. Used by frontend `<img>` tag and the PDF service.

### Logo Preview in Frontend
Fetch logo as blob → `URL.createObjectURL()` → set as `<img>` `src`. Clean up with `useEffect` return to prevent memory leaks.

### Fee Update
Update `regularFee` and `expressFee` on the singleton row. Set `updatedAt = Instant.now()`.

### No Audit Log for Fees (MVP)
Add `SETTINGS_UPDATED` audit event in Phase 2 for compliance. Skip in MVP.

---

## API Endpoints

| Method | Path | Role |
|--------|------|------|
| GET | `/api/v1/settings` | ADMIN |
| PUT | `/api/v1/settings` | ADMIN |
| POST | `/api/v1/settings/logo` | ADMIN |
| GET | `/api/v1/settings/logo` | ADMIN, CLERK |
| GET | `/api/v1/settings/fees` | ADMIN |
| PUT | `/api/v1/settings/fees` | ADMIN |

---

## Definition of Done

- [ ] `GET /settings` as ADMIN → 200 with current settings
- [ ] `PUT /settings` updates barangay name; next PDF generation reflects new name
- [ ] `POST /settings/logo` with valid PNG ≤ 2MB → 204
- [ ] `POST /settings/logo` with oversized file → 400
- [ ] `POST /settings/logo` with non-image file → 400
- [ ] `PUT /settings/fees` updates fees; next payment initiation uses new fee amount
- [ ] Frontend: logo upload shows preview after selection; save shows success toast

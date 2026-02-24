# Phase 5 — PDF Generation

**Status:** Not Started
**Estimated Timeline:** Week 5
**Priority:** High

---

## Goal

Generate barangay clearance certificates on demand using Apache PDFBox 3.x. The PDF must be production-quality: A4 layout, correct fonts, logo embedding, and all required fields from the clearance request and resident profile.

---

## Dependencies

**Depends on:**
- Phase 0 (Scaffolding) — `pom.xml` with PDFBox 3.0.3 dependency
- Phase 1 (Auth) — JWT for endpoint security; resident portal endpoint needs RESIDENT role
- Phase 3 (Clearance) — `ClearanceRequest` + `Resident` entities; only RELEASED clearances generate PDF
- Phase 6 (Settings) — `BarangaySettings` provides barangay name, logo, captain name for PDF header

**Note:** Phase 6 (Settings) and Phase 5 (PDF) have a soft dependency — PDF can be implemented and tested without the logo/settings by using hardcoded defaults. However, the logo embedding requires Phase 6 to be fully functional for end-to-end testing.

**Required by:** Phase 9 (Testing) — `ClearancePdfServiceTest`

**Can run in parallel with:** Phase 4 (Payments) — PDF generation is independent of payment logic; both require Phase 3 to be done.

---

## Deliverables

### Backend
**Services:**
- `pdf/service/ClearancePdfService.java` — interface: `byte[] generate(ClearanceRequest, Resident, BarangaySettings)`
- `pdf/service/ClearancePdfServiceImpl.java` — PDFBox 3.x implementation

**Wire into controllers:**
- `GET /api/v1/clearances/{id}/pdf` — add to `ClearanceController` (CLERK, ADMIN)
- `GET /api/v1/me/clearances/{id}/pdf` — add to `PortalClearanceController` (RESIDENT, RELEASED only)

### Frontend
- "Download PDF" button on `/portal/requests/[id]` — visible only when `status = RELEASED`
- "Print / Download PDF" button on `/backoffice/clearances/[id]` — visible when `status = RELEASED`
- Binary blob download trigger using `URL.createObjectURL`

---

## Key Implementation Notes

### PDFBox 3.x Layout
Coordinate origin: bottom-left `(0,0)`. Track current `y` position, decrement as content is drawn downward.

```java
private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();    // 595.28 pt
private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();  // 841.89 pt
private static final float MARGIN = 50f;
private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
```

### PDF Structure (top to bottom)
1. **Header:** Logo (if present) + barangay info (republika, municipality, province, barangay name)
2. **Horizontal rule**
3. **Title:** "BARANGAY CLEARANCE" (centered, bold, 16pt)
4. **Metadata:** Clearance No., Date, Validity (6 months)
5. **Body paragraph:** Full text with resident name, age, birthdate, address, purok, purpose
6. **Signature block:** Captain name + position + signature line

### Logo Embedding
```java
PDImageXObject logo = PDImageXObject.createFromByteArray(doc, settings.getLogoImage(), "logo");
float logoHeight = Math.min(80f, logo.getHeight());
float logoWidth = logo.getWidth() * (logoHeight / logo.getHeight()); // proportional
```
If `settings.getLogoImage() == null` → skip logo, render text-only header (no exception).

### Text Wrapping Helper
PDFBox has no auto-wrap. Implement `wrapText(String, PDFont, fontSize, maxWidth)` that splits words and measures with `font.getStringWidth(text) / 1000 * fontSize`.

### Body Paragraph Text Template
```
"This is to certify that [firstName] [lastName], [age] years old, born on [birthdate],
residing at [address], Purok/Zone [purokZone], is a bona fide resident of this barangay
and has no derogatory record on file in this office. This clearance is issued upon the
request of the above-named person for the purpose of: [purpose]."
```
Compute age with `Period.between(resident.getBirthdate(), LocalDate.now()).getYears()`.

### Controller Response Pattern
```java
return ResponseEntity.ok()
    .contentType(MediaType.APPLICATION_PDF)
    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"clearance-" + number + ".pdf\"")
    .body(pdfBytes);
```
Portal endpoint: validate `status == RELEASED` before calling `generatePdf`; otherwise → 403.

### Frontend Download Trigger
```typescript
const response = await api.get(`/me/clearances/${id}/pdf`, { responseType: 'blob' });
const url = URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
const link = document.createElement('a');
link.href = url;
link.download = `clearance-${clearanceNumber}.pdf`;
link.click();
URL.revokeObjectURL(url);
```

---

## API Endpoints

| Method | Path | Role |
|--------|------|------|
| GET | `/api/v1/clearances/{id}/pdf` | CLERK, ADMIN |
| GET | `/api/v1/me/clearances/{id}/pdf` | RESIDENT (RELEASED only) |

---

## Definition of Done

- [ ] `GET /clearances/{id}/pdf` for RELEASED clearance → 200 with `Content-Type: application/pdf`
- [ ] PDF contains: clearance number, issuance date, resident full name, age, birthdate, address, purpose
- [ ] PDF contains: barangay name, municipality, province, captain name
- [ ] If logo uploaded in settings → logo appears in PDF header
- [ ] If no logo → header shows text only (no exception)
- [ ] `GET /me/clearances/{id}/pdf` for non-RELEASED clearance → 403
- [ ] `ClearancePdfServiceTest` generates non-null byte array starting with `%PDF` magic bytes

# Defect Log — Barangay Clearance System

> **Session Date:** March 6, 2026  
> **Environment:** Local development (macOS, Spring Boot 3.3.4, Next.js 14.2.35)

---

## DEF-001 — Login Page Module Error (`Cannot find module './948.js'`)

| Field        | Detail           |
| ------------ | ---------------- |
| **Severity** | Critical         |
| **Area**     | Frontend / Build |
| **Status**   | Resolved         |

### Description

The login page threw a module resolution error on load, making the application completely inaccessible.

### Root Cause

Stale `.next` build cache contained references to chunk files (e.g., `948.js`) that no longer existed after dependency or source changes. The Next.js incremental build did not invalidate the stale manifest.

### Resolution

Cleared the `.next` build directory and reinstalled `node_modules`:

```bash
cd frontend
rm -rf node_modules .next
npm install
npm run dev
```

---

## DEF-002 — Sidebar Text Appearing Black on Dark Background

| Field        | Detail             |
| ------------ | ------------------ |
| **Severity** | High               |
| **Area**     | Frontend / Styling |
| **Status**   | Resolved           |

### Description

Sidebar navigation text was rendered in black, making it unreadable against the dark `primary-900` sidebar background.

### Root Cause

The Tailwind configuration and `globals.css` were missing the `primary-200` (`#bbdefb`) and `primary-300` (`#64b5f6`) CSS custom property definitions. Components referencing `text-primary-200` or `text-primary-300` utility classes silently fell back to black (default color) since the values were undefined.

### Resolution

Added missing CSS variables to `frontend/src/app/globals.css`:

```css
--color-primary-200: #bbdefb;
--color-primary-300: #64b5f6;
```

Added corresponding mappings in `frontend/tailwind.config.ts`:

```ts
'primary-200': 'var(--color-primary-200)',
'primary-300': 'var(--color-primary-300)',
```

---

## DEF-003 — Form Input Text Invisible / Hard to Read

| Field        | Detail                   |
| ------------ | ------------------------ |
| **Severity** | High                     |
| **Area**     | Frontend / UI Components |
| **Status**   | Resolved                 |

### Description

Text typed into `Input`, `Select`, and `Textarea` components was nearly invisible (very light or transparent) in both form fields and placeholders.

### Root Cause

The shared UI components (`Input.tsx`, `Select.tsx`, `Textarea.tsx`) did not explicitly set a text color class. They inherited the parent's color, which defaulted to a very light tone in the theme context, making input text unreadable.

### Resolution

Added explicit color utility classes to each component:

- `Input.tsx` — added `text-neutral-900 placeholder-neutral-400`
- `Select.tsx` — added `text-neutral-900 bg-white`
- `Textarea.tsx` — added `text-neutral-900 placeholder-neutral-400`

**Files modified:**

- `frontend/src/components/ui/Input.tsx`
- `frontend/src/components/ui/Select.tsx`
- `frontend/src/components/ui/Textarea.tsx`

---

## DEF-004 — Fee Configuration Inputs Showing Redundant "Amount (₱)" Labels

| Field        | Detail                    |
| ------------ | ------------------------- |
| **Severity** | Low                       |
| **Area**     | Frontend / Admin Settings |
| **Status**   | Resolved                  |

### Description

The Standard and Rush fee input fields on the Fee Configuration page (`/backoffice/admin/settings/fees`) displayed a redundant "Amount (₱)" floating label inside the fields, even though the section heading already described the purpose.

### Root Cause

The `label` prop was passed to the `Input` component for both fee fields, producing floating label UI in a context where it was not needed.

### Resolution

Removed the `label` prop from both fee `Input` components in `frontend/src/app/backoffice/admin/settings/fees/page.tsx`.

---

## DEF-005 — Floating Labels Rendering Inside Input Fields (Multiple Pages)

| Field        | Detail                     |
| ------------ | -------------------------- |
| **Severity** | High                       |
| **Area**     | Frontend / Forms & Filters |
| **Status**   | Resolved                   |

### Description

Across several admin pages, filter and form fields displayed their labels **inside** the input box (as floating labels) rather than above them as separate label elements. Affected pages and fields:

| Page                                               | Fields Affected                                                                                |
| -------------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| Reports (`/backoffice/reports`)                    | Purok / Address, From, To                                                                      |
| Audit Logs (`/backoffice/admin/audit-logs`)        | Actor User ID, From, To                                                                        |
| User Accounts (`/backoffice/admin/users`)          | Search                                                                                         |
| Barangay Settings (`/backoffice/admin/settings`)   | Barangay Name, Municipality, Province, Barangay Captain                                        |
| View Resident (`/backoffice/residents/[id]`)       | First Name, Last Name, Middle Name, Birth Date, Gender, Address, Contact Number, Email, Status |
| New Walk-in Request (`/backoffice/clearances/new`) | Purpose, Purpose Description, Number of Copies, Notes                                          |

### Root Cause

The `Input`, `Select`, and `Textarea` components implement a floating label animation pattern via Framer Motion. This works for standalone forms but is visually broken in filter toolbars and compact settings forms — the placeholder/label visually overlaps the input content area. The `label` prop was being passed to these components in contexts where a static positioned label was expected.

### Resolution

Replaced the `label` prop pattern with a separate `<label>` element rendered above each input field using a consistent wrapper structure:

```tsx
// Before
<Input label="Field Name" {...register('field')} />

// After
<div>
  <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">
    Field Name
  </label>
  <Input {...register('field')} />
</div>
```

**Files modified:**

- `frontend/src/app/backoffice/reports/page.tsx`
- `frontend/src/app/backoffice/admin/audit-logs/page.tsx`
- `frontend/src/app/backoffice/admin/users/page.tsx`
- `frontend/src/app/backoffice/admin/settings/page.tsx`
- `frontend/src/app/backoffice/residents/[id]/page.tsx`
- `frontend/src/app/backoffice/clearances/new/page.tsx`

---

## DEF-006 — Selected Resident Not Displayed After Selection in Walk-in Request Form

| Field        | Detail                        |
| ------------ | ----------------------------- |
| **Severity** | High                          |
| **Area**     | Frontend / Clearance Workflow |
| **Status**   | Resolved                      |

### Description

On the New Walk-in Request page (`/backoffice/clearances/new`), clicking a resident from the search dropdown would update the hidden `residentId` form value but the visible search input would not clearly indicate which resident was selected. The search results list remained visible and there was no distinct "selected" state.

### Root Cause

The implementation set `residentSearch` state to the selected resident's name, which caused the resident list to re-fetch immediately (since `residentSearch` was used as the query parameter), collapsing the selection back to a search state. There was no dedicated "selected resident" display state separate from the search box.

### Resolution

Introduced a `selectedResidentDisplay` state to track and render the selected resident in a distinct green highlighted box. The search input is hidden once a resident is selected, replaced by:

1. A confirmation box showing `"LASTNAME, FIRSTNAME"` with green styling.
2. A "Clear" button to reset the selection and return to search mode.
3. The `useResidents` hook is only called with a query when actively searching (not after selection).

**File modified:** `frontend/src/app/backoffice/clearances/new/page.tsx`

---

## DEF-007 — Dev Server "Page Could Not Be Found" on All Routes

| Field        | Detail                             |
| ------------ | ---------------------------------- |
| **Severity** | Critical                           |
| **Area**     | Frontend / Development Environment |
| **Status**   | Resolved                           |

### Description

All application pages returned a "Page could not be found" or similar error when accessed via the browser.

### Root Cause

The Next.js development server defaults to port 3000, but ports 3000–3003 were already occupied by other processes running on the machine. Next.js automatically incremented the port to 3004 but the user was still attempting to access the app on port 3000–3002 due to a previously cached URL.

### Resolution

Identified the active port via Next.js startup logs. Application was accessible at `http://localhost:3004`. No code changes were required.

**Diagnostic command used:**

```bash
lsof -i :3002
```

---

## DEF-008 — Backend: `NoClassDefFoundError` for `ClearanceAuditListener$1` on Walk-in Request Creation

| Field        | Detail                  |
| ------------ | ----------------------- |
| **Severity** | Critical                |
| **Area**     | Backend / Audit Logging |
| **Status**   | Resolved                |

### Description

Creating a walk-in clearance request via `POST /api/v1/clearances` returned a 500 Internal Server Error. The backend logs showed:

```
java.lang.NoClassDefFoundError: com/barangay/clearance/clearance/service/ClearanceAuditListener$1
  at ClearanceAuditListener.resolveAction(ClearanceAuditListener.java:64)
  at ClearanceAuditListener.onStatusChanged(ClearanceAuditListener.java:42)
```

The clearance record itself was persisted successfully (confirmed by log line `CLEARANCE_CREATED (walk-in) id=...`), but the transaction was rolled back due to the unhandled exception thrown by the event listener.

### Root Cause

`ClearanceAuditListener.resolveAction()` uses a Java 21 switch expression. The Java compiler generates synthetic anonymous inner classes (`$1`, `$2`, …) for switch expressions involving sealed types or complex pattern matching. These synthetic classes were not present in the `target/classes` directory because the backend had not been fully recompiled after `ClearanceAuditListener.java` was introduced in Phase 12. The Spring Boot debug launcher loaded stale class files that were missing the synthetic companion classes.

```
Caused by: java.lang.ClassNotFoundException: com.barangay.clearance.clearance.service.ClearanceAuditListener$1
```

### Resolution

Performed a full clean rebuild of the backend:

```bash
cd backend
./mvnw clean compile
```

Then restarted the Spring Boot application via the VS Code debug launcher. The `clean` phase deleted the stale `target/` directory, forcing `javac` to regenerate all class files including the synthetic inner classes.

**Always run `./mvnw clean compile` after adding new source files or when encountering unexpected `NoClassDefFoundError` / `ClassNotFoundException` errors during development.**

---

## DEF-009 — Read-Only Form Fields Not Displaying as Disabled

| Field        | Detail                   |
| ------------ | ------------------------ |
| **Severity** | Medium                   |
| **Area**     | Frontend / UI Components |
| **Status**   | Resolved                 |

### Description

When viewing a resident profile in read-only mode (before clicking Edit), the form fields were disabled but visually indistinguishable from active input fields. Users could not tell which fields were read-only versus editable, reducing usability and creating confusion.

### Root Cause

The `Input`, `Select`, and `Textarea` components applied the `disabled` HTML attribute when the field was in read-only state, but the CSS styling did not reflect this. The components lacked conditional styling to apply visual indicators (background color, border color, text color, cursor style) when `disabled={true}`.

### Resolution

Enhanced the component styling in three files to conditionally apply disabled-state CSS classes:

```tsx
// Applied to Input, Select, and Textarea components:
className={clsx(
  props.disabled
    ? "bg-neutral-100 border-neutral-200 text-neutral-500 cursor-not-allowed"
    : "... active state classes ..."
)}
```

**Files modified:**

- `frontend/src/components/ui/Input.tsx`
- `frontend/src/components/ui/Select.tsx`
- `frontend/src/components/ui/Textarea.tsx`

**Changes:**

- Light gray background (`bg-neutral-100`) for disabled fields
- Lighter border color (`border-neutral-200`)
- Muted text color (`text-neutral-500`)
- "Not-allowed" cursor (`cursor-not-allowed`)

---

## DEF-010 — Edit Resident Form Updates Data on Edit Button Click (Should Require Explicit Save)

| Field        | Detail                     |
| ------------ | -------------------------- |
| **Severity** | High                       |
| **Area**     | Frontend / Form Submission |
| **Status**   | Resolved                   |

### Description

When a user clicked the "Edit" button on the resident detail page, the form entered edit mode but any field changes were immediately persisted to the backend without requiring an explicit "Save Changes" button click. Users expected a confirm/save step before data was committed.

### Root Cause

The form submission was not properly guarded by the `isEditing` state. The form could be submitted at any time, not just when the user explicitly clicked the "Save Changes" button. Additionally, the "Save Changes" button was missing the `type="submit"` attribute, and the form handler was not checking edit mode.

### Resolution

Implemented strict form submission controls:

1. **Added edit-mode guard to `onSubmit` handler:**

   ```tsx
   const onSubmit = async (data: FormData) => {
     if (!isEditing) return; // Only allow submission in edit mode
     // ... validation and API call
   };
   ```

2. **Wrapped form submission with explicit edit-mode check:**

   ```tsx
   <form
     onSubmit={(e) => {
       e.preventDefault();
       if (isEditing) {
         handleSubmit(onSubmit)(e);
       }
     }}
   />
   ```

3. **Added explicit button types:**
   - Edit button: `type="button"` — does not trigger form submission
   - Cancel button: `type="button"` — does not trigger form submission
   - Save Changes button: `type="submit"` — triggers form submission only when in edit mode

**Files modified:**

- `frontend/src/app/backoffice/residents/[id]/page.tsx`

**Expected behavior after fix:**

- View mode: Fields are read-only, only "Edit" button visible
- Click "Edit": Fields become editable, "Cancel" and "Save Changes" buttons appear
- Make changes: Form tracks dirty state via React Hook Form's `isDirty`
- Click "Save Changes": Data is persisted to backend
- Click "Cancel": Abandon changes, return to view mode

---

## DEF-011 — Portal Account Status Not Displayed on Resident Detail Page

| Field        | Detail                               |
| ------------ | ------------------------------------ |
| **Severity** | Medium                               |
| **Area**     | Frontend / Backend / Resident Detail |
| **Status**   | Resolved                             |

### Description

The resident detail page showed a "Portal Account" section with Activate and Reject buttons, but did not display the current status of the linked portal user account (ACTIVE, INACTIVE, PENDING_VERIFICATION, REJECTED, DEACTIVATED).

### Root Cause

The backend `ResidentDTO` did not include the portal user's status from the `User` table, and the frontend had no type definition or UI component to display it. The `Resident` entity stores a `userId` foreign key, but the status was not being hydrated or exposed.

### Resolution

**Backend changes:**

1. **Updated `ResidentDTO`** to include `portalStatus` field:

   ```java
   @Data
   @Builder
   public class ResidentDTO {
     // ... existing fields
     private User.UserStatus portalStatus;  // NEW
   }
   ```

2. **Updated `ResidentService`** to enrich DTOs with portal status:

   ```java
   private ResidentDTO enrichWithPortalStatus(ResidentDTO dto) {
     if (dto.isHasPortalAccount() && dto.getUserId() != null) {
       userRepository.findById(dto.getUserId())
               .ifPresent(user -> dto.setPortalStatus(user.getStatus()));
     }
     return dto;
   }
   ```

   Applied to all read methods: `search()`, `getById()`, `findPendingUsers()`, `create()`, `update()`

3. **Updated `ResidentMapper`** to ignore `portalStatus` during mapping:
   ```java
   @Mapping(target = "portalStatus", ignore = true)
   ResidentDTO toDTO(Resident resident);
   ```

**Frontend changes:**

1. **Updated `Resident` type** to include portal status:

   ```typescript
   export type UserStatus =
     | "ACTIVE"
     | "INACTIVE"
     | "PENDING_VERIFICATION"
     | "REJECTED"
     | "DEACTIVATED";
   export interface Resident {
     portalStatus?: UserStatus; // NEW
   }
   ```

2. **Enhanced `Badge` component** to support user status:
   - Added `user-status` variant
   - Defined color mappings for all `UserStatus` values
   - `ACTIVE`: green, `INACTIVE`: gray, `PENDING_VERIFICATION`: yellow, `REJECTED`: red, `DEACTIVATED`: gray

3. **Updated resident detail page** to display portal status:
   - Imported `Badge` component
   - Added status badge display in Portal Account section with label and colored dot indicator

**Files modified:**

- Backend: `ResidentDTO.java`, `ResidentService.java`, `ResidentMapper.java`
- Frontend: `resident.ts`, `Badge.tsx`, `residents/[id]/page.tsx`

---

## Summary Table

| ID      | Title                                       | Area                          | Severity | Status      |
| ------- | ------------------------------------------- | ----------------------------- | -------- | ----------- |
| DEF-001 | Login page module error                     | Frontend / Build              | Critical | ✅ Resolved |
| DEF-002 | Sidebar text black on dark background       | Frontend / Styling            | High     | ✅ Resolved |
| DEF-003 | Form input text invisible                   | Frontend / UI Components      | High     | ✅ Resolved |
| DEF-004 | Redundant fee configuration labels          | Frontend / Admin Settings     | Low      | ✅ Resolved |
| DEF-005 | Floating labels rendering inside inputs     | Frontend / Forms              | High     | ✅ Resolved |
| DEF-006 | Selected resident not displayed in form     | Frontend / Clearance Workflow | High     | ✅ Resolved |
| DEF-007 | "Page not found" on all routes (wrong port) | Frontend / Dev Environment    | Critical | ✅ Resolved |
| DEF-008 | `NoClassDefFoundError` on walk-in creation  | Backend / Audit Logging       | Critical | ✅ Resolved |
| DEF-009 | Read-only fields not displaying as disabled | Frontend / UI Components      | Medium   | ✅ Resolved |
| DEF-010 | Edit form submits immediately               | Frontend / Form Submission    | High     | ✅ Resolved |
| DEF-011 | Portal account status not displayed         | Frontend / Backend / Details  | Medium   | ✅ Resolved |

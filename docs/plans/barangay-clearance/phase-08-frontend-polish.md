# Phase 8 — Frontend Polish & Role-Based Navigation

**Status:** Not Started
**Estimated Timeline:** Week 6
**Priority:** High

---

## Goal

Wire up all frontend pages with real API integration, complete the `middleware.ts` route guard, implement the backoffice dashboard, and add polish: loading skeletons, error toasts, status timeline, and the "must change password" flow.

---

## Dependencies

**Depends on:**

- Phase 1 (Auth) — `AuthContext`, login/register pages (skeletons already in Phase 1)
- Phase 2 (Residents) — resident pages exist but may need final integration
- Phase 3 (Clearance) — clearance pages exist; dashboard summary endpoint
- Phase 4 (Payments) — "Pay Now" and "Mark as Paid" buttons
- Phase 5 (PDF) — "Download PDF" button
- Phase 7 (Reports) — reports page

**Note:** Phase 8 is primarily a polish/wiring phase. Individual page skeletons are created in their respective feature phases; this phase completes the integration and cross-cutting concerns.

**Required by:** Phase 9 (Testing) — manual QA flows require full frontend integration.

**Can run in parallel with:** Phase 7 (Reports) and Phase 6 (Settings) — all Week 6 work.

---

## Deliverables

### Frontend

**Route Guard (`middleware.ts` — complete):**

- Public routes: `/login`, `/register` — if already authenticated, redirect to role home
- Protected routes: redirect unauthenticated users to `/login`
- Role routing: RESIDENT → `/portal/*` only; CLERK/APPROVER/ADMIN → `/backoffice/*` only
- Admin guard: `/backoffice/admin/**` requires ADMIN role
- Add `jwt-decode` package for client-side JWT decoding (routing only; verification on backend)

**`AuthContext.tsx` — complete:**

- Interface: `user`, `accessToken`, `login`, `logout`, `refreshToken`
- Persist `accessToken` to `localStorage` on login; re-hydrate on app init
- Expose `refreshToken()` method called by Axios 401 interceptor

**Backoffice Dashboard:**

- `src/app/backoffice/dashboard/page.tsx` — 3 summary `<Card>` components (Pending Review, Approved Awaiting Payment, Released Today)
- Auto-refresh every 30s with `refetchInterval: 30000`
- `<Skeleton>` placeholders while loading

**Shared Components:**

- `src/components/shared/StatusBadge.tsx` — color-coded clearance status badge
- `src/components/shared/PaymentBadge.tsx` — payment status badge
- `src/components/shared/LoadingSkeleton.tsx` — reusable skeleton layouts
- `src/components/shared/ErrorToast.tsx` — standardized error notification
- `src/components/shared/PageHeader.tsx` — consistent page header

**Portal `StatusTimeline` Component:**
Steps: Submitted → Under Review / Approved → Payment → Released. Visual states: completed (green check), current (yellow spinner), pending (grey circle). REJECTED status shows red step with rejection reason.

**Error Toast System:**
Configure Radix UI `Toast` (shadcn/ui). `useToast` hook. All API errors trigger `toast.error(message)`. All success actions trigger `toast.success(...)`.

**Loading Skeletons:**
Apply `<Skeleton>` for all list and detail pages while TanStack Query `isLoading = true`.

**"Must Change Password" Flow:**

- Frontend detects `mustChangePassword: true` claim in JWT after login
- Redirects to `/change-password` page
- Backend: `PUT /api/v1/auth/change-password` validates current password, sets new, clears flag, returns new tokens
- Form: current password + new password + confirm fields

**Mobile-First Tailwind Breakpoints:**

- Default (mobile): single column stack
- `md:` (768px+): sidebar + main content for backoffice
- `lg:` (1024px+): wider sidebar, more table columns

---

## Key Implementation Notes

### `middleware.ts` Pattern

```typescript
// Uses jwt-decode for client-side role extraction (no signature verify — backend handles that)
const { role } = jwtDecode<{ role: string }>(token);
```

Matcher: `['/portal/:path*', '/backoffice/:path*', '/login', '/register']`

### `AuthContext` Re-hydration

On app init, read `accessToken` from `localStorage`. If present, decode to get `user` info and populate context. On `logout`, clear `localStorage` + context state.

### TanStack Query Integration

- `useQuery` for all GET requests
- `useMutation` for POST/PUT/DELETE
- `queryClient.invalidateQueries(...)` after mutations to refresh stale data
- Error handling in `onError` callback → trigger toast

### Backoffice Layout (Sidebar)

- Sidebar links filter by role (CLERK doesn't see Admin menu)
- Use Next.js `usePathname()` for active link highlighting

---

## Definition of Done

- [ ] Navigating to `/portal/dashboard` without token → redirects to `/login`
- [ ] CLERK token trying `/portal/dashboard` → redirected to `/backoffice/dashboard`
- [ ] RESIDENT token trying `/backoffice/clearances` → redirected to `/portal/dashboard`
- [ ] CLERK/APPROVER token trying `/backoffice/admin/users` → redirected to `/backoffice/dashboard`
- [ ] Dashboard stat cards load and show correct counts
- [x] Status timeline shows correct step for each clearance status (payment step added 2026-02-25)
- [ ] Loading skeletons appear before data loads; no layout shift on load
- [ ] Toast notifications appear on success and error
- [ ] First-login admin (`mustChangePassword=true`) is prompted to change password before accessing other pages

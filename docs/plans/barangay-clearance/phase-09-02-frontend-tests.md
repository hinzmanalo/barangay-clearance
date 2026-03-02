# Phase 9.2 — Frontend Unit Tests + Vitest Infrastructure

## Overview

Adds Vitest testing infrastructure to the Next.js 14 frontend and implements unit tests for AuthContext, React Query hooks, and middleware route guards. No E2E/Cypress tests — those are in Phase 9.3.

**PRD Reference:** [phase-09-testing-prd.md](phase-09-testing-prd.md) — Sections 4.15–4.17, 4.25

---

## Prerequisites

- Frontend app is functional (Phases 0–8 complete)
- `frontend/package.json` currently has zero test dependencies
- Node.js 20+ installed locally

---

## Step 1: Install test dependencies

**File:** `frontend/package.json`

Add to `devDependencies`:

```bash
cd frontend && npm install -D vitest @vitest/ui jsdom \
  @testing-library/react @testing-library/jest-dom @testing-library/user-event
```

Add scripts:

```json
{
  "scripts": {
    "test": "vitest run",
    "test:watch": "vitest"
  }
}
```

**Note:** Cypress is deferred to Phase 9.3. Do NOT install it here.

---

## Step 2: Create Vitest configuration

**File:** `frontend/vitest.config.ts`

```typescript
import { defineConfig } from 'vitest/config';
import path from 'path';

export default defineConfig({
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
});
```

**Key decisions:**
- `globals: true` — no need to import `describe`, `it`, `expect`
- `environment: 'jsdom'` — DOM simulation for React components
- Alias `@/` to `src/` matching Next.js `tsconfig.json` paths

---

## Step 3: Create test setup file

**File:** `frontend/src/test/setup.ts`

```typescript
import '@testing-library/jest-dom/vitest';
```

This adds extended matchers like `toBeInTheDocument()`, `toHaveTextContent()`, etc.

---

## Step 4: Create test utilities

**File:** `frontend/src/test/test-utils.tsx`

```typescript
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, type RenderOptions } from '@testing-library/react';
import { ReactElement } from 'react';

// Fresh QueryClient per test to prevent state leakage
export function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
}

export function renderWithQueryClient(
  ui: ReactElement,
  options?: Omit<RenderOptions, 'wrapper'>
) {
  const queryClient = createTestQueryClient();
  return render(ui, {
    wrapper: ({ children }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    ),
    ...options,
  });
}
```

---

## Step 5: Frontend Unit Tests — AuthContext

**File:** `frontend/src/context/__tests__/AuthContext.test.tsx`

**Target:** [AuthContext.tsx](../../frontend/src/context/AuthContext.tsx)

**Test setup:**
- Mock `@/lib/api` at module level: `vi.mock('@/lib/api')`
- Mock `jwt-decode`: `vi.mock('jwt-decode')` to control decoded payloads
- Mock `localStorage` via jsdom (available by default)
- Mock `document.cookie` setter

**Test methods:**

| # | Test | Description | Key assertion |
|---|------|-------------|---------------|
| 1 | `login stores tokens and sets authenticated state` | Call `login()`, mock api.post to return tokens | `localStorage.setItem` called with accessToken + refreshToken; context `isAuthenticated=true`, `role` matches decoded payload |
| 2 | `logout clears tokens and resets state` | Login first, then call `logout()` | `localStorage.removeItem` called for both tokens; context `isAuthenticated=false` |
| 3 | `hydration with valid token sets authenticated` | Pre-set `localStorage.accessToken` to a valid (non-expired) JWT, render AuthProvider | Context has `isAuthenticated=true` after mount; no API calls made |
| 4 | `hydration with expired token clears storage` | Pre-set localStorage with expired JWT (exp in past) | `localStorage.removeItem` called; context `isAuthenticated=false` |
| 5 | `hydration with no token sets unauthenticated` | Empty localStorage | `isAuthenticated=false`; `isLoading` transitions from true to false |

**Implementation notes:**
- The actual AuthContext does NOT call a refresh API on expired tokens during hydration — it simply clears storage. This differs from PRD section 4.15 which expects a refresh call. Test the actual behavior.
- Use `renderHook` from `@testing-library/react` to test the `useAuth()` hook within an `AuthProvider` wrapper.
- Mock `jwtDecode` to return controlled payloads with configurable `exp` timestamps.

**Deviations from PRD:**

| PRD Expectation | Actual Code | Test Adjustment |
|-----------------|-------------|-----------------|
| Re-hydration with expired token calls refresh API | Expired token is simply cleared | Test that storage is cleared, no refresh call |
| `isTokenValid()` boolean method | Uses try/catch on `jwtDecode` + exp check | Test decode failure → unauthenticated |

---

## Step 6: Frontend Unit Tests — React Query Hooks

### 6a: useClearances

**File:** `frontend/src/hooks/__tests__/useClearances.test.tsx`

**Target:** [useClearances.ts](../../frontend/src/hooks/useClearances.ts)

**Setup:** Mock `@/lib/api` module. Use `renderWithQueryClient` from test utils.

| # | Test | Description |
|---|------|-------------|
| 1 | `useClearances returns loading then data` | Mock `api.get` → paginated response; assert `isLoading=true` initially, then data matches |
| 2 | `useClearances returns error on failure` | Mock `api.get` → reject; assert `isError=true` |
| 3 | `useMyClearances returns resident's clearances` | Mock response; verify correct endpoint called |

### 6b: useResidents

**File:** `frontend/src/hooks/__tests__/useResidents.test.tsx`

**Target:** [useResidents.ts](../../frontend/src/hooks/useResidents.ts)

| # | Test | Description |
|---|------|-------------|
| 1 | `useResidents returns paginated data` | Mock response; assert data matches |
| 2 | `useResidents passes search query` | Call with `q: "Juan"`; verify API called with `?q=Juan` |
| 3 | `useResidents returns error on failure` | Mock reject; assert `isError=true` |

### 6c: useSettings

**File:** `frontend/src/hooks/__tests__/useSettings.test.tsx`

**Target:** [useSettings.ts](../../frontend/src/hooks/useSettings.ts)

| # | Test | Description |
|---|------|-------------|
| 1 | `useBarangaySettings returns settings data` | Mock GET; assert data |
| 2 | `useUpdateBarangaySettings calls PUT endpoint` | Trigger mutation; verify `api.put` called with payload |
| 3 | `useBarangaySettings returns error` | Mock reject; assert error |

### 6d: useReports

**File:** `frontend/src/hooks/__tests__/useReports.test.tsx`

**Target:** [useReports.ts](../../frontend/src/hooks/useReports.ts)

| # | Test | Description |
|---|------|-------------|
| 1 | `useReports returns filtered data` | Mock with date range params; assert data |
| 2 | `useReports returns error on 403` | Mock 403 reject; assert `isError=true` |

**Implementation notes for all hook tests:**
- Use `renderHook` from `@testing-library/react`
- Wrap in `QueryClientProvider` with a fresh `QueryClient` per test (retry: false)
- Use `waitFor` to assert async state transitions
- Mock at the `api` (Axios instance) level, not at `fetch` level

---

## Step 7: Frontend Unit Tests — Middleware Route Guards

**File:** `frontend/src/__tests__/middleware.test.ts`

**Target:** [middleware.ts](../../frontend/src/middleware.ts)

**Test setup:**
- Mock `jwt-decode` at module level
- Mock `next/server` — specifically `NextRequest` and `NextResponse`
- Create helper to build mock `NextRequest` objects with optional cookies

**Test methods:**

| # | Test | Description | Expected |
|---|------|-------------|----------|
| 1 | `unauthenticated → /backoffice/clearances` | No accessToken cookie | Redirect to `/login?next=/backoffice/clearances` |
| 2 | `unauthenticated → /portal/my-requests` | No accessToken cookie | Redirect to `/login?next=/portal/my-requests` |
| 3 | `RESIDENT → /backoffice/clearances` | Cookie with RESIDENT role token | Redirect to `/portal/dashboard` |
| 4 | `CLERK → /backoffice/clearances` | Cookie with CLERK role token | `NextResponse.next()` (pass through) |
| 5 | `ADMIN → /backoffice/admin/users` | Cookie with ADMIN role token | `NextResponse.next()` (pass through) |
| 6 | `RESIDENT → /portal/my-requests` | Cookie with RESIDENT role token | `NextResponse.next()` (pass through) |
| 7 | `unauthenticated → /login` | No cookie | `NextResponse.next()` (public route) |
| 8 | `authenticated → /login` | Valid non-expired token | Redirect to role-appropriate home |
| 9 | `CLERK → /backoffice/admin/users` | Cookie with CLERK role | Redirect to `/backoffice/dashboard` (not ADMIN) |
| 10 | `ADMIN → /portal/dashboard` | Cookie with ADMIN role | Redirect to `/backoffice/dashboard` (staff can't access portal) |
| 11 | `expired token → /backoffice` | Cookie with expired token | Redirect to `/login` |

**Implementation notes:**
- The middleware reads tokens from cookies (not localStorage) — `request.cookies.get('accessToken')?.value`
- `jwtDecode` is used to decode the token and check `exp` and `role`
- Mock `NextRequest` with a `cookies` map and `nextUrl` with `pathname`
- Mock `NextResponse.redirect()` and `NextResponse.next()` to return identifiable objects
- The matcher config excludes `/api`, `/_next/*`, and `favicon.ico` — these paths won't hit middleware

**Key differences from PRD:**

| PRD | Actual | Impact |
|-----|--------|--------|
| RESIDENT redirected to `/portal` | Redirected to `/portal/dashboard` | Assert `/portal/dashboard` |
| CLERK allowed on `/backoffice/*` | Yes, BACKOFFICE_ROLES includes CLERK | Matches PRD |
| `/api/v1/auth/register` always allowed | API routes excluded by matcher config | Not testable in middleware unit test (never hits middleware) |

---

## Step 8: Verify all frontend tests pass

**Command:** `cd frontend && npm run test`

**Definition of Done:**

- [ ] `vitest.config.ts` is created with jsdom environment and `@/` alias
- [ ] `src/test/setup.ts` imports `@testing-library/jest-dom/vitest`
- [ ] `src/test/test-utils.tsx` provides `renderWithQueryClient` helper
- [ ] `AuthContext.test.tsx` — 5 tests pass (login, logout, hydration x3)
- [ ] Hook tests — minimum 2 tests per hook (loading/success + error), totaling ~10 tests
- [ ] `middleware.test.ts` — 11 route guard tests pass
- [ ] `npm run test` completes in under 30 seconds
- [ ] No test depends on a running backend or network access
- [ ] All mocks are isolated per test — no state leakage between tests

---

## File Summary

| File | Type | Tests |
|------|------|-------|
| `frontend/vitest.config.ts` | Config | — |
| `frontend/src/test/setup.ts` | Setup | — |
| `frontend/src/test/test-utils.tsx` | Utility | — |
| `frontend/src/context/__tests__/AuthContext.test.tsx` | Test | 5 |
| `frontend/src/hooks/__tests__/useClearances.test.tsx` | Test | 3 |
| `frontend/src/hooks/__tests__/useResidents.test.tsx` | Test | 3 |
| `frontend/src/hooks/__tests__/useSettings.test.tsx` | Test | 3 |
| `frontend/src/hooks/__tests__/useReports.test.tsx` | Test | 2 |
| `frontend/src/__tests__/middleware.test.ts` | Test | 11 |
| **Total** | | **~27 tests** |

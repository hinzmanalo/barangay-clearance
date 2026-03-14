# Fix: Intermittent Login Redirect on Navigation

## Problem

After successfully logging in, navigating to certain pages (e.g., `/backoffice/residents`) occasionally redirects the user back to `/login`. This is intermittent and unrelated to refresh token expiry.

## Root Cause

`middleware.ts` is the gatekeeper for all protected routes — it only reads the `accessToken` **cookie**. When Axios silently refreshes a 401'd token in `api.ts`, the new access token is written only to `localStorage` but the **cookie is never updated**.

The next time the user navigates, Next.js fires an RSC prefetch against the server — middleware runs, finds the old/expired cookie, and redirects to `/login`. This triggers exactly when the 15-min access token has expired but a silent background refresh has already issued a new one in the background.

### Contributing Factors

| #   | Location                                                       | Issue                                                                                                                                                                                     |
| --- | -------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | `frontend/src/lib/api.ts` line 63                              | After silent token refresh, `localStorage` is updated but `document.cookie` is **never synced** — middleware still sees the old/expired cookie                                            |
| 2   | `frontend/src/context/AuthContext.tsx`                         | `isLoading: true` on initial state; hydration runs in `useEffect` (after first paint) — there's no `isInitialized` flag to distinguish "not yet checked" from "genuinely unauthenticated" |
| 3   | `frontend/src/app/portal/layout.tsx` / `backoffice/layout.tsx` | Neither layout guards `{children}` behind `isLoading` — component-level redirects can fire during the brief hydration window                                                              |

### Sequence That Produces the Bug

```
User logs in          → cookie set (15-min TTL) + localStorage updated
~15 min pass          → cookie expires; localStorage still has old AT + valid RT
User navigates        → Next.js RSC prefetch hits server
                      → middleware runs → old cookie expired → redirect /login
                      (even though a silent refresh would have succeeded client-side)
```

---

## Fix Plan

### Step 1 — Sync cookie after silent refresh in `api.ts` _(primary fix)_

**File:** `frontend/src/lib/api.ts`

After `localStorage.setItem('accessToken', newAccessToken)`, decode the new token and write the cookie — identical to the pattern already used in `AuthContext.login()`:

```ts
import { jwtDecode } from "jwt-decode";
import type { JwtPayload } from "@/types/auth";

// Inside the refresh success block:
localStorage.setItem("accessToken", newAccessToken);

// Sync cookie so Next.js middleware sees the new token
if (typeof window !== "undefined") {
  try {
    const payload = jwtDecode<JwtPayload>(newAccessToken);
    document.cookie = `accessToken=${newAccessToken}; path=/; SameSite=Lax; max-age=${payload.exp - Math.floor(Date.now() / 1000)}`;
  } catch {
    /* ignore decode errors */
  }
}
```

### Step 2 — Extract a shared `syncCookie` helper _(removes divergent cookie-write paths)_

**File:** `frontend/src/context/AuthContext.tsx`

Extract a module-level utility `syncCookie(token: string, payload: JwtPayload)` that writes `document.cookie`. Call it from:

- `login()` — replaces the inline `document.cookie = …` line
- `useEffect` hydration block — replaces the inline write
- `api.ts` step 1 — import and reuse

This ensures all three code paths use the same cookie format and expiry calculation.

### Step 3 — Add `isInitialized` flag to `AuthContext` _(hardening)_

**File:** `frontend/src/context/AuthContext.tsx`

Add a separate `isInitialized: boolean` (distinct from `isLoading`) that flips to `true` once the `useEffect` hydration completes — regardless of whether the token was valid or not. Expose it on the context value.

```ts
interface AuthState {
  // ... existing fields
  isLoading: boolean;
  isInitialized: boolean; // ← new
}
```

### Step 4 — Guard layouts during hydration _(hardening)_

**Files:** `frontend/src/app/portal/layout.tsx`, `frontend/src/app/backoffice/layout.tsx`

Use `isInitialized` from `useAuth()` to render a loading skeleton instead of children during the brief SSR→client hydration window:

```tsx
const { isInitialized } = useAuth();
if (!isInitialized) return <FullPageSpinner />;
return <>{children}</>;
```

---

## Verification Steps

1. Log in, wait ~15–16 minutes (let the AT cookie expire naturally while keeping the tab open)
2. Make any API call to trigger a silent refresh (or navigate to any menu item)
3. Immediately navigate to `/backoffice/residents` — should **not** redirect to `/login`
4. Hard-reload the app — session should restore correctly (cookie re-synced by `useEffect`)
5. Open DevTools → Application → Cookies — confirm `accessToken` cookie timestamp updates after a silent refresh

---

## Notes

- **Step 1 is sufficient** to fix the reported bug. Steps 3 & 4 are hardening for the hydration edge case.
- The cookie is written with `SameSite=Lax` (no `Secure` flag) to match existing behavior for local HTTP dev. In production over HTTPS, also add `; Secure`.

---

## Suggested Commit Message

```
fix: sync accessToken cookie after silent refresh to prevent middleware redirect 🔑⚡🍪
```

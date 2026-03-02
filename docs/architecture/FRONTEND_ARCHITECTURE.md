# Barangay Clearance System — Frontend Architecture & System Design

> **Audience:** Frontend developers and full-stack engineers
> **Stack:** Next.js 14 · TypeScript 5 · Tailwind CSS 3 · TanStack React Query v5 · Axios · React Hook Form + Zod
> **Pattern:** App Router with role-based route segmentation

---

## Table of Contents

1. [Overview](#1-overview)
2. [Tech Stack](#2-tech-stack)
3. [Design Decisions](#3-design-decisions)
4. [Project Structure](#4-project-structure)
5. [Routing & Access Control](#5-routing--access-control)
6. [Authentication Architecture](#6-authentication-architecture)
7. [Data Fetching Layer](#7-data-fetching-layer)
8. [Component Architecture](#8-component-architecture)
9. [Form Handling & Validation](#9-form-handling--validation)
10. [TypeScript Type System](#10-typescript-type-system)
11. [State Management](#11-state-management)
12. [API Integration](#12-api-integration)
13. [Styling System](#13-styling-system)
14. [Error Handling](#14-error-handling)
15. [Configuration](#15-configuration)

---

## 1. Overview

The frontend is a **Next.js 14 App Router** application that serves two distinct user experiences:

- **Resident Portal** — Where residents apply for clearances, track status, pay fees, and download their clearance PDF.
- **Back-office Dashboard** — Where barangay staff (Clerk, Approver, Admin) manage clearance requests, residents, payments, settings, and reports.

Both experiences share a common authentication layer, API client, and component library, but are visually and functionally distinct.

### User Journeys

```
RESIDENT
  Register → Login → Portal Dashboard → Submit Request → Track Status → Pay → Download PDF

CLERK
  Login → Backoffice → Create Walk-in Request OR Release Clearance OR Record Cash Payment

APPROVER
  Login → Backoffice → Review Pending Requests → Approve / Reject

ADMIN
  Login → Backoffice → All Staff Actions + Manage Settings, Fees, Staff Accounts
```

---

## 2. Tech Stack

| Component | Technology | Version | Rationale |
|-----------|-----------|---------|-----------|
| Framework | Next.js | 14 | App Router; SSR/SSG; React 18 Server Components |
| Language | TypeScript | 5.x | Strict typing across API boundaries; prevents runtime errors |
| Styling | Tailwind CSS | 3.x | Utility-first; no runtime computation; tree-shaken in production |
| UI Components | shadcn/ui (Radix) | latest | Accessible, composable primitives; dependency declared |
| Forms | React Hook Form | 7.x | Uncontrolled inputs; minimal re-renders; native validation integration |
| Schema Validation | Zod | 4.x | Runtime-safe schemas shared between form and type inference |
| Data Fetching | TanStack React Query | v5 | Declarative server state; caching; background refetch; mutation lifecycle |
| HTTP Client | Axios | 1.x | Request/response interceptors for 401→refresh→retry flow |
| JWT Parsing | jwt-decode | 4.x | Client-side decode without signature verification (trusted server tokens) |
| Toast Notifications | Sonner | 2.x | Accessible toast API; works with App Router |
| Icons | Lucide React | 0.5xx | Consistent icon set; tree-shakable |

---

## 3. Design Decisions

### 3.1 App Router over Pages Router

**Decision:** Next.js 14 App Router with nested layouts.

**Rationale:**
- Nested layouts eliminate layout duplication across portal vs. backoffice sections
- Route-level code splitting is automatic — residents never download backoffice JS bundles
- `layout.tsx` files provide stable shell components that do not re-mount on navigation
- Layouts at `/portal/layout.tsx` and `/backoffice/layout.tsx` enforce visual separation by role

### 3.2 Client-Side Auth State with Cookie Sync

**Decision:** Auth state lives in `AuthContext` (client-side), with the access token mirrored to an HTTP cookie for server-side middleware access.

**Rationale:**
- Next.js middleware runs on the Edge Runtime and cannot import `localStorage`
- The cookie (set by `AuthContext.login()`) allows `middleware.ts` to gate routes server-side before any page renders
- The token is **not** HttpOnly — the Axios interceptor reads it from `localStorage` on each request
- Actual token validation happens on the backend; the middleware performs only expiry checks for redirect decisions

### 3.3 Layered Data Fetching with React Query

**Decision:** All server state is fetched and cached via TanStack React Query hooks. No `useEffect` + `useState` for data.

**Rationale:**
- Built-in caching with `staleTime: 30_000` prevents redundant API calls on tab switch
- `useMutation` lifecycle callbacks (`onSuccess`, `onError`) centralize cache invalidation and toast notifications
- Query key factories enable targeted cache invalidation without over-fetching
- Background refetch keeps dashboard stats current without manual polling

### 3.4 Domain-Scoped Hook Files

**Decision:** One hook file per domain (`useClearances.ts`, `useResidents.ts`, etc.) instead of co-locating hooks with pages.

**Rationale:**
- Multiple pages can share the same hook (e.g., both `/backoffice/clearances` and `/backoffice/dashboard` use `useClearanceSummary()`)
- Query key factories are defined in the same file, making cache invalidation relationships explicit
- Easier to test hooks independently of UI

### 3.5 Zod Schemas Co-located with Forms

**Decision:** Zod validation schemas are defined directly above the component that uses them, not in a shared `schemas/` directory.

**Rationale:**
- Each form's schema is only used in one place — sharing would be premature abstraction
- Type inference from Zod (`z.infer<typeof schema>`) keeps form types DRY without a separate `types/` entry
- Colocated schemas reduce file-jumping when modifying forms

### 3.6 Middleware-Level Route Protection

**Decision:** `middleware.ts` intercepts all non-public routes and verifies the auth cookie before the page renders.

**Rationale:**
- Prevents authenticated page content from flashing before client-side checks complete
- Users with invalid/expired tokens are redirected to `/login?next={path}` immediately
- Role-based redirects (e.g., RESIDENT accessing `/backoffice`) happen before any component mounts

---

## 4. Project Structure

```
frontend/
├── next.config.mjs                    # Minimal Next.js config (defaults)
├── tailwind.config.ts                 # Content paths, theme extensions
├── tsconfig.json                      # strict=true, path alias @/*
├── package.json                       # Dependencies
│
└── src/
    ├── app/                           # Next.js App Router root
    │   ├── layout.tsx                 # Root HTML shell + metadata
    │   ├── page.tsx                   # / → redirects to /login
    │   ├── providers.tsx              # QueryClientProvider + AuthProvider
    │   │
    │   ├── login/
    │   │   └── page.tsx               # Email/password login form
    │   ├── register/
    │   │   └── page.tsx               # Resident self-registration
    │   ├── change-password/
    │   │   └── page.tsx               # Forced password change on first login
    │   │
    │   ├── portal/                    # RESIDENT-only segment
    │   │   ├── layout.tsx             # Portal sidebar + header
    │   │   ├── dashboard/
    │   │   │   └── page.tsx           # My clearance requests list
    │   │   └── requests/
    │   │       ├── new/
    │   │       │   └── page.tsx       # Submit new clearance request
    │   │       └── [id]/
    │   │           └── page.tsx       # Request detail: status, pay, download
    │   │
    │   └── backoffice/                # ADMIN | CLERK | APPROVER segment
    │       ├── layout.tsx             # Backoffice sidebar + header
    │       ├── dashboard/
    │       │   └── page.tsx           # Summary stats cards
    │       ├── clearances/
    │       │   ├── page.tsx           # Paginated list with filters
    │       │   ├── new/
    │       │   │   └── page.tsx       # Walk-in clearance creation
    │       │   └── [id]/
    │       │       └── page.tsx       # Detail: approve, reject, release
    │       ├── residents/
    │       │   ├── page.tsx           # Paginated resident list
    │       │   ├── new/
    │       │   │   └── page.tsx       # Create walk-in resident
    │       │   └── [id]/
    │       │       └── page.tsx       # Edit resident details
    │       ├── reports/
    │       │   └── page.tsx           # Filtered clearance reports
    │       └── admin/                 # ADMIN-only sub-segment
    │           └── settings/
    │               ├── page.tsx       # Barangay profile (name, captain, logo)
    │               └── fees/
    │                   └── page.tsx   # Standard / rush fee config
    │
    ├── components/
    │   ├── shared/                    # Used by both portal and backoffice
    │   │   ├── StatusBadge.tsx        # Color-coded clearance status
    │   │   ├── PaymentBadge.tsx       # Color-coded payment status
    │   │   ├── LoadingSkeleton.tsx    # Skeleton placeholders
    │   │   ├── ErrorToast.tsx         # Sonner toast helpers
    │   │   └── PageHeader.tsx         # Consistent page title
    │   │
    │   ├── backoffice/                # Back-office only components
    │   │   ├── Sidebar.tsx            # Role-aware staff navigation
    │   │   ├── ClearanceTable.tsx     # Paginated clearance table
    │   │   ├── ResidentTable.tsx      # Paginated resident table
    │   │   └── ActionButtons.tsx      # Approve / Reject / Release / Mark-Paid
    │   │
    │   └── portal/                    # Resident portal only components
    │       ├── Sidebar.tsx            # Resident navigation
    │       ├── RequestCard.tsx        # Compact clearance summary card
    │       └── StatusTimeline.tsx     # Visual state progression
    │
    ├── context/
    │   └── AuthContext.tsx            # Auth state, login(), logout(), useAuth()
    │
    ├── hooks/                         # React Query domain hooks
    │   ├── useClearances.ts           # Clearance queries + mutations
    │   ├── useResidents.ts            # Resident queries + mutations
    │   ├── useSettings.ts             # Settings + fee config queries
    │   └── useReports.ts             # Reports queries
    │
    ├── lib/
    │   └── api.ts                     # Axios instance + interceptors
    │
    ├── types/                         # TypeScript domain interfaces
    │   ├── auth.ts
    │   ├── clearance.ts
    │   ├── resident.ts
    │   ├── payment.ts
    │   ├── settings.ts
    │   └── common.ts
    │
    └── middleware.ts                  # Edge runtime route guard
```

---

## 5. Routing & Access Control

### 5.1 Route Map

```
/                          → Redirect to /login
/login                     → Public (redirect if authenticated)
/register                  → Public (redirect if authenticated)
/change-password           → Any authenticated user (mustChangePassword=true)
/portal/**                 → RESIDENT role only
/backoffice/**             → ADMIN | CLERK | APPROVER role
/backoffice/admin/**       → ADMIN role only
```

### 5.2 Role Permission Matrix

| Route | RESIDENT | CLERK | APPROVER | ADMIN | Public |
|-------|----------|-------|----------|-------|--------|
| `/login` | ✗ → portal | ✗ → backoffice | ✗ → backoffice | ✗ → backoffice | ✓ |
| `/register` | ✗ → portal | ✗ → backoffice | ✗ → backoffice | ✗ → backoffice | ✓ |
| `/change-password` | ✓ | ✓ | ✓ | ✓ | ✗ |
| `/portal/dashboard` | ✓ | ✗ → backoffice | ✗ → backoffice | ✗ → backoffice | ✗ |
| `/portal/requests/new` | ✓ | ✗ | ✗ | ✗ | ✗ |
| `/portal/requests/[id]` | ✓ | ✗ | ✗ | ✗ | ✗ |
| `/backoffice/dashboard` | ✗ → portal | ✓ | ✓ | ✓ | ✗ |
| `/backoffice/clearances` | ✗ → portal | ✓ | ✓ | ✓ | ✗ |
| `/backoffice/residents` | ✗ → portal | ✓ | ✓ | ✓ | ✗ |
| `/backoffice/reports` | ✗ → portal | ✓ | ✓ | ✓ | ✗ |
| `/backoffice/admin/**` | ✗ | ✗ → dashboard | ✗ → dashboard | ✓ | ✗ |

### 5.3 Middleware Route Guard Flow

```
Incoming Request
    │
    ▼
middleware.ts (Edge Runtime)
    │
    ├── Is route public? (/login, /register, /api/*)
    │       YES → Pass through
    │
    ├── Read accessToken cookie
    │       MISSING or EXPIRED → Redirect /login?next={pathname}
    │
    ├── jwt-decode(token) → extract { role, exp }
    │
    ├── Is route /portal/**?
    │       role !== RESIDENT → Redirect /backoffice/dashboard
    │
    ├── Is route /backoffice/admin/**?
    │       role !== ADMIN → Redirect /backoffice/dashboard
    │
    └── Is route /backoffice/**?
            role === RESIDENT → Redirect /portal/dashboard
            ELSE → Pass through
```

### 5.4 Client-Side Role-Based Rendering

Beyond route-level protection, components conditionally render based on role:

```tsx
// Sidebar shows Admin section only for ADMIN role
const { role } = useAuth();

{role === 'ADMIN' && (
  <NavSection title="Administration">
    <NavLink href="/backoffice/admin/settings">Settings</NavLink>
    <NavLink href="/backoffice/admin/settings/fees">Fees</NavLink>
  </NavSection>
)}
```

```tsx
// Action buttons scoped to role capabilities
{(role === 'ADMIN' || role === 'APPROVER') && (
  <ApproveButton clearanceId={id} />
)}
{(role === 'ADMIN' || role === 'CLERK') && (
  <ReleaseButton clearanceId={id} />
)}
```

---

## 6. Authentication Architecture

### 6.1 Auth Flow

```
┌──────────────┐    POST /api/v1/auth/login       ┌──────────────────┐
│    Browser   │ ──────────────────────────────►  │  Spring Boot API  │
│              │   { email, password }             └────────┬─────────┘
│              │                                            │ BCrypt verify
│              │                                            │ Generate tokens
│              │   { accessToken, refreshToken,             │
│              │ ◄────────────────────────────────────────── expiresIn, mustChangePassword }
│              │
│  AuthContext │
│  .login()    │──► localStorage.setItem('accessToken')
│              │──► localStorage.setItem('refreshToken')
│              │──► document.cookie = 'accessToken=...'  ← for middleware
│              │──► decodeToken() → set { userId, email, role, mustChangePassword }
│              │
│              │──► if mustChangePassword → router.push('/change-password')
│              │──► else → router.push(role === RESIDENT ? '/portal/dashboard' : '/backoffice/dashboard')
└──────────────┘
```

### 6.2 Token Refresh Flow (Axios Interceptor)

```
Axios Response Interceptor
    │
    ├── Response OK → Pass through
    │
    └── Response 401
            │
            ├── isRefreshing? → Queue this request (promise array)
            │
            ├── Set isRefreshing = true
            ├── POST /api/v1/auth/refresh { refreshToken }
            │       │
            │       ├── SUCCESS
            │       │     └── Store new accessToken
            │       │         Flush queued requests with new token
            │       │         Retry original request
            │       │
            │       └── FAILURE
            │             └── Clear all tokens
            │                 Redirect to /login
            │
            └── Set isRefreshing = false
```

### 6.3 AuthContext API

```typescript
interface AuthContextValue {
  // State
  userId: string | null;
  email: string | null;
  role: Role | null;
  mustChangePassword: boolean;
  isAuthenticated: boolean;
  isLoading: boolean;           // true during hydration from localStorage

  // Actions
  login(email: string, password: string): Promise<void>;
  logout(): Promise<void>;
  clearAuth(): void;            // Used by Axios on 401 failure
}
```

### 6.4 Token Storage Strategy

| Token | Storage | Reason |
|-------|---------|--------|
| `accessToken` | `localStorage` | Read by Axios interceptor on every request |
| `accessToken` | `document.cookie` (mirrored) | Read by Next.js Edge middleware |
| `refreshToken` | `localStorage` | Used only by Axios refresh interceptor |

> **Why not HttpOnly cookies?** HttpOnly cookies cannot be read by JavaScript. The Axios request interceptor needs to read the `accessToken` directly, so it must be accessible via `localStorage`. The cookie mirror is exclusively for the middleware.

### 6.5 Session Hydration on Page Load

```
App Mount
    │
    ▼
AuthContext useEffect
    │
    ├── localStorage.getItem('accessToken')
    │       MISSING → isAuthenticated = false, isLoading = false
    │
    ├── jwtDecode(token) → payload
    │
    ├── payload.exp * 1000 > Date.now()?
    │       NO (expired) → clearAuth(), isLoading = false
    │
    └── YES → set userId, email, role, mustChangePassword
              sync cookie
              isAuthenticated = true
              isLoading = false
```

---

## 7. Data Fetching Layer

### 7.1 React Query Configuration

```typescript
// app/providers.tsx
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,   // Data considered fresh for 30 seconds
      retry: 1,            // Retry once on failure before showing error
    },
  },
});
```

### 7.2 Query Key Factory Pattern

Every hook file exports a key factory for predictable cache targeting:

```typescript
// hooks/useClearances.ts
export const clearanceKeys = {
  all: ['clearances'] as const,
  lists: () => [...clearanceKeys.all, 'list'] as const,
  list: (params: ClearanceListParams) => [...clearanceKeys.lists(), params] as const,
  detail: (id: string) => [...clearanceKeys.all, 'detail', id] as const,
  summary: () => [...clearanceKeys.all, 'summary'] as const,
  myList: () => [...clearanceKeys.all, 'my-list'] as const,
  myDetail: (id: string) => [...clearanceKeys.all, 'my-detail', id] as const,
  payments: (id: string) => [...clearanceKeys.all, 'payments', id] as const,
};
```

### 7.3 Hook Inventory

#### Clearance Hooks (`useClearances.ts`)

| Hook | HTTP | Endpoint | Roles |
|------|------|----------|-------|
| `useClearances(params)` | GET | `/api/v1/clearances` | ADMIN, CLERK, APPROVER |
| `useClearance(id)` | GET | `/api/v1/clearances/{id}` | ADMIN, CLERK, APPROVER |
| `useClearanceSummary()` | GET | `/api/v1/clearances/summary` | ADMIN, CLERK, APPROVER |
| `useMyClearances()` | GET | `/api/v1/me/clearances` | RESIDENT |
| `useMyClearance(id)` | GET | `/api/v1/me/clearances/{id}` | RESIDENT |
| `useCreateWalkInClearance()` | POST | `/api/v1/clearances` | ADMIN, CLERK |
| `useSubmitClearance()` | POST | `/api/v1/me/clearances` | RESIDENT |
| `useResubmitClearance()` | POST | `/api/v1/me/clearances/{id}/resubmit` | RESIDENT |
| `useApproveClearance()` | PATCH | `/api/v1/clearances/{id}/approve` | ADMIN, APPROVER |
| `useRejectClearance()` | PATCH | `/api/v1/clearances/{id}/reject` | ADMIN, APPROVER |
| `useReleaseClearance()` | PATCH | `/api/v1/clearances/{id}/release` | ADMIN, CLERK |
| `usePayClearance()` | POST | `/api/v1/me/clearances/{id}/pay` | RESIDENT |
| `useMarkClearancePaid()` | POST | `/api/v1/clearances/{id}/mark-paid` | ADMIN, CLERK |
| `useClearancePayments(id)` | GET | `/api/v1/clearances/{id}/payments` | ADMIN, CLERK |

#### Resident Hooks (`useResidents.ts`)

| Hook | HTTP | Endpoint | Roles |
|------|------|----------|-------|
| `useResidents(params)` | GET | `/api/v1/residents` | ADMIN, CLERK |
| `useResident(id)` | GET | `/api/v1/residents/{id}` | ADMIN, CLERK |
| `usePendingResidents()` | GET | `/api/v1/residents?status=PENDING` | ADMIN, CLERK |
| `useCreateResident()` | POST | `/api/v1/residents` | ADMIN, CLERK |
| `useUpdateResident()` | PUT | `/api/v1/residents/{id}` | ADMIN, CLERK |
| `useActivateResident()` | PATCH | `/api/v1/residents/{id}/activate` | ADMIN, CLERK |
| `useRejectResident()` | PATCH | `/api/v1/residents/{id}/reject` | ADMIN, CLERK |

#### Settings Hooks (`useSettings.ts`)

| Hook | HTTP | Endpoint | Roles |
|------|------|----------|-------|
| `useBarangaySettings()` | GET | `/api/v1/settings/barangay` | ADMIN |
| `useUpdateBarangaySettings()` | PUT | `/api/v1/settings/barangay` | ADMIN |
| `useUploadLogo()` | POST | `/api/v1/settings/barangay/logo` | ADMIN |
| `useFeeConfig()` | GET | `/api/v1/settings/fees` | ADMIN |
| `useUpdateFeeConfig()` | PUT | `/api/v1/settings/fees` | ADMIN |

#### Reports Hook (`useReports.ts`)

| Hook | HTTP | Endpoint | Roles |
|------|------|----------|-------|
| `useReports(filters)` | GET | `/api/v1/reports` | ADMIN, CLERK, APPROVER |

### 7.4 Mutation Pattern

All mutations follow a consistent lifecycle pattern:

```typescript
export function useApproveClearance() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) =>
      api.patch(`/api/v1/clearances/${id}/approve`).then(r => r.data),

    onSuccess: (_, id) => {
      // 1. Invalidate the specific clearance detail
      queryClient.invalidateQueries({ queryKey: clearanceKeys.detail(id) });
      // 2. Invalidate the list (status changed)
      queryClient.invalidateQueries({ queryKey: clearanceKeys.lists() });
      // 3. Invalidate the summary dashboard counts
      queryClient.invalidateQueries({ queryKey: clearanceKeys.summary() });
      // 4. Show success feedback
      toast.success('Clearance approved');
    },

    onError: (error: AxiosError<{ message: string }>) => {
      toast.error(error.response?.data?.message ?? 'Failed to approve clearance');
    },
  });
}
```

### 7.5 Idempotency Key Generation (Payments)

When a resident initiates a payment, a fresh UUID v4 is generated client-side:

```typescript
export function usePayClearance() {
  return useMutation({
    mutationFn: ({ clearanceId }: { clearanceId: string }) => {
      const idempotencyKey = crypto.randomUUID(); // Fresh UUID per attempt
      return api.post(
        `/api/v1/me/clearances/${clearanceId}/pay`,
        {},
        { headers: { 'Idempotency-Key': idempotencyKey } }
      ).then(r => r.data);
    },
  });
}
```

### 7.6 PDF Download Pattern

PDF downloads use a fetch-then-blob approach since Axios cannot open browser download dialogs:

```typescript
async function downloadMyClearancePdf(clearanceId: string, clearanceNumber: string) {
  const response = await api.get(`/api/v1/me/clearances/${clearanceId}/pdf`, {
    responseType: 'blob',
  });
  const url = URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = url;
  link.download = `clearance-${clearanceNumber}.pdf`;
  link.click();
  URL.revokeObjectURL(url);
}
```

---

## 8. Component Architecture

### 8.1 Component Hierarchy

```
app/layout.tsx (Root HTML)
└── providers.tsx (QueryClientProvider + AuthProvider)
    │
    ├── app/backoffice/layout.tsx
    │   ├── Sidebar (backoffice)      ← role-aware navigation
    │   ├── Header                    ← email, role badge, logout
    │   └── {children}               ← page content
    │       ├── dashboard/page.tsx
    │       │   └── summary stat cards
    │       ├── clearances/page.tsx
    │       │   ├── ClearanceTable
    │       │   └── ActionButtons
    │       └── clearances/[id]/page.tsx
    │           ├── StatusBadge
    │           ├── PaymentBadge
    │           └── ActionButtons
    │
    └── app/portal/layout.tsx
        ├── Sidebar (portal)          ← resident navigation
        ├── Header                    ← email, logout
        └── {children}
            ├── dashboard/page.tsx
            │   └── RequestCard[]
            └── requests/[id]/page.tsx
                ├── StatusTimeline
                ├── PaymentBadge
                └── [Pay Button / Download Button]
```

### 8.2 Shared Components

#### `StatusBadge`

Renders a color-coded pill for clearance status values:

| Status | Color |
|--------|-------|
| `DRAFT` | Gray |
| `FOR_APPROVAL` | Yellow |
| `APPROVED` | Green |
| `REJECTED` | Red |
| `RELEASED` | Blue |

#### `PaymentBadge`

Renders a color-coded pill for payment status values:

| Status | Color |
|--------|-------|
| `UNPAID` | Orange |
| `PAID` | Green |
| `WAIVED` | Purple |

#### `LoadingSkeleton`

Provides placeholder UI during data fetching:
- `<TableRowSkeleton>` — Animated gray bars matching table row layout
- `<CardSkeleton>` — Animated gray block matching card layout
- Generic `<Skeleton height={n}>` — Custom height

#### `PageHeader`

Consistent page title rendering:
```tsx
<PageHeader title="Clearance Requests" subtitle="Manage all clearance applications" />
```

### 8.3 Backoffice Components

#### `Sidebar` (Backoffice)

- Dark background (`bg-gray-900`)
- Navigation links: Dashboard, Clearances, Residents, Reports
- Admin section (conditionally rendered for `ADMIN` role only): Settings, Fees
- Active link highlighted with `bg-gray-700`
- Mobile: hidden by default, toggle via hamburger button

#### `ClearanceTable`

| Column | Source |
|--------|--------|
| Clearance # | `clearanceNumber` (nullable, shown as `—` until released) |
| Resident | `residentName` |
| Purpose | `purpose` (formatted label) |
| Urgency | `urgency` badge |
| Status | `<StatusBadge>` |
| Payment | `<PaymentBadge>` |
| Date | `createdAt` (formatted) |
| Actions | Link to detail page |

Supports pagination via `page` / `totalPages` from `PageResponse<T>`.

#### `ActionButtons`

Renders the correct action buttons for the current clearance state and user role:

```
State: FOR_APPROVAL + Role: APPROVER/ADMIN  →  [Approve] [Reject]
State: APPROVED + PaymentStatus: UNPAID + Role: CLERK/ADMIN  →  [Mark Paid]
State: APPROVED + PaymentStatus: PAID + Role: CLERK/ADMIN  →  [Release]
State: RELEASED  →  [Download PDF]
```

### 8.4 Portal Components

#### `StatusTimeline`

Visual horizontal progression showing the clearance workflow:

```
● DRAFT → ● FOR_APPROVAL → ● APPROVED → ● RELEASED
                            ↘ ● REJECTED
```

Each step is highlighted (filled circle) or grayed (empty circle) based on the current `status`.

#### `RequestCard`

Compact card rendering key clearance fields:
- Clearance number (or `—` if not yet assigned)
- Purpose label
- Urgency badge
- Status badge + Payment badge
- Fee amount
- Submission date

---

## 9. Form Handling & Validation

### 9.1 Pattern

All forms use **React Hook Form** with a **Zod resolver**:

```typescript
const schema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
});

type FormData = z.infer<typeof schema>;

export default function LoginPage() {
  const form = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '' },
  });

  const onSubmit = form.handleSubmit(async (data) => {
    await login(data.email, data.password);
  });
}
```

### 9.2 Form Inventory

| Form | Location | Fields |
|------|----------|--------|
| **Login** | `/login` | email, password |
| **Register** | `/register` | email, password, firstName, lastName, middleName (opt), birthDate, gender, address, contactNumber (opt) |
| **Change Password** | `/change-password` | currentPassword, newPassword, confirmPassword |
| **Create Clearance (Portal)** | `/portal/requests/new` | purpose, purposeOther (if OTHER), urgency, copies, notes (opt) |
| **Create Clearance (Walk-in)** | `/backoffice/clearances/new` | residentId (search), purpose, urgency, copies, feeOverride (opt) |
| **Reject Clearance** | `/backoffice/clearances/[id]` | reason (textarea, required) |
| **Create Resident** | `/backoffice/residents/new` | firstName, lastName, middleName (opt), birthDate, gender, address, contactNumber (opt), email (opt) |
| **Edit Resident** | `/backoffice/residents/[id]` | Same as Create + status |
| **Barangay Settings** | `/backoffice/admin/settings` | barangayName, municipality, province, captainName, logo (file upload) |
| **Fee Config** | `/backoffice/admin/settings/fees` | standardFee, rushFee |

### 9.3 Common Validation Rules

```typescript
// Shared patterns used across forms
const nameField = z.string().min(1).max(100);
const optionalName = z.string().max(100).optional();
const contactNumber = z.string().max(20).optional();
const fee = z.number().positive('Fee must be positive');
const passwordConfirm = (schema: ZodObject) =>
  schema.refine(data => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });
```

### 9.4 Error Display Pattern

Server-side errors are displayed in a red alert box above the form submit button:

```tsx
{serverError && (
  <div className="rounded-md bg-red-50 p-3 text-sm text-red-700">
    {serverError}
  </div>
)}
```

Field-level errors appear below each input:
```tsx
{form.formState.errors.email && (
  <p className="mt-1 text-xs text-red-600">
    {form.formState.errors.email.message}
  </p>
)}
```

---

## 10. TypeScript Type System

### 10.1 Domain Types

#### `types/auth.ts`

```typescript
type Role = 'ADMIN' | 'CLERK' | 'APPROVER' | 'RESIDENT';
type UserStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING_VERIFICATION' | 'REJECTED' | 'DEACTIVATED';

interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  status: UserStatus;
  mustChangePassword: boolean;
}

interface JwtPayload {
  sub: string;          // userId
  email: string;
  role: Role;
  mustChangePassword: boolean;
  iat: number;
  exp: number;
}

interface TokenResponse {
  accessToken: string;
  refreshToken?: string;
  tokenType: string;
  expiresIn: number;
  mustChangePassword?: boolean;
}
```

#### `types/clearance.ts`

```typescript
type ClearanceStatus = 'DRAFT' | 'FOR_APPROVAL' | 'APPROVED' | 'REJECTED' | 'RELEASED';
type ClearancePaymentStatus = 'UNPAID' | 'PAID' | 'WAIVED';
type Urgency = 'STANDARD' | 'RUSH';
type Purpose =
  | 'EMPLOYMENT'
  | 'TRAVEL_ABROAD'
  | 'SCHOLARSHIP'
  | 'LOAN'
  | 'BUSINESS_PERMIT'
  | 'LEGAL'
  | 'CEDULA'
  | 'OTHER';

interface ClearanceRequest {
  id: string;
  clearanceNumber: string | null;
  residentId: string;
  residentName: string | null;       // Denormalized for display
  requestedBy: string;
  purpose: Purpose;
  purposeOther: string | null;
  urgency: Urgency;
  feeAmount: number;
  copies: number;
  status: ClearanceStatus;
  paymentStatus: ClearancePaymentStatus;
  notes: string | null;
  reviewedBy: string | null;
  reviewedAt: string | null;
  issuedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

interface ClearanceSummary {
  pendingApproval: number;
  approved: number;
  awaitingPayment: number;
  releasedToday: number;
}
```

#### `types/resident.ts`

```typescript
type Gender = 'MALE' | 'FEMALE' | 'OTHER';
type ResidentStatus = 'ACTIVE' | 'INACTIVE';

interface Resident {
  id: string;
  userId: string | null;           // null for walk-in residents
  firstName: string;
  middleName: string | null;
  lastName: string;
  birthDate: string;               // ISO 8601 date
  gender: Gender;
  address: string;
  contactNumber: string | null;
  email: string | null;
  status: ResidentStatus;
  hasPortalAccount: boolean;
  createdAt: string;
  updatedAt: string;
}
```

#### `types/payment.ts`

```typescript
type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED';
type PaymentMethod = 'STUB' | 'CASH';

interface PaymentDTO {
  id: string;
  clearanceRequestId: string;
  amount: number;
  idempotencyKey: string;
  initiatedByUserId: string;
  paymentMethod: PaymentMethod;
  provider: string;
  status: PaymentStatus;
  idempotent: boolean;             // true if this was a duplicate request
  createdAt: string;
  updatedAt: string;
}
```

#### `types/common.ts`

```typescript
interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

interface ErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp: string;
  path: string;
  details?: string[];
}
```

---

## 11. State Management

### 11.1 State Layers

| State Type | Tool | Scope | Persistence |
|-----------|------|-------|-------------|
| **Authentication** | `AuthContext` + `localStorage` | Global | localStorage + cookie |
| **Server data** | TanStack React Query | Global (cache) | In-memory (TTL 30s) |
| **Form state** | React Hook Form | Component | Component lifecycle |
| **UI state** (sidebar open/close) | `useState` | Component | Component lifecycle |
| **Filter/pagination state** | URL search params | Page | URL (survives refresh) |
| **Notifications** | Sonner | Global | None |

### 11.2 No Global UI Store

The application intentionally avoids Redux or Zustand for UI state. Reasons:

- The only truly global state is authentication — handled by `AuthContext`
- All server data is managed by React Query, which is purpose-built for it
- Local component state (`useState`) is sufficient for sidebar toggle, modal visibility, etc.
- URL search params handle filter state, enabling bookmarkable/shareable filtered views

### 11.3 Auth Context Flow

```
AuthProvider mounts
    │
    ├── isLoading = true
    ├── Hydrate from localStorage
    └── isLoading = false
        │
        ├── Renders {children} (app)
        │
        └── Components call useAuth() hook
                → { role, isAuthenticated, login, logout }
```

---

## 12. API Integration

### 12.1 Axios Instance

```typescript
// lib/api.ts
const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
});
```

### 12.2 Request Interceptor

Attaches the access token to every outgoing request:

```typescript
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

### 12.3 Response Interceptor

Handles 401 Unauthorized with queued refresh:

```
401 Response received
    │
    ├── Already refreshing?
    │     YES → Queue this request (resolve when refresh completes)
    │
    ├── isRefreshing = true
    │
    ├── POST /api/v1/auth/refresh { refreshToken: localStorage.refreshToken }
    │
    │   SUCCESS:
    │     └── Store new accessToken in localStorage + cookie
    │         Flush queue (resolve all waiting requests)
    │         Retry original request with new token
    │
    │   FAILURE:
    │     └── clearAuth() (clears localStorage + cookie)
    │         Flush queue (reject all waiting requests)
    │         window.location.href = '/login'
    │
    └── isRefreshing = false
```

### 12.4 API Base URL

| Environment | Value |
|-------------|-------|
| Development | `http://localhost:8080` (default) |
| Production | Set via `NEXT_PUBLIC_API_URL` environment variable |

### 12.5 Endpoint Reference

| Category | Method | Path | Description |
|----------|--------|------|-------------|
| **Auth** | POST | `/api/v1/auth/login` | Login |
| | POST | `/api/v1/auth/register` | Resident registration |
| | POST | `/api/v1/auth/refresh` | Refresh access token |
| | POST | `/api/v1/auth/logout` | Revoke refresh token |
| | POST | `/api/v1/auth/change-password` | Change password |
| **Clearances** | GET | `/api/v1/clearances` | List all (paginated, filtered) |
| | POST | `/api/v1/clearances` | Create walk-in |
| | GET | `/api/v1/clearances/{id}` | Get detail |
| | PATCH | `/api/v1/clearances/{id}/approve` | Approve |
| | PATCH | `/api/v1/clearances/{id}/reject` | Reject |
| | PATCH | `/api/v1/clearances/{id}/release` | Release |
| | POST | `/api/v1/clearances/{id}/mark-paid` | Record cash payment |
| | GET | `/api/v1/clearances/{id}/pdf` | Download PDF (blob) |
| | GET | `/api/v1/clearances/summary` | Dashboard counts |
| **Portal** | GET | `/api/v1/me/clearances` | My clearances |
| | POST | `/api/v1/me/clearances` | Submit clearance |
| | GET | `/api/v1/me/clearances/{id}` | My clearance detail |
| | POST | `/api/v1/me/clearances/{id}/resubmit` | Resubmit rejected |
| | POST | `/api/v1/me/clearances/{id}/pay` | Pay online |
| | GET | `/api/v1/me/clearances/{id}/pdf` | Download own PDF |
| **Residents** | GET | `/api/v1/residents` | List (paginated, filtered) |
| | POST | `/api/v1/residents` | Create |
| | GET | `/api/v1/residents/{id}` | Get detail |
| | PUT | `/api/v1/residents/{id}` | Update |
| | PATCH | `/api/v1/residents/{id}/activate` | Activate account |
| | PATCH | `/api/v1/residents/{id}/reject` | Reject account |
| **Settings** | GET | `/api/v1/settings/barangay` | Get barangay profile |
| | PUT | `/api/v1/settings/barangay` | Update barangay profile |
| | POST | `/api/v1/settings/barangay/logo` | Upload logo |
| | GET | `/api/v1/settings/fees` | Get fee config |
| | PUT | `/api/v1/settings/fees` | Update fee config |
| **Reports** | GET | `/api/v1/reports` | Clearance report (paginated) |

---

## 13. Styling System

### 13.1 Tailwind Configuration

```typescript
// tailwind.config.ts
export default {
  content: [
    'src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    'src/components/**/*.{js,ts,jsx,tsx,mdx}',
    'src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        background: 'var(--background)',
        foreground: 'var(--foreground)',
      },
    },
  },
};
```

### 13.2 Visual Identity

| Area | Background | Accent |
|------|-----------|--------|
| Backoffice sidebar | `gray-900` | `gray-700` (active) |
| Portal sidebar | `blue-900` | `blue-700` (active) |
| Primary buttons | `blue-600` | `blue-700` (hover) |
| Danger buttons | `red-600` | `red-700` (hover) |
| Page background | `gray-50` / `white` | — |

### 13.3 Badge Color Conventions

```
ClearanceStatus:
  DRAFT         → bg-gray-100    text-gray-700
  FOR_APPROVAL  → bg-yellow-100  text-yellow-800
  APPROVED      → bg-green-100   text-green-700
  REJECTED      → bg-red-100     text-red-700
  RELEASED      → bg-blue-100    text-blue-700

PaymentStatus:
  UNPAID        → bg-orange-100  text-orange-700
  PAID          → bg-green-100   text-green-700
  WAIVED        → bg-purple-100  text-purple-700
```

### 13.4 Layout Patterns

**Backoffice / Portal layouts** share the same structure:

```
┌──────────────────────────────────────────────┐
│  Sidebar (fixed left, w-64)                  │  ← role-specific
│  ┌────────────────────────────────────────┐  │
│  │  Logo / System Name                    │  │
│  │  Nav Links                             │  │
│  │  [Admin Section — ADMIN only]          │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  Main Content (flex-1)                       │
│  ┌────────────────────────────────────────┐  │
│  │  Top bar: email | role badge | logout  │  │
│  ├────────────────────────────────────────┤  │
│  │  {children} (page content)             │  │
│  └────────────────────────────────────────┘  │
└──────────────────────────────────────────────┘
```

**Mobile behavior:**
- Sidebar hidden by default on small screens
- Hamburger button toggles sidebar visibility
- Content area takes full width when sidebar is hidden

---

## 14. Error Handling

### 14.1 Error Boundaries

No explicit React Error Boundary implemented. Next.js `error.tsx` files can be added per-route for boundary-level handling. Currently, errors surface through:

1. React Query's `isError` / `error` state on queries
2. `onError` callbacks on mutations
3. Axios interceptor for 401 handling

### 14.2 Error Display Patterns

| Scenario | UI Pattern |
|----------|-----------|
| Query loading | `<LoadingSkeleton>` placeholder |
| Query error | Inline red alert box with message |
| Mutation success | Sonner green toast (top-right) |
| Mutation error | Sonner red toast (top-right) |
| Form validation error | Inline red text below field |
| API 401 (expired token) | Auto-refresh → transparent to user |
| API 401 (refresh failed) | Redirect to `/login` |
| API 403 (forbidden) | Toast: "You don't have permission" |
| API 404 | Toast: "Not found" |
| API 409 (conflict) | Toast: backend message (e.g., duplicate payment) |
| API 500 | Toast: "An unexpected error occurred" |

### 14.3 Toast Configuration

```tsx
// app/layout.tsx
import { Toaster } from 'sonner';

<Toaster
  position="top-right"
  closeButton
  richColors
/>
```

---

## 15. Configuration

### 15.1 Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `NEXT_PUBLIC_API_URL` | No | `http://localhost:8080` | Backend base URL |

### 15.2 Next.js Configuration

```javascript
// next.config.mjs
const nextConfig = {};
export default nextConfig;
```

No rewrites, redirects, or custom headers configured. The Nginx reverse proxy handles routing in production.

### 15.3 TypeScript Configuration

```json
{
  "compilerOptions": {
    "strict": true,
    "target": "ES2017",
    "moduleResolution": "bundler",
    "jsx": "preserve",
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

The `@/` alias resolves to `src/`, enabling clean imports:
```typescript
import { api } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import type { ClearanceRequest } from '@/types/clearance';
```

### 15.4 Development vs. Production

| Concern | Development | Production |
|---------|-------------|------------|
| API URL | `http://localhost:8080` | `NEXT_PUBLIC_API_URL` env var |
| CORS | Backend allows `localhost:3000` | Backend allows production domain |
| Auth cookie | SameSite=Lax (dev) | SameSite=Strict + Secure (prod) |
| Routing | Next.js dev server | Nginx proxies `/*` → Next.js :3000 |
| Build | `npm run dev` | `npm run build && npm start` |

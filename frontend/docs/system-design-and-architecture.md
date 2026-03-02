# Frontend System Design & Architecture

**Project:** Barangay Clearance System — Resident Portal & Back-Office Dashboard  
**Framework:** Next.js 14 (App Router) · TypeScript · Tailwind CSS  
**Last Updated:** 2026-02-24

---

## Table of Contents

1. [Overview](#1-overview)
2. [Technology Stack](#2-technology-stack)
3. [Project Structure](#3-project-structure)
4. [Application Layers](#4-application-layers)
5. [Routing Architecture](#5-routing-architecture)
6. [Authentication & Authorization](#6-authentication--authorization)
7. [API Communication Layer](#7-api-communication-layer)
8. [State Management](#8-state-management)
9. [Component Architecture](#9-component-architecture)
10. [Type System](#10-type-system)
11. [Data Fetching Patterns](#11-data-fetching-patterns)
12. [Form Handling](#12-form-handling)
13. [Styling System](#13-styling-system)
14. [Key Flows](#14-key-flows)
15. [Environment Configuration](#15-environment-configuration)
16. [Development Guidelines](#16-development-guidelines)

---

## 1. Overview

The frontend is a **client-rendered Next.js 14 application** that serves two distinct user-facing surfaces:

| Surface                   | Path Prefix     | Audience                                                           |
| ------------------------- | --------------- | ------------------------------------------------------------------ |
| **Resident Portal**       | `/portal/*`     | Residents filing and tracking clearance requests                   |
| **Back-Office Dashboard** | `/backoffice/*` | Barangay staff (Admin, Clerk, Approver) managing the full workflow |

Both surfaces share authentication infrastructure, a common Axios API client, and a TanStack React Query server-state layer. Business logic lives in the Spring Boot backend — the frontend is a consumer of the REST API and never owns data.

---

## 2. Technology Stack

| Concern            | Library / Tool       | Version             |
| ------------------ | -------------------- | ------------------- |
| Framework          | Next.js (App Router) | 14.x                |
| Language           | TypeScript           | 5.x                 |
| Styling            | Tailwind CSS         | 3.4.x               |
| UI Components      | shadcn/ui            | (component library) |
| HTTP Client        | Axios                | 1.x                 |
| Server-State Cache | TanStack React Query | 5.x                 |
| Forms              | React Hook Form      | 7.x                 |
| Validation         | Zod                  | 4.x                 |
| JWT Decoding       | jwt-decode           | 4.x                 |
| Auth Context       | React Context API    | Built-in            |

---

## 3. Project Structure

```
frontend/
├── src/
│   ├── app/                         # Next.js App Router — pages and layouts
│   │   ├── layout.tsx               # Root layout (html, body, Providers)
│   │   ├── page.tsx                 # Landing / redirect page
│   │   ├── providers.tsx            # QueryClient + AuthProvider tree
│   │   ├── globals.css              # Tailwind base styles
│   │   ├── login/page.tsx           # Public login page
│   │   ├── register/page.tsx        # Public self-registration page (residents)
│   │   ├── change-password/page.tsx # Forced password change on first login
│   │   ├── portal/                  # RESIDENT-only routes
│   │   │   └── dashboard/page.tsx
│   │   └── backoffice/              # Staff-only routes (ADMIN|CLERK|APPROVER)
│   │       ├── dashboard/page.tsx
│   │       └── residents/           # Resident management
│   ├── components/                  # Reusable UI components
│   │   └── backoffice/
│   │       └── ResidentTable.tsx
│   ├── context/
│   │   └── AuthContext.tsx          # Auth state + login/logout actions
│   ├── hooks/                       # TanStack React Query hooks per domain
│   │   └── useResidents.ts
│   ├── lib/
│   │   └── api.ts                   # Axios instance + token interceptors
│   ├── middleware.ts                 # Next.js Edge middleware — route guards
│   └── types/                       # TypeScript domain type definitions
│       ├── auth.ts
│       ├── clearance.ts
│       ├── common.ts
│       ├── payment.ts
│       ├── resident.ts
│       └── settings.ts
├── next.config.mjs
├── tailwind.config.ts
├── tsconfig.json
└── package.json
```

---

## 4. Application Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                        Pages (app/)                             │
│   login · register · portal/* · backoffice/*                    │
├─────────────────────────────────────────────────────────────────┤
│                     Components (components/)                    │
│   Presentational + domain-specific UI widgets                   │
├────────────────────────┬────────────────────────────────────────┤
│   Custom Hooks         │   Auth Context                         │
│   (hooks/)             │   (context/AuthContext.tsx)            │
│   TanStack Query       │   userId · role · login · logout       │
├────────────────────────┴────────────────────────────────────────┤
│                    API Client (lib/api.ts)                       │
│   Axios instance · Bearer token injection · 401→refresh→retry   │
├─────────────────────────────────────────────────────────────────┤
│                  Backend REST API (:8080)                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. Routing Architecture

### Route Groups

```
/                      → redirect based on role
/login                 → public
/register              → public (resident self-registration)
/change-password       → authenticated (forced after first login)

/portal/
  dashboard            → RESIDENT
  my-requests          → RESIDENT
  my-requests/[id]     → RESIDENT

/backoffice/
  dashboard            → ADMIN | CLERK | APPROVER
  residents            → ADMIN | CLERK
  residents/[id]       → ADMIN | CLERK
  clearances           → ADMIN | CLERK | APPROVER
  clearances/[id]      → ADMIN | CLERK | APPROVER
  reports              → ADMIN
  settings             → ADMIN
  admin/users          → ADMIN
```

### Route Guard — `middleware.ts`

The Next.js Edge Middleware runs **before every request** (except static assets) and enforces role-based access control:

```
Request arrives
       │
       ▼
Is route public (/login, /register)?
  YES  → pass through
  NO   ▼
Cookie `accessToken` present?
  NO   → redirect /login?next=<path>
  YES  ▼
Decode JWT (jwt-decode)
  exp < now?  → redirect /login
  /backoffice && role NOT in [ADMIN,CLERK,APPROVER]? → redirect /portal/dashboard
  /portal && role !== RESIDENT?                       → redirect /backoffice/dashboard
  otherwise   → pass through
```

**Token storage strategy:** Access tokens live in `localStorage` (Client-side) and are **mirrored to an `accessToken` cookie** by `AuthContext` so that the Edge Middleware can read them server-side without a round-trip.

---

## 6. Authentication & Authorization

### Token Lifecycle

```
User submits credentials
        │
        ▼
POST /api/v1/auth/login
        │
        ▼
Receive { accessToken, refreshToken, mustChangePassword }
        │
        ├─ Store accessToken  → localStorage + cookie
        ├─ Store refreshToken → localStorage
        └─ Decode JWT payload → hydrate AuthContext state
```

### JWT Payload Shape (`JwtPayload`)

| Field                | Type      | Description                                    |
| -------------------- | --------- | ---------------------------------------------- |
| `sub`                | `string`  | User UUID                                      |
| `email`              | `string`  | User email                                     |
| `role`               | `Role`    | `ADMIN` \| `CLERK` \| `APPROVER` \| `RESIDENT` |
| `mustChangePassword` | `boolean` | Forces password-change redirect                |
| `exp` / `iat`        | `number`  | Unix timestamps                                |

### `AuthContext` State

```typescript
interface AuthState {
  userId: string | null;
  email: string | null;
  role: Role | null;
  mustChangePassword: boolean;
  isAuthenticated: boolean;
  isLoading: boolean; // true while hydrating from localStorage on mount
}
```

Exposed actions: `login(data)`, `logout()`, `clearAuth()`.

### Logout Flow

1. Call `POST /api/v1/auth/logout` with `refreshToken` to invalidate server-side.
2. Remove `accessToken` and `refreshToken` from localStorage.
3. Clear `accessToken` cookie (`max-age=0`).
4. Reset `AuthContext` state to unauthenticated.

---

## 7. API Communication Layer

All REST calls go through a single Axios instance: `src/lib/api.ts`.

### Base Configuration

```
baseURL  : NEXT_PUBLIC_API_URL  (default: http://localhost:8080)
headers  : { Content-Type: application/json }
```

### Request Interceptor

Reads `accessToken` from `localStorage` and attaches it as `Authorization: Bearer <token>` on every outgoing request.

### Response Interceptor — 401 → Refresh → Retry

```
Response returns 401 AND request has not been retried yet?
    │
    ├─ A refresh is already in-flight?
    │     YES → queue this request; resolve when new token arrives
    │
    └─ NO  → start refresh
              POST /api/v1/auth/refresh { refreshToken }
              ├─ SUCCESS: store new accessToken, notify queue, retry original
              └─ FAIL:    clear tokens, redirect window to /login
```

The queuing mechanism prevents a cascade of simultaneous refresh calls when multiple concurrent requests all hit 401 at the same time — only one refresh attempt is made and all waiting requests are replayed with the new token.

---

## 8. State Management

The application uses **two complementary state layers**:

### Server State — TanStack React Query

All data fetched from the backend is managed by React Query. There is no global Redux / Zustand store for API data.

| Config      | Value                                 |
| ----------- | ------------------------------------- |
| `staleTime` | 30 000 ms (data stays fresh for 30 s) |
| `retry`     | 1 (one automatic retry on failure)    |

Query client is created once in `providers.tsx` and provided via `QueryClientProvider`.

**Query key convention** (see `useResidents.ts`):

```typescript
const residentKeys = {
  all: ["residents"],
  lists: () => [...residentKeys.all, "list"],
  list: (params) => [...residentKeys.lists(), params],
  detail: (id) => [...residentKeys.all, "detail", id],
  pending: () => [...residentKeys.all, "pending"],
};
```

This hierarchical key structure enables targeted or broad cache invalidation after mutations.

### Client State — React Context

`AuthContext` holds transient auth state only (identity, role, loading flag). It is not used for domain data.

---

## 9. Component Architecture

Components are **presentational by default** — they receive data via props and raise events via callbacks. Data fetching is the responsibility of hooks, not components.

### Directory Conventions

```
components/
├── backoffice/          # Components used only in the back-office surface
│   └── ResidentTable.tsx
├── portal/              # Components used only in the portal surface
└── shared/              # Components shared across both surfaces
    ├── StatusBadge.tsx
    ├── PageHeader.tsx
    └── ...
```

### Component Patterns

- **`'use client'` directive** — add only when the component needs browser APIs, event handlers, or hooks. Server Components are the default.
- **Props typing** — every component has an explicit `interface Props` or `type Props`.
- **Loading / empty states** — components handle all three states: loading skeleton, empty message, and populated data.

---

## 10. Type System

All domain types live in `src/types/` and are **imported by both hooks and components** — never duplicated.

| File           | Contents                                                                                 |
| -------------- | ---------------------------------------------------------------------------------------- |
| `auth.ts`      | `Role`, `User`, `TokenResponse`, `LoginRequest`, `JwtPayload`                            |
| `resident.ts`  | `Resident`, `CreateResidentPayload`, `UpdateResidentPayload`, `Gender`, `ResidentStatus` |
| `clearance.ts` | `ClearanceRequest`, `ClearanceStatus`, `PaymentStatus`, `Urgency`                        |
| `payment.ts`   | `Payment`                                                                                |
| `settings.ts`  | `BarangaySettings`, `FeeConfig`                                                          |
| `common.ts`    | `PageResponse<T>`, `ErrorResponse`                                                       |

### `PageResponse<T>`

All paginated API responses conform to:

```typescript
interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
```

### `ErrorResponse`

Backend errors are normalized to:

```typescript
interface ErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp: string;
  path: string;
  details?: Record<string, string>;
}
```

---

## 11. Data Fetching Patterns

### Query Hooks (reads)

```typescript
// Paginated list
const { data, isLoading, isError } = useResidents({ q, page, size });

// Single entity
const { data: resident } = useResident(id);
```

### Mutation Hooks (writes)

```typescript
const createResident = useCreateResident();

// In form onSubmit:
createResident.mutate(payload, {
  onSuccess: () => router.push("/backoffice/residents"),
  onError: (err) => setError(err.message),
});
```

Mutations call `queryClient.invalidateQueries()` on success to keep list views in sync without manual refetching.

### Error Handling

Axios throws on non-2xx responses. React Query surfaces the error through `isError` and `error` fields. Components display user-friendly messages; raw `ErrorResponse.details` fields are mapped to form field errors when available.

---

## 12. Form Handling

All user-input forms use **React Hook Form + Zod**:

1. Define a Zod schema — this is the **single source of validation truth**.
2. Derive the TypeScript type from the schema via `z.infer<typeof schema>`.
3. Pass the schema to `useForm` via `zodResolver`.
4. Map `ErrorResponse.details` from the API back to field-level errors using `form.setError`.

```typescript
const schema = z.object({
  email: z.string().email(),
  password: z.string().min(8),
});
type FormData = z.infer<typeof schema>;

const form = useForm<FormData>({ resolver: zodResolver(schema) });
```

---

## 13. Styling System

- **Tailwind CSS** utility classes are the primary styling mechanism.
- **shadcn/ui** provides accessible, unstyled component primitives (Button, Input, Dialog, etc.) that are styled with Tailwind.
- No CSS Modules or styled-components are used.
- Global styles (fonts, CSS variables, base resets) are defined in `app/globals.css`.
- `tailwind.config.ts` is used for theme extensions (colors, spacing, etc.).

---

## 14. Key Flows

### Login Flow

```
/login form submit
      │
      ▼
api.post /api/v1/auth/login
      │
      ├─ 200 → store tokens → decode JWT
      │         ├─ mustChangePassword=true → /change-password
      │         ├─ role=RESIDENT           → /portal/dashboard
      │         └─ staff role              → /backoffice/dashboard
      │
      └─ 4xx → display error message
```

### Staff — Resident Management Flow

```
/backoffice/residents (ResidentTable)
      │  uses useResidents({ q, page, size })
      │
      ├─ Search input → debounced query param → cache refetch
      │
      ├─ Row click → /backoffice/residents/[id]
      │               uses useResident(id)
      │
      └─ "Add Resident" → modal or /backoffice/residents/new
                          uses useCreateResident()
                          on success → invalidates residents list cache
```

### Clearance Request Flow (Resident)

```
/portal/my-requests
      │  lists resident's own clearance requests
      │
      └─ "New Request" → form (purpose, urgency)
                         POST /api/v1/clearance/requests
                         → status: DRAFT
                         → staff picks up in back-office
```

---

## 15. Environment Configuration

| Variable              | Description          | Default                 |
| --------------------- | -------------------- | ----------------------- |
| `NEXT_PUBLIC_API_URL` | Backend API base URL | `http://localhost:8080` |

The `NEXT_PUBLIC_` prefix makes the variable available in client-side code (bundled into JS). Never put secrets in `NEXT_PUBLIC_*` variables.

For production, set `NEXT_PUBLIC_API_URL` to the Nginx reverse-proxy URL (e.g., `https://barangay.example.gov.ph`).

---

## 16. Development Guidelines

### Adding a New Feature

1. **Define types** in `src/types/<domain>.ts`.
2. **Create a hook** in `src/hooks/use<Domain>.ts` using TanStack Query.
3. **Build components** in `src/components/<surface>/`.
4. **Add the page** in `src/app/<surface>/<feature>/page.tsx`.
5. **Update middleware** matcher if a new protected route is added.

### Rules

- Always use the `api` Axios instance — never `fetch` directly.
- Never expose raw API types directly in component props; wrap with domain types.
- Keep query keys in a `<domain>Keys` object in the hook file.
- Colocate form Zod schemas with the form component that uses them.
- Mark components as `'use client'` only when necessary.
- `AuthContext` is for auth state only — do not store domain data there.
- All forms must handle both client-side Zod validation and server-side `ErrorResponse.details` field errors.

### Running Locally

```bash
cd frontend
npm run dev        # Dev server → http://localhost:3000
npm run build      # Production build
npm run lint       # ESLint check
```

Backend must be running at `http://localhost:8080` (or set `NEXT_PUBLIC_API_URL`).

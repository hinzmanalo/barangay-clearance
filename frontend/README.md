# Barangay Clearance System — Frontend

Next.js 14 resident portal and backoffice dashboard for the Barangay Clearance System. Residents submit and track clearance requests via the portal; barangay staff manage, approve, and release clearances through the backoffice.

## Tech Stack

| Technology      | Version | Purpose                      |
| --------------- | ------- | ---------------------------- |
| Next.js         | 14      | React framework (App Router) |
| TypeScript      | 5.x     | Type safety                  |
| Tailwind CSS    | 3.x     | Utility-first styling        |
| shadcn/ui       | —       | UI component library         |
| React Hook Form | 7.x     | Form state management        |
| Zod             | 3.x     | Schema validation            |
| TanStack Query  | 5.x     | Server state / data fetching |
| Axios           | 1.x     | HTTP client                  |
| jwt-decode      | 4.x     | Client-side JWT decoding     |
| sonner          | 1.x     | Toast notifications          |

---

## Prerequisites

- **Node.js 20+** — `node -v`
- **npm** — included with Node.js
- **Backend running** — the API must be reachable at `http://localhost:8080` (or the URL configured in `NEXT_PUBLIC_API_URL`)

---

## Getting Started

### 1. Install dependencies

```bash
npm install
```

### 2. Configure environment

Create a `.env.local` file in the `frontend/` directory:

```
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### 3. Start the development server

```bash
npm run dev
```

The app starts on **http://localhost:3000**.

---

## Common Commands

```bash
npm run dev      # Start dev server (port 3000, hot reload)
npm run build    # Production build
npm run start    # Serve production build locally
npm run lint     # ESLint check
```

---

## Application Routes

### Public

| Path        | Description                               |
| ----------- | ----------------------------------------- |
| `/login`    | Login — role-aware redirect after success |
| `/register` | Resident self-registration                |

### Resident Portal (`/portal/**` — requires `RESIDENT` role)

| Path                    | Description                                   |
| ----------------------- | --------------------------------------------- |
| `/portal/dashboard`     | Active requests + recent history              |
| `/portal/requests`      | Full request history with status              |
| `/portal/requests/new`  | Submit a new clearance request                |
| `/portal/requests/[id]` | Request detail, status timeline, PDF download |

### Backoffice (`/backoffice/**` — requires `ADMIN`, `CLERK`, or `APPROVER` role)

| Path                              | Description                                               |
| --------------------------------- | --------------------------------------------------------- |
| `/backoffice/dashboard`           | Summary cards (pending, awaiting payment, released today) |
| `/backoffice/clearances`          | Clearance list with filters                               |
| `/backoffice/clearances/new`      | Walk-in request creation                                  |
| `/backoffice/clearances/[id]`     | Clearance detail + approve/reject/release actions         |
| `/backoffice/residents`           | Resident directory with search                            |
| `/backoffice/residents/new`       | Register a walk-in resident                               |
| `/backoffice/residents/[id]`      | Resident detail/edit + portal activation                  |
| `/backoffice/reports`             | Filtered clearance reports                                |
| `/backoffice/admin/settings`      | Barangay profile + logo upload                            |
| `/backoffice/admin/settings/fees` | Fee configuration                                         |

### Special

| Path               | Description                                                          |
| ------------------ | -------------------------------------------------------------------- |
| `/change-password` | Forced password change (redirected when `mustChangePassword` is set) |

---

## Project Structure

```
src/
├── app/                         # Next.js App Router pages
│   ├── login/                   # Public auth pages
│   ├── register/
│   ├── change-password/         # Forced password change
│   ├── portal/                  # Resident portal (RESIDENT role)
│   │   ├── dashboard/
│   │   └── requests/
│   └── backoffice/              # Staff backoffice (ADMIN, CLERK, APPROVER)
│       ├── dashboard/
│       ├── clearances/
│       ├── residents/
│       ├── reports/
│       └── admin/
│           └── settings/
│
├── components/
│   ├── portal/                  # Portal-specific UI (StatusTimeline, RequestCard)
│   ├── backoffice/              # Backoffice UI (ClearanceTable, ActionButtons, ResidentTable)
│   └── shared/                  # StatusBadge, PaymentBadge, PageHeader, LoadingSkeleton, ErrorToast
│
├── context/
│   └── AuthContext.tsx          # Auth state — useAuth() hook, localStorage persistence
│
├── hooks/                       # TanStack React Query data-fetching hooks
│   ├── useClearances.ts
│   ├── useResidents.ts
│   ├── useReports.ts
│   └── useSettings.ts
│
├── lib/
│   └── api.ts                   # Axios instance — Bearer token injection + 401→refresh→retry
│
├── types/                       # TypeScript domain interfaces
│   ├── auth.ts
│   ├── clearance.ts
│   ├── resident.ts
│   ├── payment.ts
│   ├── settings.ts
│   └── common.ts
│
└── middleware.ts                 # Edge route guard — role-based access + auth redirect
```

---

## Key Patterns

### Auth State

`useAuth()` from `AuthContext.tsx` provides `user`, `login`, `logout`, and `clearAuth`. Tokens are stored in `localStorage`; the access token is also mirrored to an `accessToken` cookie so `middleware.ts` (which runs at the Edge before the React tree renders) can enforce role-based route guards without a network round-trip.

### API Client

All API calls go through the Axios instance in `src/lib/api.ts`. It:

1. Attaches `Authorization: Bearer <accessToken>` to every outbound request.
2. On `401`, queues all concurrent failing requests, performs exactly **one** silent token refresh, then replays all queued requests with the new token — no duplicate refresh calls.
3. If the refresh itself fails (e.g., refresh token expired), calls `clearAuth()` and redirects to `/login`.

### Data Fetching

TanStack React Query hooks live in `src/hooks/`. Each domain uses a hierarchical key factory so mutations can invalidate exactly the right cache entries without over-clearing. For example, approving a clearance invalidates the detail query and the list, but not unrelated resident queries.

### Forms

React Hook Form + Zod for all user input. The Zod schema is the single source of validation truth — it drives both client-side field errors and the TypeScript `FormData` type via `z.infer<typeof schema>`.

### Toasts

`toast.success(message)` / `toast.error(message)` from `@/components/shared/ErrorToast` (a thin re-export of `sonner`). The `<Toaster>` is mounted once in `providers.tsx`.

### Loading States

`TableRowSkeleton`, `DetailPageSkeleton`, and `CardSkeleton` from `@/components/shared/LoadingSkeleton` are used on all list and detail pages to prevent layout shift while data loads.

---

## Architecture Reference

For a full technical deep-dive into routing, auth flow, API client interceptors, state management rationale, query key conventions, and extension guidelines, see [docs/system-design-and-architecture.md](docs/system-design-and-architecture.md).

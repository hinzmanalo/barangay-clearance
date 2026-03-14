# PRD: Barangay Clearance System — UI/UX Redesign

- **Version**: 1.0
- **Last Updated**: March 5, 2026
- **Author**: Engineering Team
- **Status**: Approved for Implementation

---

## 1. Product Overview

### 1.1 Summary

The current Barangay Clearance System frontend is functional but visually sparse — built with raw Tailwind utilities, Arial fonts, and no animation library. It serves real Filipino residents and LGU staff daily. This PRD specifies a comprehensive UI/UX overhaul to elevate trust, usability, and delight while preserving the existing architecture (Next.js 14, TanStack Query, React Hook Form + Zod, Axios).

The redesign targets:

- **Public surfaces**: Login, Register, Change Password
- **Resident Portal**: Layout, Dashboard, New Request, Request Detail
- **Backoffice**: Layout shell (sidebar + topbar), Dashboard, Clearance List/Detail/New, Resident List/Detail/New, Reports, Admin (Users, Settings, Fee Config, Audit Logs)

The aesthetic direction is **Modern Gov-Tech**: clean, trustworthy, and professional — evoking PH government digital services (PhilSys, eGovPH) while feeling clearly more refined. Primary palette is deep government blue + teal accent on a cool off-white base. Typography pairs **Sora** (display/headings) with **Geist** (body/UI). Rich, intentional motion via Framer Motion rewards users without distracting from task completion.

### 1.2 Current State

| Dimension          | Current State                              | Target State                                                                     |
| ------------------ | ------------------------------------------ | -------------------------------------------------------------------------------- |
| Typography         | Arial (system fallback)                    | Sora (headings) + Geist (body) via `next/font`                                   |
| Color system       | Ad-hoc Tailwind classes                    | CSS custom-property token system                                                 |
| Animation          | `animate-pulse` + `transition-shadow` only | Framer Motion — page transitions, staggered lists, animated counters             |
| Components         | Hand-built raw Tailwind                    | Design-system primitives (`Button`, `Card`, `Badge`, `Input`, `Table`, `Dialog`) |
| Backoffice sidebar | `bg-gray-900` static                       | `bg-[#0A1E3D]` with gradient tint + icon-only collapse                           |
| Portal sidebar     | `bg-blue-900` static                       | `bg-[#0D5FA6]` with motion-aware active indicator                                |
| Backgrounds        | `bg-gray-50` flat                          | Layered: subtle mesh gradient header + `#F8FAFC` page fill                       |
| Cards              | `bg-white border-gray-200 shadow-sm`       | Refined with hover lift, `shadow-md` target, optional colored left-border accent |
| Forms              | Bare inputs with `focus:ring-blue-500`     | Floating-label inputs with animated focus state                                  |
| Loading states     | `animate-pulse` skeleton                   | Staggered skeleton fade-in via Framer Motion                                     |
| Empty states       | Plain text                                 | Illustrated / icon-centered with a CTA                                           |

---

## 2. Goals

### 2.1 Business Goals

- Project credibility and institutional trust: the system should visually convey official government use
- Reduce resident confusion by making status flows visually intuitive with clear progression indicators
- Decrease staff task-switching friction via a polished, consistent backoffice layout

### 2.2 User Goals

- Residents: feel confident their request is being processed; clearly understand current status and next step
- Staff (Clerk/Approver): scan large data tables quickly; take actions without disorientation
- Admin: navigate the system hierarchy (users → settings → fees → audit) without cognitive load

### 2.3 Non-Goals

- This PRD does not change any API contracts, business logic, or backend code
- This PRD does not introduce new features (no new pages or routes are added)
- This PRD does not require a full shadcn/ui installation — the design system will be purpose-built to minimize bundle risk

---

## 3. Design System Specification

### 3.1 Typography

Install via `next/font/google`:

```ts
// app/layout.tsx
import { Sora, Geist } from "next/font/google";

const sora = Sora({
  subsets: ["latin"],
  variable: "--font-sora",
  weight: ["400", "500", "600", "700", "800"],
});

const geist = Geist({
  subsets: ["latin"],
  variable: "--font-geist",
  weight: ["400", "500", "600"],
});
```

| Role                          | Font                 | Weight  | Usage                                         |
| ----------------------------- | -------------------- | ------- | --------------------------------------------- |
| Display / Hero headings       | Sora                 | 700–800 | Login page title, page `h1`                   |
| Section headings              | Sora                 | 600     | `PageHeader`, card headings, sidebar app name |
| Body / UI copy                | Geist                | 400–500 | Table text, form labels, descriptions         |
| Monospace (clearance numbers) | `font-mono` (system) | 500     | Clearance number display                      |

Tailwind config additions:

```ts
fontFamily: {
  sora: ['var(--font-sora)', 'sans-serif'],
  geist: ['var(--font-geist)', 'sans-serif'],
}
```

### 3.2 Color Token System

Define in `globals.css` as CSS custom properties:

```css
:root {
  /* Brand */
  --clr-primary-900: #062040;
  --clr-primary-800: #0a3060;
  --clr-primary-700: #0a4f8f; /* Primary action, sidebar bg-backoffice */
  --clr-primary-600: #1565c0;
  --clr-primary-500: #1976d2;
  --clr-primary-100: #e3f0ff;

  /* Teal accent */
  --clr-teal-600: #0d7a70;
  --clr-teal-500: #0d9488; /* Portal sidebar bg, accent CTA */
  --clr-teal-400: #14b8a6;
  --clr-teal-100: #ccfbf1;

  /* Neutral */
  --clr-neutral-950: #0c1117;
  --clr-neutral-900: #111827;
  --clr-neutral-700: #374151;
  --clr-neutral-500: #6b7280;
  --clr-neutral-300: #d1d5db;
  --clr-neutral-100: #f3f4f6;
  --clr-neutral-50: #f8fafc; /* Page backgrounds */

  /* Status */
  --clr-status-draft: #6b7280;
  --clr-status-for-approval: #d97706;
  --clr-status-approved: #059669;
  --clr-status-rejected: #dc2626;
  --clr-status-released: #2563eb;

  /* Payment */
  --clr-pay-unpaid: #ea580c;
  --clr-pay-paid: #16a34a;
  --clr-pay-waived: #7c3aed;

  /* Surface */
  --clr-surface: #ffffff;
  --clr-surface-alt: #f8fafc;
  --clr-border: #e2e8f0;

  /* Shadow */
  --shadow-sm: 0 1px 3px 0 rgb(0 0 0 / 0.08), 0 1px 2px -1px rgb(0 0 0 / 0.08);
  --shadow-md: 0 4px 12px 0 rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.08);
  --shadow-lg: 0 8px 24px -4px rgb(0 0 0 / 0.12);
}
```

Tailwind config: map all tokens via `extend.colors` using `hsl(var(...))` or direct CSS variable references.

### 3.3 Spacing & Layout

- Page content max-width: `max-w-7xl mx-auto px-6`
- Card padding: `p-6` (desktop), `p-4` (mobile)
- Section vertical rhythm: `space-y-6` between major sections
- Sidebar width: `w-64` expanded, `w-16` collapsed (icon-only)
- Topbar height: `h-16`

### 3.4 Component Primitives

Build these shared/reusable primitives (all in `components/ui/`):

| Primitive    | Description                                                                                           | Key Design                                                                       |
| ------------ | ----------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| `Button`     | variants: `primary`, `secondary`, `ghost`, `danger`, `outline`; sizes: `sm`, `md`, `lg`               | Rounded `rounded-lg`; `hover:scale-[1.02] active:scale-[0.98]` via Framer Motion |
| `Card`       | wrapper with `bg-white rounded-2xl shadow-[--shadow-sm] hover:shadow-[--shadow-md] transition-shadow` | Optional `accentColor` prop adds 4px left-border                                 |
| `Badge`      | Status/Payment/Role variants, replaces current `StatusBadge`, `PaymentBadge`, `RoleBadge`             | Rounded-full, color from token map, optional dot indicator                       |
| `Input`      | Floating-label animated input; error state with red underline                                         | Framer Motion `AnimatePresence` for error message slide-in                       |
| `Select`     | Styled native select matching Input                                                                   | Consistent `h-11` height                                                         |
| `Textarea`   | Resizable, matching Input styling                                                                     | Auto-grow option                                                                 |
| `Skeleton`   | Replaces `LoadingSkeleton.tsx`                                                                        | Framer Motion `opacity` stagger instead of `animate-pulse`                       |
| `PageHeader` | Sora bold `h1` + subtitle + actions slot                                                              | Optional back-button with animated arrow                                         |
| `StatCard`   | Animated counter + icon + label + delta                                                               | `useMotionValue` + `animate` from Framer Motion                                  |
| `EmptyState` | Icon (lucide) + heading + description + optional CTA button                                           | Framer Motion fade-in                                                            |
| `DataTable`  | Reusable table wrapper with `thead`/`tbody` and `TableRowSkeleton`                                    | Staggered row entrance animation                                                 |

### 3.5 Animation Library

**Add `framer-motion` v11** to `frontend/package.json`.

#### Core Animation Patterns

```
1. PAGE TRANSITION (layout-level)
   - Wrap page content in <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3, ease: 'easeOut' }} />
   - Applied in all layout.tsx files

2. STAGGERED LIST
   - Parent: <motion.ul variants={containerVariants} initial="hidden" animate="visible">
   - Child: <motion.li variants={itemVariants}> where itemVariant has delayChildren + staggerChildren
   - Used in: ClearanceTable rows, ResidentTable rows, RequestCard list, dashboard nav cards

3. ANIMATED STAT COUNTER
   - useMotionValue(0) → animate to target number → display with useTransform
   - Used on: backoffice dashboard stat cards (pending count, approved count, released count)

4. PRESENCE ANIMATION
   - <AnimatePresence mode="wait"> for inline confirm dialogs, filter panel slide-down, reject textarea expand
   - mount: { opacity: 1, height: 'auto' } | unmount: { opacity: 0, height: 0 }

5. SIDEBAR COLLAPSE
   - <motion.aside> with animate={{ width: isCollapsed ? 64 : 256 }} transition={{ type: 'spring', stiffness: 300, damping: 30 }}
   - AnimatePresence for label text (fade out on collapse)

6. MODAL/SHEET
   - backdrop: fade-in
   - panel: slide-in from bottom (mobile) or right (desktop)

7. SKELETON STAGGER
   - Skeleton rows animate in with stagger delay (0.05s per row)
   - Fade from opacity 0.4 → 0.8 → 0.4 (custom keyframe, not CSS pulse)
```

---

## 4. Page-by-Page Design Spec

### 4.1 Public Pages

#### Login (`/login`)

**Layout**: Two-column split screen on desktop (left: graphic panel, right: form). Single column on mobile.

- **Left panel** (`hidden md:block w-1/2`): deep blue gradient `bg-gradient-to-br from-[#062040] to-[#0A4F8F]`; centered content block with:
  - Barangay seal / logo (placeholder `Shield` lucide icon until logo uploaded)
  - App name in Sora 36px bold white
  - Tagline: _"Ang serbisyo ng barangay, nasa dulo ng iyong daliri"_ (or English variant) in Sora 16px, `text-blue-200`
  - Decorative: subtle SVG map outline of Philippines at bottom, `opacity-10`
- **Right panel**: centered form on `bg-[#F8FAFC]`
  - Card with `rounded-2xl shadow-lg p-8`
  - Heading "Welcome back" in Sora 28px bold
  - Subheading "Sign in to your account" in Geist text-neutral-500
  - Floating-label `Input` for Email
  - Floating-label `Input` for Password (with toggle visibility)
  - `Button` variant=primary full-width "Sign In" with loading spinner (Framer Motion `rotate` keyframe on `Loader2` icon)
  - Register link: "New resident? Register here"
- **Entrance animation**: form card slides up from `y: 20` + fades in on mount

#### Register (`/register`)

**Layout**: Centered single column, max-width `md:max-w-2xl`

- Top: same sealed header bar (app name + seal, `bg-[#0A4F8F]` strip)
- Progress indicator: 2-step horizontal stepper (Step 1: Account Info, Step 2: Personal Info)
- **Step 1**: Email + Password + Confirm Password — Framer Motion `AnimatePresence` slide-in
- **Step 2**: First/Middle/Last Name, Birth Date (date picker), Gender (styled select), Address fields (street, purok, city)
- Navigation: "Back" ghost button + "Next" / "Submit" primary button
- Step transition: current step slides out left, new step slides in from right

#### Change Password (`/change-password`)

- Minimal centered card on `bg-[#F8FAFC]`
- Warning banner: amber icon + "For security, please set a new password before continuing"
- Three `Input`s: Current, New, Confirm
- Same animated entrance as login

---

### 4.2 Backoffice Layout Shell

#### Sidebar (`components/backoffice/Sidebar.tsx`)

- Background: `bg-[#0A1E3D]` (deepest navy) with subtle `bg-gradient-to-b from-[#0A1E3D] to-[#062040]`
- Width: 256px expanded → 64px collapsed (Framer Motion spring)
- Top: App logo/crest + "Barangay Clearance" text (fades out on collapse)
- Nav items:
  - Icon (lucide, 20px) always visible
  - Label (Geist 14px medium) — AnimatePresence fade
  - Active state: `bg-white/10 text-white rounded-lg` with animated left accent bar (4px wide, teal, Framer Motion `layoutId="activeIndicator"`)
  - Hover: `bg-white/5`
- Admin sub-section: collapsible group header with `ChevronDown` rotation animation
- Bottom: User info chip (email truncated, role badge) + logout button
- Mobile: overlay sheet (Framer Motion `x: -256 → 0`)

#### Topbar

- `h-16 bg-white border-b border-[--clr-border] shadow-[--shadow-sm]`
- Left: hamburger (mobile) + breadcrumb trail in Geist
- Right: notification bell (future), user email + role badge, logout
- On scroll: `backdrop-blur-sm` + slight shadow intensification (CSS `transition`)

#### Page transition wrapper

In `backoffice/layout.tsx`, wrap `{children}` in a Framer Motion `motion.div` for page transitions.

---

### 4.3 Backoffice Dashboard (`/backoffice/dashboard`)

- **Stat cards row** (4-up grid): Pending, Approved, Released, Total — each a `StatCard` primitive:
  - Animated counter from 0 → value on mount (Framer Motion `animate`)
  - Colored icon circle (teal for approved, amber for pending, blue for released)
  - Delta text (e.g., "+3 today") in `text-sm text-neutral-500`
  - Stagger entrance: cards appear with 0.1s delay between each
- **Quick action cards** (grid): "New Walk-in Request", "View All Clearances", "Manage Residents" — large `Card` components with hover lift + icon
- **Recent clearances mini-table**: last 5 records, right-aligned "View all →" link
- Remove 30s polling indicator UX awkwardness — keep the poll, but show a subtle "Last updated X seconds ago" caption

---

### 4.4 Clearances List (`/backoffice/clearances`)

- Replace 4-card summary header with a slim horizontal stat strip (borderless, colored numbers inline)
- Filters bar: horizontal flex row with styled `Select` dropdowns + primary "Search" button, all in a `Card` with `p-4`
- `ClearanceTable` redesign:
  - `thead` with `bg-neutral-50 text-neutral-500 text-xs uppercase tracking-wide`
  - Rows: staggered entrance animation (Framer Motion); hover `bg-neutral-50`
  - `Badge` components for status + payment (replace current `<span>` pills)
  - Clearance number: monospace, `text-primary-700`
- Pagination: centered, with `←` `→` buttons + "Page X of Y" in Geist sm
- Loading: `DataTable` skeleton with stagger

#### Clearance Detail (`/backoffice/clearances/[id]`)

- Two-column layout on `lg:` breakpoint: left `w-2/3` (details) + right `w-1/3` (action panel)
- `StatusTimeline` redesign:
  - Vertical timeline (always vertical, not conditional)
  - Each step: circle dot with icon + label + timestamp; connecting line animated via Framer Motion path draw (`pathLength: 0 → 1`)
  - Animated: completed steps' connector lines draw in on mount
- Action panel: `Card` with `ActionButtons`; reject reason uses `AnimatePresence` slide-down for the textarea
- PDF download button: outlined with `Download` icon; Framer Motion scale on click

---

### 4.5 Residents List & Detail

Same pattern as Clearances. Key additions:

- **Pending activations panel**: Replace current yellow `alert` box with a styled `Card` with amber left-border accent, clearer call-to-action buttons with icons (`UserCheck`, `UserX`)
- Resident detail: two-column card layout with personal info grid + portal status section

---

### 4.6 Portal Layout Shell

#### Sidebar (`components/portal/Sidebar.tsx`)

- Background: `bg-gradient-to-b from-[#0D5FA6] to-[#0A4F8F]`
- Same collapse mechanics as backoffice sidebar
- Active indicator: white `layoutId` pill
- Bottom: resident name + "Resident" chip

---

### 4.7 Portal Dashboard (`/portal/dashboard`)

- **Welcome banner**: `Card` with teal left-border, "Welcome, {firstName}" in Sora 22px + "Track your clearance requests below" in Geist
- **New Request CTA button**: prominent, full-width on mobile, right-aligned on desktop, with `FilePlus` icon
- **Request cards** (`RequestCard`): redesign to a richer card:
  - Top: clearance number (mono) + `Badge` for status + date
  - Middle: purpose label
  - Bottom: payment status `Badge` + "View Details →" link
  - Staggered entrance: each card `y: 20 → 0, opacity: 0 → 1` with 0.1s stagger
- **Empty state**: `EmptyState` component with `FileText` icon + "You have no clearance requests yet" + "Submit your first request" button

---

### 4.8 Portal Request Detail (`/portal/requests/[id]`)

- **Redesigned `StatusTimeline`**: full Framer Motion path-draw animation on progress connectors; rejected step highlighted in red with `XCircle` icon
- Payment info block: a teal-tinted `Card` when payment required
- PDF download: green `Button` variant with `Download` icon — only shown when `RELEASED`

---

### 4.9 Admin Pages

#### User Management, Settings, Fee Config

- Forms: floating-label `Input`s replacing all bare `<input>` tags
- Settings page: barangay logo upload area redesigned as a dashed-border drop zone with `Upload` icon + preview
- Fee config: side-by-side standard/rush fee cards with teal accent

#### Audit Logs (`/backoffice/admin/audit-logs`)

- Filter bar: collapsible advanced filter panel (AnimatePresence slide-down)
- Table: `AuditLogTable` with monospace entity IDs, colored action `Badge`s (CREATE=green, UPDATE=amber, DELETE=red, LOGIN=blue)

---

### 4.10 Reports (`/backoffice/reports`)

- Filter section: horizontal row with date pickers + status/payment `Select` + "Generate Report" `Button`
- Results table header: subtle animated counter showing total records matching filter
- Export action: outlined "Download CSV" button in topbar-right of the results card

---

## 5. New Dependencies

| Package                | Version                 | Reason                                                                |
| ---------------------- | ----------------------- | --------------------------------------------------------------------- |
| `framer-motion`        | ^11.x                   | Page transitions, staggered lists, animated counters, AnimatePresence |
| `next/font` (built-in) | bundled with Next.js 14 | Sora + Geist font loading; no new package needed                      |

Optional (evaluate during implementation):

| Package                    | Reason                                                     |
| -------------------------- | ---------------------------------------------------------- |
| `clsx` + `tailwind-merge`  | Cleaner conditional class merging for component primitives |
| `class-variance-authority` | Variant-driven `Button`, `Badge`, `Card` primitives        |

No shadcn/ui installation required — primitives are purpose-built.

---

## 6. File Change Surface

| Area                  | Files Affected                                                                                                                                                                      |
| --------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Design tokens         | `globals.css`, `tailwind.config.ts`                                                                                                                                                 |
| Layout fonts          | `app/layout.tsx`                                                                                                                                                                    |
| UI primitives (new)   | `components/ui/Button.tsx`, `Card.tsx`, `Badge.tsx`, `Input.tsx`, `Select.tsx`, `Textarea.tsx`, `Skeleton.tsx`, `StatCard.tsx`, `EmptyState.tsx`, `DataTable.tsx`, `PageHeader.tsx` |
| Shared components     | `StatusBadge.tsx`, `PaymentBadge.tsx`, `RoleBadge.tsx`, `LoadingSkeleton.tsx`, `PageHeader.tsx`                                                                                     |
| Backoffice components | `Sidebar.tsx`, `ClearanceTable.tsx`, `ResidentTable.tsx`, `UserTable.tsx`, `AuditLogTable.tsx`, `ActionButtons.tsx`                                                                 |
| Portal components     | `Sidebar.tsx`, `RequestCard.tsx`, `StatusTimeline.tsx`                                                                                                                              |
| All page files        | `login/page.tsx`, `register/page.tsx`, `change-password/page.tsx`, all `portal/**/page.tsx`, all `backoffice/**/page.tsx`                                                           |
| Layout files          | `backoffice/layout.tsx`, `portal/layout.tsx`                                                                                                                                        |

---

## 7. Implementation Phases

### Phase A — Foundation (Design System + Layout Shells)

1. Add `framer-motion` to `package.json`; install
2. Update `globals.css` with full CSS token system
3. Update `tailwind.config.ts` — font families, extend colors, extend shadows
4. Update `app/layout.tsx` — integrate `next/font` Sora + Geist
5. Build `components/ui/` primitives: `Button`, `Card`, `Badge`, `Input`, `Select`, `Textarea`, `Skeleton`, `PageHeader`, `EmptyState`, `StatCard`, `DataTable`
6. Redesign `backoffice/layout.tsx` — sidebar (animated collapse, `layoutId` active indicator, spring width), topbar, page-transition wrapper
7. Redesign `portal/layout.tsx` — same sidebar pattern with teal palette

### Phase B — Public Pages

8. Login page — split-screen layout, floating-label form, entrance animation
9. Register page — 2-step stepper with slide transitions
10. Change Password page

### Phase C — Backoffice Pages

11. Dashboard — `StatCard` with animated counters, staggered quick action cards
12. Clearances list — filter bar, `DataTable` with staggered rows, redesigned `Badge`s
13. Clearance detail — two-column layout, animated `StatusTimeline`, `AnimatePresence` for reject textarea
14. Residents list — pending activations `Card` redesign, staggered table
15. Resident detail/new forms
16. Admin: Users list/detail/new
17. Admin: Settings + Fee Config
18. Admin: Audit Logs with collapsible filter
19. Reports

### Phase D — Portal Pages

20. Portal dashboard — welcome banner, staggered `RequestCard` list, empty state
21. New request form
22. Request detail — animated `StatusTimeline`

### Phase E — Polish & QA

23. Verify responsive behavior at `sm`, `md`, `lg`, `xl` breakpoints
24. Test keyboard accessibility (focus rings, tab order) for all new components
25. Verify Framer Motion `reducedMotion` support (wrap with `useReducedMotion()` — skip animations if true)
26. Run `npm run build` to confirm no type errors
27. Run `npm run lint` — fix all ESLint issues from new files

---

## 8. Non-Functional Requirements

- **Performance**: No page should exceed LCP of 2.5s on localhost. Framer Motion is tree-shakeable; only import used APIs
- **Accessibility**: All interactive elements must have visible focus states; `Badge` components include `aria-label`; `StatusTimeline` steps include `aria-current`
- **Responsive**: All pages must be usable on 375px (mobile), 768px (tablet), 1280px+ (desktop)
- **Reduced motion**: All Framer Motion animations must respect `prefers-reduced-motion` via `useReducedMotion()` hook
- **Hydration**: No SSR/client hydration mismatches — all animations must use `useEffect` / client-only patterns or `AnimatePresence`

---

## 9. Definition of Done

- [ ] All pages render without console errors
- [ ] Design tokens applied globally — no hardcoded hex values in component files
- [ ] Sora + Geist fonts load and render correctly (no FOUT)
- [ ] Framer Motion page transitions work on navigation between all routes
- [ ] Backoffice sidebar collapse/expand animates smoothly with spring physics
- [ ] Portal sidebar collapse/expand same
- [ ] Dashboard stat counters animate from 0 on first mount
- [ ] Staggered list animations play on Clearances, Residents, Request Cards, Audit Logs
- [ ] `StatusTimeline` path-draw animation plays in clearance and request detail pages
- [ ] `AnimatePresence` inline reject textarea expands/collapses smoothly
- [ ] All form inputs use floating-label pattern
- [ ] All status/payment/role badges use `Badge` primitive with token-based colors
- [ ] Empty states render on all list pages when no data
- [ ] `useReducedMotion()` disables all decorative animations
- [ ] All pages pass `npm run lint` and `npm run build`
- [ ] Responsive layout tested at 375px, 768px, 1280px breakpoints

---

## 10. Success Metrics

| Metric                                      | Baseline                | Target              |
| ------------------------------------------- | ----------------------- | ------------------- |
| Lighthouse Performance (localhost)          | N/A                     | ≥ 85                |
| Lighthouse Accessibility                    | N/A                     | ≥ 90                |
| `npm run build` warnings                    | N/A                     | 0 TypeScript errors |
| Design consistency score (internal review)  | 3/10                    | 9/10                |
| Time-on-task for clearance approval (staff) | Baseline to be measured | 10% reduction       |

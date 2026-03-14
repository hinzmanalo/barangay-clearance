# Phase 13-1: Design System & Layout Shells

- **Part of:** Phase 13 — UI/UX Redesign
- **Status:** 🔴 Not Started
- **Depends on:** Phase 8 (Frontend Polish & Navigation) — all pages must already exist
- **Blocks:** Phase 13-2, 13-3, 13-4 (all must wait for this foundation)
- **Parallel with:** Nothing — this is the critical path entry point for Phase 13

---

## Goal

Establish the entire design system foundation: CSS token system, typography (Sora + Geist via `next/font`), Tailwind config extensions, Framer Motion installation, and all `components/ui/` primitives. Redesign both layout shells (backoffice + portal) with animated sidebars and page-transition wrappers.

All subsequent phases (13-2 through 13-4) import from this layer — nothing should be built before this is complete.

---

## Dependencies

```
Phase 8 (complete) ──► Phase 13-1 ──► Phase 13-2 (public pages)
                                   ──► Phase 13-3 (backoffice pages)
                                   ──► Phase 13-4 (portal pages)
```

---

## Deliverables

### 1. Install `framer-motion`

```bash
cd frontend && npm install framer-motion@^11
```

Install optional helpers:

```bash
npm install clsx tailwind-merge class-variance-authority
```

Verify `package.json` has all three new deps.

---

### 2. CSS Token System (`globals.css`)

Replace the current `globals.css` variable block with the full token set:

```css
:root {
  /* Brand — deep government blue */
  --clr-primary-900: #062040;
  --clr-primary-800: #0a3060;
  --clr-primary-700: #0a4f8f; /* Primary action, backoffice sidebar bg */
  --clr-primary-600: #1565c0;
  --clr-primary-500: #1976d2;
  --clr-primary-100: #e3f0ff;

  /* Teal accent — portal CTA, approved states */
  --clr-teal-600: #0d7a70;
  --clr-teal-500: #0d9488; /* Portal sidebar bg */
  --clr-teal-400: #14b8a6;
  --clr-teal-100: #ccfbf1;

  /* Neutral scale */
  --clr-neutral-950: #0c1117;
  --clr-neutral-900: #111827;
  --clr-neutral-700: #374151;
  --clr-neutral-500: #6b7280;
  --clr-neutral-300: #d1d5db;
  --clr-neutral-100: #f3f4f6;
  --clr-neutral-50: #f8fafc;

  /* Status colors */
  --clr-status-draft: #6b7280;
  --clr-status-for-approval: #d97706;
  --clr-status-approved: #059669;
  --clr-status-rejected: #dc2626;
  --clr-status-released: #2563eb;

  /* Payment colors */
  --clr-pay-unpaid: #ea580c;
  --clr-pay-paid: #16a34a;
  --clr-pay-waived: #7c3aed;

  /* Surfaces */
  --clr-surface: #ffffff;
  --clr-surface-alt: #f8fafc;
  --clr-border: #e2e8f0;

  /* Elevation */
  --shadow-sm: 0 1px 3px 0 rgb(0 0 0 / 0.08), 0 1px 2px -1px rgb(0 0 0 / 0.08);
  --shadow-md: 0 4px 12px 0 rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.08);
  --shadow-lg: 0 8px 24px -4px rgb(0 0 0 / 0.12);
}
```

Set global defaults:

```css
body {
  font-family: var(--font-geist), system-ui, sans-serif;
  background-color: var(--clr-surface-alt);
  color: var(--clr-neutral-900);
}
```

---

### 3. Tailwind Config (`tailwind.config.ts`)

Extend with:

- `fontFamily.sora`, `fontFamily.geist`
- `colors` mapped from all CSS tokens above
- `boxShadow` entries for `sm`, `md`, `lg` tokens
- `borderRadius` — add `'2xl': '1rem'` if not present

```ts
import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        sora: ["var(--font-sora)", "sans-serif"],
        geist: ["var(--font-geist)", "sans-serif"],
      },
      colors: {
        primary: {
          900: "var(--clr-primary-900)",
          800: "var(--clr-primary-800)",
          700: "var(--clr-primary-700)",
          600: "var(--clr-primary-600)",
          500: "var(--clr-primary-500)",
          100: "var(--clr-primary-100)",
        },
        teal: {
          600: "var(--clr-teal-600)",
          500: "var(--clr-teal-500)",
          400: "var(--clr-teal-400)",
          100: "var(--clr-teal-100)",
        },
        neutral: {
          950: "var(--clr-neutral-950)",
          900: "var(--clr-neutral-900)",
          700: "var(--clr-neutral-700)",
          500: "var(--clr-neutral-500)",
          300: "var(--clr-neutral-300)",
          100: "var(--clr-neutral-100)",
          50: "var(--clr-neutral-50)",
        },
        surface: "var(--clr-surface)",
        border: "var(--clr-border)",
      },
      boxShadow: {
        sm: "var(--shadow-sm)",
        md: "var(--shadow-md)",
        lg: "var(--shadow-lg)",
      },
    },
  },
  plugins: [],
};

export default config;
```

---

### 4. Typography Integration (`app/layout.tsx`)

```tsx
import { Sora } from "next/font/google";
import { Geist } from "next/font/google";

const sora = Sora({
  subsets: ["latin"],
  variable: "--font-sora",
  weight: ["400", "500", "600", "700", "800"],
  display: "swap",
});

const geist = Geist({
  subsets: ["latin"],
  variable: "--font-geist",
  weight: ["400", "500", "600"],
  display: "swap",
});

// Apply both variables to <html>:
// <html className={`${sora.variable} ${geist.variable}`}>
```

---

### 5. Shared Animation Variants (`lib/animations.ts`)

Create `frontend/src/lib/animations.ts` with shared Framer Motion variants:

```ts
export const fadeUp = {
  hidden: { opacity: 0, y: 12 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.3, ease: "easeOut" } },
};

export const staggerContainer = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.06, delayChildren: 0.1 } },
};

export const staggerItem = {
  hidden: { opacity: 0, y: 10 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.25, ease: "easeOut" },
  },
};

export const slideInLeft = {
  hidden: { opacity: 0, x: -16 },
  visible: { opacity: 1, x: 0, transition: { duration: 0.3, ease: "easeOut" } },
};

export const presenceExpand = {
  initial: { opacity: 0, height: 0, overflow: "hidden" },
  animate: {
    opacity: 1,
    height: "auto",
    overflow: "visible",
    transition: { duration: 0.25, ease: "easeOut" },
  },
  exit: {
    opacity: 0,
    height: 0,
    overflow: "hidden",
    transition: { duration: 0.2, ease: "easeIn" },
  },
};
```

---

### 6. UI Primitives (`components/ui/`)

Create each file below. All use `clsx` + `tailwind-merge` for class merging.

#### `Button.tsx`

- Props: `variant` (`primary` | `secondary` | `ghost` | `danger` | `outline`), `size` (`sm` | `md` | `lg`), `loading`, all native button props
- Loading: replaces children with `Loader2` icon (`lucide-react`) + Framer Motion `animate={{ rotate: 360 }}` + `transition={{ repeat: Infinity, duration: 1, ease: 'linear' }}`
- Scale on interaction: `whileHover={{ scale: 1.02 }}` + `whileTap={{ scale: 0.98 }}` (skip if `useReducedMotion()`)

#### `Card.tsx`

- Props: `accentColor` (`'blue'` | `'teal'` | `'amber'` | `'red'` | `'green'` | `undefined`), `hover` (boolean), all div props
- `accentColor` adds `border-l-4` with matching color
- `hover` adds `hover:shadow-md transition-shadow cursor-pointer`

#### `Badge.tsx`

- Props: `variant` (`status` | `payment` | `role`), `value` (the enum string), `dot` (boolean)
- Maps each enum value to color + label (migrates all logic from `StatusBadge.tsx`, `PaymentBadge.tsx`, `RoleBadge.tsx`)
- Deprecate old badge files — re-export from them for backward compat initially

#### `Input.tsx`

- Floating-label pattern: label starts at center, shrinks + moves to top on focus or when value is set
- Uses Framer Motion `AnimatePresence` for error message slide-in
- Error state: red border + `text-red-600` error message beneath
- Props: all `<input>` props + `label`, `error`

#### `Select.tsx`

- Matches `Input` height (`h-11`) and border styling
- Styled native `<select>` — no custom dropdown
- Props: all `<select>` props + `label`, `error`

#### `Textarea.tsx`

- Floating-label pattern same as `Input`
- `rows` defaults to 3
- Props: all `<textarea>` props + `label`, `error`

#### `Skeleton.tsx`

- Exports: `Skeleton` (inline block), `SkeletonText`, `TableRowSkeleton`, `CardSkeleton`, `ListPageSkeleton`, `DetailPageSkeleton`
- Animation: Framer Motion `animate={{ opacity: [0.5, 0.9, 0.5] }} transition={{ repeat: Infinity, duration: 1.5 }}` instead of CSS `animate-pulse`
- Stagger: wrap multiple skeletons in `motion.div` with `staggerChildren: 0.05s`

#### `PageHeader.tsx`

- Props: `title`, `description`, `actions` (ReactNode), `backHref`
- `backHref`: renders `← Back` link with `motion.span whileHover={{ x: -3 }}`
- Title: `font-sora font-bold text-2xl text-neutral-900`
- Description: `font-geist text-sm text-neutral-500`

#### `StatCard.tsx`

- Props: `label`, `value` (number), `icon` (LucideIcon), `accentColor`, `delta` (string, optional)
- Animated counter: `useMotionValue(0)` → `animate(motionValue, value, { duration: 1.2, ease: 'easeOut' })` on mount
- `useReducedMotion()` guard: if true, display static value
- Entrance: Framer Motion `fadeUp` variant

#### `EmptyState.tsx`

- Props: `icon` (LucideIcon), `title`, `description`, `action` (`{ label, href | onClick }`)
- Layout: centered column, icon in `text-neutral-300 mb-4`, title in Sora, description in Geist text-neutral-500
- Entrance: Framer Motion `fadeUp` with slight delay

#### `DataTable.tsx`

- Wrapper component: `<DataTable columns header>` + `<DataTableBody>` + `<DataTableRow>`
- `thead`: `bg-neutral-50 text-neutral-500 text-xs uppercase tracking-wide`
- Rows: Framer Motion `staggerContainer` + `staggerItem` on `<tbody>` rows
- Loading prop: renders `TableRowSkeleton` rows instead

---

### 7. Backoffice Layout Shell (`app/backoffice/layout.tsx` + `components/backoffice/Sidebar.tsx`)

#### Sidebar

- `<motion.aside>` with `animate={{ width: collapsed ? 64 : 256 }} transition={{ type: 'spring', stiffness: 300, damping: 30 }}`
- Background: `bg-gradient-to-b from-[#0A1E3D] to-[#062040]`
- Nav active indicator: `<motion.span layoutId="backoffice-active-nav" />` (4px wide teal bar, left side of item)
- Label text: `<AnimatePresence>` — fade out when collapsed
- Admin sub-section: `<motion.div animate={{ rotate: isOpen ? 180 : 0 }}>` on `ChevronDown`
- Bottom user chip: email (truncated) + `RoleBadge` + logout `Button` (ghost)
- Mobile: `<motion.aside initial={{ x: -256 }} animate={{ x: open ? 0 : -256 }} transition={{ type: 'spring' }} />` with overlay backdrop

#### Topbar

- `bg-white border-b border-[--clr-border]` with `h-16`
- Left: hamburger button (mobile) + breadcrumb in `font-geist text-sm`
- Right: user email + `<Badge>` for role + logout

#### Page transition wrapper

```tsx
// In layout.tsx
<motion.div
  key={pathname}
  initial={{ opacity: 0, y: 8 }}
  animate={{ opacity: 1, y: 0 }}
  transition={{ duration: 0.25, ease: "easeOut" }}
>
  {children}
</motion.div>
```

Use `usePathname()` as the `key` to trigger per-route transitions.

---

### 8. Portal Layout Shell (`app/portal/layout.tsx` + `components/portal/Sidebar.tsx`)

Same structure as backoffice sidebar with:

- Background: `bg-gradient-to-b from-[#0D5FA6] to-[#0A4F8F]`
- Active indicator: `layoutId="portal-active-nav"` (white pill, not teal bar)
- Bottom: resident full name + "Resident" chip

---

## Definition of Done

- [ ] `npm install` succeeds — `framer-motion`, `clsx`, `tailwind-merge`, `class-variance-authority` all in `package.json`
- [ ] CSS tokens visible in browser DevTools `:root` declarations
- [ ] Sora renders on `<h1>` headings; Geist renders on `<p>` and table text (verify via browser font inspector)
- [ ] No FOUT (fonts load with `display: swap`, layout stable)
- [ ] `tailwind.config.ts` — `font-sora`, `font-geist`, `primary-700`, `teal-500`, `shadow-md` all resolve as valid Tailwind classes
- [ ] All 11 `components/ui/` primitives render without errors (smoke-test each in a temporary test page)
- [ ] `lib/animations.ts` exports all variants without TypeScript errors
- [ ] Backoffice sidebar collapses/expands with spring animation (verify in browser)
- [ ] Portal sidebar collapses/expands same
- [ ] Page transition plays on route change within backoffice
- [ ] `useReducedMotion()` — confirm animations are skipped when OS reduced-motion is enabled
- [ ] `npm run build` — zero TypeScript errors
- [ ] `npm run lint` — zero ESLint warnings in new files

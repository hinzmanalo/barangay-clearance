# Phase 13-5: Polish & QA

- **Part of:** Phase 13 — UI/UX Redesign
- **Status:** 🔴 Not Started
- **Depends on:** Phase 13-2, Phase 13-3, Phase 13-4 (all three must be complete)
- **Blocks:** Nothing — this is the final phase in Phase 13
- **Parallel with:** Nothing — runs after all page implementations are done

---

## Goal

Cross-cutting quality pass over the entire redesigned frontend. Validate responsiveness at all breakpoints, enforce accessibility standards, verify animation behavior with reduced-motion systems, confirm zero build/lint errors, and measure the result against the success metrics defined in the PRD.

---

## Dependencies

```
Phase 13-2 (public) ──┐
Phase 13-3 (backoffice) ──► Phase 13-5 (QA & Polish)
Phase 13-4 (portal) ──┘
```

---

## Deliverables

### 1. Responsive Validation

Test every redesigned page at the following viewport widths using browser DevTools:

| Breakpoint | Width             | Key checks                                                                     |
| ---------- | ----------------- | ------------------------------------------------------------------------------ |
| Mobile     | 375px (iPhone SE) | Single-column layout, sidebar hidden, form full-width, table horizontal scroll |
| Tablet     | 768px             | Sidebar overlay closes on nav, 2-col grids activate at `md:`                   |
| Desktop    | 1280px            | Full sidebar visible, 2/3-1/3 detail layouts, 4-up stat grids                  |
| Wide       | 1536px            | Content respects `max-w-7xl`, nothing stretches excessively                    |

**Checklist per page:**

- [ ] Login — left panel hides on mobile, form card fills screen
- [ ] Register — stepper wraps cleanly on mobile, form fields stack single-column
- [ ] Backoffice dashboard — stat grid 2×2 on mobile, 4×1 on desktop
- [ ] Clearances list — table scrolls horizontally on mobile (no overflow clip)
- [ ] Clearance detail — stacks single column on mobile, 2-col on `lg:`
- [ ] Portal dashboard — request cards fill full width on mobile
- [ ] Request detail — stacks single column on mobile

---

### 2. Accessibility Audit

Run browser accessibility audit (DevTools Lighthouse) and manual keyboard navigation:

#### Keyboard navigation

- [ ] All interactive elements (buttons, links, inputs, selects) are reachable via Tab
- [ ] Focus order is logical (left-to-right, top-to-bottom)
- [ ] Focus rings are visible — `focus:outline-2 focus:outline-primary-500` or equivalent on all `<Button>`, `<Input>`, `<Select>` primitives
- [ ] Sidebar nav items are keyboard-accessible; Enter triggers navigation
- [ ] Modal/inline confirm blocks trap focus correctly (if any)

#### ARIA

- [ ] `<Badge>` components include `aria-label` where the visual dot is the only indicator
- [ ] `StatusTimeline` steps include `aria-current="step"` on the active step
- [ ] Loading skeletons have `aria-busy="true"` on their container
- [ ] `<EmptyState>` has no interactive ARIA conflicts
- [ ] All icon-only buttons (`<Button variant="ghost">` with only a lucide icon) have `aria-label`

#### Color contrast

- [ ] All text passes WCAG AA (4.5:1 for body, 3:1 for large text)
- [ ] Verify: `text-neutral-500` on `bg-white` — passes (`#6B7280` / `#FFF` = 4.6:1 ✓)
- [ ] Verify: `text-primary-700` on `bg-white` — passes
- [ ] Verify: white text on `bg-[#0A1E3D]` sidebar — passes
- [ ] Verify: status badge foreground/background combinations all pass

---

### 3. Reduced-Motion Compliance

Test with OS "Reduce Motion" setting enabled (macOS: System Settings → Accessibility → Reduce Motion):

- [ ] All `framer-motion` components that use `useReducedMotion()` skip decorative animations
- [ ] Page transitions: `opacity` only (no `y` translate) when reduced motion
- [ ] Sidebar collapse: still collapses/expands but without spring — instant or simple `opacity`
- [ ] `StatCard` counter: displays final value immediately (no animated count-up)
- [ ] Staggered lists: all items appear simultaneously (no stagger delay)
- [ ] `StatusTimeline` path-draw: lines appear fully drawn (no animation)
- [ ] `AnimatePresence` expand: still shows/hides, but without height tween — use CSS `display` toggle instead

**Implementation pattern** in each animated component:

```tsx
const shouldReduceMotion = useReducedMotion();

const transition = shouldReduceMotion
  ? { duration: 0 }
  : { duration: 0.3, ease: "easeOut" };
```

---

### 4. Token Audit — No Hardcoded Colors

Search all TypeScript/TSX files for raw hex values and inline color classes that bypass the token system:

```bash
# Run from frontend/
grep -rn --include="*.tsx" --include="*.ts" \
  -E '#[0-9a-fA-F]{3,6}|from-\[#|to-\[#|bg-\[#|text-\[#|border-\[#' \
  src/
```

**Exceptions allowed:**

- `from-[#062040]`, `to-[#0A4F8F]`, `from-[#0D5FA6]`, `to-[#0A4F8F]` in layout gradients (these are one-off decorative values not needing tokens)
- `globals.css` `:root` variable definitions themselves

All other instances must be replaced with token-based Tailwind classes.

---

### 5. Build & Lint Clean-Up

```bash
cd frontend

# Full TypeScript compile check
npm run build

# ESLint
npm run lint
```

**Targets:**

- [ ] `npm run build` — zero TypeScript errors, zero warnings
- [ ] `npm run lint` — zero ESLint errors
- [ ] No unused imports in any redesigned file
- [ ] No `any` types introduced by redesign work

---

### 6. Font Rendering Verification

In browser with DevTools open:

- [ ] Open any page with an `<h1>` or `<h2>` — confirm `font-family` is Sora in Computed Styles
- [ ] Open any page with `<p>` body text — confirm `font-family` is Geist in Computed Styles
- [ ] Confirm no FOUT: reload page with network throttled to "Fast 3G" — text renders correctly with no invisible flash
- [ ] Check Network tab: `sora-*.woff2` and `geist-*.woff2` requested with status 200 (from `/_next/static/media/`)

---

### 7. Lighthouse Baseline Measurements

Run Lighthouse in Chrome DevTools (incognito, desktop preset, throttling: no throttle) on the following pages:

| Page                     | Performance target | Accessibility target |
| ------------------------ | ------------------ | -------------------- |
| `/login`                 | ≥ 85               | ≥ 90                 |
| `/portal/dashboard`      | ≥ 85               | ≥ 90                 |
| `/backoffice/dashboard`  | ≥ 85               | ≥ 90                 |
| `/backoffice/clearances` | ≥ 85               | ≥ 90                 |

Record scores in the Progress Log in `project_status.md`.

---

### 8. Cross-Browser Smoke Test

| Browser          | Test                                                          |
| ---------------- | ------------------------------------------------------------- |
| Chrome (latest)  | Full walkthrough: login → portal flow → backoffice flow       |
| Safari (macOS)   | Login, register, portal dashboard, backoffice clearances list |
| Firefox (latest) | Login, backoffice dashboard                                   |

Focus on:

- Framer Motion spring animations (should work — Framer is cross-browser)
- `backdrop-blur-sm` on topbar (Safari may need `-webkit-backdrop-filter`)
- `font-display: swap` — confirm no invisible text flash in Safari

---

### 9. Final Design Review Checklist

Walk through each surface and verify the unified design language is applied:

- [ ] No page uses Arial or system-default `font-sans` for headings (all should be `font-sora`)
- [ ] No page has bare `<h1>` without `font-sora font-bold` classes
- [ ] All action buttons have consistent sizing within their context
- [ ] Status badges are visually identical across backoffice clearances list, clearance detail, portal dashboard, request detail
- [ ] Card elevation is consistent — list pages use `shadow-sm`, detail panels use `shadow-md` in side columns
- [ ] Sidebar nav items have consistent hover + active states across backoffice and portal
- [ ] Empty states are present on: clearances list, residents list, portal dashboard, reports

---

## Definition of Done

- [ ] All responsive breakpoints validated for all redesigned pages
- [ ] Lighthouse Accessibility ≥ 90 on all target pages
- [ ] Lighthouse Performance ≥ 85 on all target pages
- [ ] All keyboard-navigable focus rings visible
- [ ] All icon-only buttons have `aria-label`
- [ ] `StatusTimeline` has `aria-current` on active step
- [ ] `useReducedMotion()` correctly disables all decorative animations
- [ ] Zero hardcoded hex values outside of gradient decorations and `:root` definitions
- [ ] `npm run build` — zero TypeScript errors
- [ ] `npm run lint` — zero ESLint errors
- [ ] Sora and Geist render correctly with no FOUT
- [ ] Cross-browser smoke test passed on Chrome, Safari, Firefox
- [ ] Lighthouse scores recorded in `project_status.md` progress log
- [ ] Phase 13 marked as complete in `project_status.md`

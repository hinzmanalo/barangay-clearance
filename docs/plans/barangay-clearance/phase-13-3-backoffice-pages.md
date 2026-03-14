# Phase 13-3: Backoffice Pages Redesign

- **Part of:** Phase 13 — UI/UX Redesign
- **Status:** 🔴 Not Started
- **Depends on:** Phase 13-1 (Design System Foundation)
- **Blocks:** Phase 13-5 (Polish & QA)
- **Parallel with:** Phase 13-2 (Public Pages), Phase 13-4 (Portal Pages)

---

## Goal

Apply the Phase 13-1 design system to all backoffice pages. Focus on data density, clarity for staff workflows, and rich but purposeful animation. Staff must scan tables faster and take actions with less cognitive friction.

---

## Dependencies

```
Phase 13-1 (complete) ──► Phase 13-3 ──► Phase 13-5 (QA)
                          (parallel with 13-2 and 13-4)
```

Internal order within Phase 13-3 (implement sequentially):

1. Dashboard
2. Clearances list → Clearance detail → Clearance new
3. Residents list → Resident detail → Resident new
4. Admin: Users list → User detail → User new
5. Admin: Settings + Fee Config
6. Admin: Audit Logs
7. Reports

---

## Deliverables

### 1. Dashboard (`app/backoffice/dashboard/page.tsx`)

**Key changes from current state:**

#### Stat cards row (4-up grid)

Replace current plain stat cards with `<StatCard>` primitive (from Phase 13-1):

- **Pending** — amber icon circle (`Clock` icon), `accentColor="amber"`
- **Approved** — teal icon circle (`CheckCircle` icon), `accentColor="teal"`
- **Released** — blue icon circle (`FileCheck` icon), `accentColor="blue"`
- **Total** — neutral icon circle (`LayoutList` icon)
- All four cards animate in with `staggerContainer` (0.1s between each)
- Each counter animates from 0 → value on mount
- 30s auto-poll: replace "auto-refreshes" label with subtle `"Last updated X sec ago"` in `text-xs text-neutral-400`

#### Quick action cards (3-up grid)

Replace current small button links with large `<Card hover>` components:

- "New Walk-in Request" — `FilePlus` icon, links to `/backoffice/clearances/new`
- "View Clearances" — `ClipboardList` icon
- "Manage Residents" — `Users` icon
- Staggered entrance with `staggerItem` after stat cards appear

#### Recent clearances (last 5)

- Compact table (no pagination) inside a `<Card>`
- `<PageHeader title="Recent Clearances">` with `<Button variant="ghost" size="sm">View all →</Button>` in actions slot
- Uses `<DataTable>` primitive with staggered rows

---

### 2. Clearances List (`app/backoffice/clearances/page.tsx`)

**Key changes:**

#### Summary strip

Replace 4-card summary header with a single horizontal stat strip:

```tsx
<div className="flex gap-8 px-6 py-3 bg-white rounded-2xl shadow-sm border border-[--clr-border]">
  <div>
    <span className="font-geist text-xs text-neutral-500 uppercase tracking-wide">
      Pending
    </span>
    <p className="font-sora font-bold text-xl text-amber-600">
      {summary.forApproval}
    </p>
  </div>
  {/* ...repeat for Approved, Released, Total */}
</div>
```

#### Filter bar

Wrap existing filter `<select>` elements in `<Card className="p-4">`:

```tsx
<div className="flex flex-wrap gap-3 items-end">
  <Select label="Status" {...} />
  <Select label="Payment" {...} />
  <Button variant="primary" size="sm">Search</Button>
  <Button variant="ghost" size="sm" onClick={clearFilters}>Clear</Button>
</div>
```

#### Table (`ClearanceTable.tsx`)

- `<DataTable>` wrapper with staggered row entrance
- `thead`: `bg-neutral-50 text-neutral-500 text-xs uppercase tracking-wide font-geist`
- Status + payment columns: replace `<span>` pills with `<Badge>` primitive
- Clearance number: `font-mono text-sm text-primary-700`
- Row hover: `hover:bg-neutral-50 transition-colors`
- Loading: `<DataTable loading>` renders skeleton rows

#### Pagination

Redesign from plain text to:

```tsx
<div className="flex items-center justify-center gap-2 mt-4">
  <Button variant="outline" size="sm" disabled={page === 0} onClick={prev}>
    ← Prev
  </Button>
  <span className="font-geist text-sm text-neutral-500">
    Page {page + 1} of {totalPages}
  </span>
  <Button
    variant="outline"
    size="sm"
    disabled={page >= totalPages - 1}
    onClick={next}
  >
    Next →
  </Button>
</div>
```

---

### 3. Clearance Detail (`app/backoffice/clearances/[id]/page.tsx`)

**Key changes:**

#### Two-column layout (lg+)

```tsx
<div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
  <div className="lg:col-span-2 space-y-6">
    {/* Clearance info card */}
    {/* Redesigned StatusTimeline */}
  </div>
  <div className="space-y-4">
    {/* Action panel card */}
    {/* PDF download card */}
  </div>
</div>
```

#### StatusTimeline (`components/portal/StatusTimeline.tsx`)

Redesign to **always-vertical** layout with path-draw animation:

- 4 steps: For Approval → Approved → Payment → Released (+ Rejected branch)
- Each step: circle dot (filled/hollow/red based on state) + icon + label + timestamp when done
- Connecting line: `<motion.div style={{ scaleY: pathProgress }} origin-top>` — draws downward on mount as steps complete
- Rejected state: red step dot + `XCircle` icon + red connector to DRAFT

#### Action panel (`ActionButtons.tsx`)

- Wrap in `<Card accentColor="blue">` titled "Actions"
- Reject reason `<Textarea>`: `<AnimatePresence>` expand when "Reject" is clicked
- All action buttons use `<Button>` primitive variants:
  - Approve: `variant="primary"` with `CheckCircle` icon
  - Reject: `variant="danger"` with `XCircle` icon
  - Mark Paid: `variant="secondary"` (teal-ish) with `CreditCard` icon
  - Release: `variant="primary"` (green tint) with `Send` icon

#### PDF download

Separate small `<Card>` below action panel:

```tsx
<Card className="p-4">
  <p className="font-geist text-xs text-neutral-500 mb-3 uppercase tracking-wide">
    Document
  </p>
  <Button variant="outline" size="sm" className="w-full" onClick={downloadPdf}>
    <Download className="w-4 h-4 mr-2" /> Download PDF
  </Button>
</Card>
```

Wrap button in `<motion.div whileTap={{ scale: 0.97 }}>`.

---

### 4. Clearance New (`app/backoffice/clearances/new/page.tsx`)

- `<PageHeader title="New Walk-in Request" backHref="/backoffice/clearances">`
- Wrap form fields in `<Card className="p-6">`
- All `<input>` / `<select>` → `<Input>` / `<Select>` primitives
- `<Button variant="primary" loading={isSubmitting}>Submit Request</Button>`

---

### 5. Residents List (`app/backoffice/residents/page.tsx`)

Same pattern as Clearances list. Key additions:

#### Pending activations panel

Replace current yellow `<div>` alert with:

```tsx
<Card accentColor="amber" className="p-5">
  <div className="flex items-center gap-2 mb-4">
    <UserCheck className="w-5 h-5 text-amber-600" />
    <h3 className="font-sora font-semibold text-base text-neutral-900">
      Pending Portal Activations ({pendingCount})
    </h3>
  </div>
  {/* List of pending residents with Activate (teal Button) + Reject (danger Button) */}
</Card>
```

#### ResidentTable (`components/backoffice/ResidentTable.tsx`)

- Same `<DataTable>` + staggered rows pattern
- Portal status: `<Badge>` (active=teal, pending=amber, rejected=red, none=neutral)

---

### 6. Resident Detail + New (`app/backoffice/residents/[id]/page.tsx`, `residents/new/page.tsx`)

- Two-column card layout on `lg:`:
  - Left: Personal info grid (`<Card>`)
  - Right: Portal account status (`<Card accentColor="teal">`)
- All bare inputs → `<Input>` / `<Select>` primitives
- `<PageHeader>` with back link

---

### 7. Admin: Users List, Detail, New

_(Only relevant if Phase 11 — User Management — is complete. Apply the same patterns.)_

- `UserTable.tsx` → `<DataTable>` + staggered rows
- Status `<Badge>`, Role `<Badge>` for each row
- User detail: two-column (`<Card>` info + `<Card accentColor="red">` danger zone: deactivate, reset password)
- Deactivate / reset-password confirmation: `<AnimatePresence>` inline confirm block (no modal needed)

---

### 8. Admin: Settings (`app/backoffice/admin/settings/page.tsx`)

- `<PageHeader title="Barangay Settings">`
- Contact info fields in `<Card className="p-6">` using `<Input>` / `<Select>` primitives
- Logo upload: replace current `<input type="file">` with a styled drop zone:

```tsx
<div
  className="border-2 border-dashed border-[--clr-border] rounded-xl p-8 flex flex-col items-center gap-3
                hover:border-primary-500 hover:bg-primary-100/30 transition-colors cursor-pointer"
>
  <Upload className="w-8 h-8 text-neutral-400" />
  <p className="font-geist text-sm text-neutral-500">
    Click to upload or drag & drop
  </p>
  <p className="font-geist text-xs text-neutral-400">
    PNG, JPG, GIF — max 2 MB
  </p>
</div>
```

- Current logo preview: `<img>` wrapped in `<Card className="p-3 w-32 h-32 object-contain">`

#### Fee Config (`app/backoffice/admin/settings/fees/page.tsx`)

- Side-by-side cards: Standard Fee (`<Card accentColor="blue">`) + Rush Fee (`<Card accentColor="teal">`)
- Each card: fee label, `<Input type="number">` for amount, save button

---

### 9. Admin: Audit Logs (`app/backoffice/admin/audit-logs/page.tsx`)

_(Only relevant if Phase 12 — Audit Logging — is complete.)_

#### Collapsible filter panel

```tsx
<Button variant="ghost" size="sm" onClick={() => setFiltersOpen(v => !v)}>
  <Filter className="w-4 h-4 mr-2" /> Filters
  <motion.span animate={{ rotate: filtersOpen ? 180 : 0 }}><ChevronDown /></motion.span>
</Button>
<AnimatePresence>
  {filtersOpen && (
    <motion.div {...presenceExpand}>
      {/* Action type, entity type, user ID, date range filters */}
    </motion.div>
  )}
</AnimatePresence>
```

#### AuditLogTable (`components/backoffice/AuditLogTable.tsx`)

- Action column: `<Badge>` with action-type color map:
  - `CREATE*`: green
  - `UPDATE*` / `CHANGE_*`: amber
  - `DELETE*` / `DEACTIVATE*` / `REJECT*`: red
  - `LOGIN*` / `LOGOUT*`: blue
- Entity ID column: `font-mono text-xs text-neutral-500`
- Expandable row details: click row → `<AnimatePresence>` inline expand showing `details` JSON

---

### 10. Reports (`app/backoffice/reports/page.tsx`)

- `<PageHeader title="Clearance Reports">`
- Filter `<Card className="p-5">`: date range `<Input type="date">` pickers, status + payment `<Select>`, "Generate" `<Button variant="primary">`
- Results count: `<motion.span animate={{ opacity: [0, 1] }}>` badge showing total match count after filter applied
- Export: `<Button variant="outline" size="sm"><Download /> Download CSV</Button>` right-aligned in results header
- Table: `<DataTable>` + staggered rows; empty state: `<EmptyState icon={FileSearch} title="No results" description="Adjust your filters" />`

---

## Definition of Done

- [ ] Dashboard stat counters animate from 0 on mount; quick action cards stagger-enter
- [ ] Clearances list uses `<DataTable>` with staggered row entrance
- [ ] All status + payment badges use `<Badge>` primitive (no bare `<span>` pills)
- [ ] Clearance detail uses two-column layout on `lg:` breakpoint
- [ ] `StatusTimeline` redraws connecting lines on mount with Framer Motion
- [ ] Reject reason textarea expands/collapses with `AnimatePresence`
- [ ] Pending activations panel in Residents uses `<Card accentColor="amber">`
- [ ] Settings page: logo upload uses dashed drop zone; fee config uses side-by-side cards
- [ ] Audit Logs filter panel collapses/expands with spring animation
- [ ] Reports shows empty state when no results
- [ ] All bare `<input>`, `<select>`, `<textarea>`, `<button>` replaced with `components/ui/` primitives
- [ ] No hardcoded hex colors in any file — only token-based Tailwind classes
- [ ] Existing API calls, React Query hooks, and business logic fully preserved
- [ ] `npm run build` — zero TypeScript errors in all affected backoffice files

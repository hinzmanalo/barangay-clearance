# Phase 13-4: Portal Pages Redesign

- **Part of:** Phase 13 — UI/UX Redesign
- **Status:** 🔴 Not Started
- **Depends on:** Phase 13-1 (Design System Foundation)
- **Blocks:** Phase 13-5 (Polish & QA)
- **Parallel with:** Phase 13-2 (Public Pages), Phase 13-3 (Backoffice Pages)

---

## Goal

Apply the Phase 13-1 design system to all resident-facing portal pages. Residents are often non-technical; the redesign must make status tracking effortless, the submission flow frictionless, and every state (pending, approved, rejected, released) unambiguous at a glance.

---

## Dependencies

```
Phase 13-1 (complete) ──► Phase 13-4 ──► Phase 13-5 (QA)
                          (parallel with 13-2 and 13-3)
```

Internal order within Phase 13-4 (implement sequentially):

1. Portal layout shell (already done in Phase 13-1 — just verify)
2. Portal dashboard
3. New request form
4. Request detail

---

## Deliverables

### 1. Portal Layout Shell (verify from Phase 13-1)

Before starting portal pages, confirm `app/portal/layout.tsx` and `components/portal/Sidebar.tsx` were completed in Phase 13-1:

- Teal gradient sidebar (`from-[#0D5FA6] to-[#0A4F8F]`)
- Animated collapse/expand with spring physics
- `layoutId="portal-active-nav"` white pill active indicator
- Page transition `motion.div` wrapper keyed to `usePathname()`

---

### 2. Portal Dashboard (`app/portal/dashboard/page.tsx`)

**Key changes from current state:**

#### Welcome banner

```tsx
<Card accentColor="teal" className="p-5 flex items-center justify-between">
  <div>
    <h2 className="font-sora font-semibold text-xl text-neutral-900">
      Welcome back, {firstName}!
    </h2>
    <p className="font-geist text-sm text-neutral-500 mt-1">
      Track and manage your clearance requests below.
    </p>
  </div>
  {/* Status summary chips: e.g. "2 Pending" */}
</Card>
```

#### New Request CTA

```tsx
// Right-aligned on md+, full-width on mobile
<div className="flex justify-end">
  <Button variant="primary" href="/portal/requests/new">
    <FilePlus className="w-4 h-4 mr-2" /> New Clearance Request
  </Button>
</div>
```

#### Request cards (`RequestCard.tsx` — redesign)

Replace current minimal card with a richer card:

```tsx
<Card hover className="p-5">
  {/* Top row: clearance number + status badge + date */}
  <div className="flex items-start justify-between gap-3 mb-3">
    <span className="font-mono text-sm font-medium text-primary-700">
      {clearanceNumber ?? "Pending #"}
    </span>
    <div className="flex items-center gap-2 flex-shrink-0">
      <Badge variant="status" value={status} />
      <span className="font-geist text-xs text-neutral-400">
        {formatDate(createdAt)}
      </span>
    </div>
  </div>

  {/* Middle: purpose */}
  <p className="font-geist text-sm text-neutral-700 mb-3">
    {PURPOSE_LABELS[purpose]}
  </p>

  {/* Bottom: payment badge + view link */}
  <div className="flex items-center justify-between">
    <Badge variant="payment" value={paymentStatus} />
    <Link
      href={`/portal/requests/${id}`}
      className="font-geist text-sm font-medium text-primary-600 hover:underline flex items-center gap-1"
    >
      View Details <ChevronRight className="w-3 h-3" />
    </Link>
  </div>
</Card>
```

**Staggered entrance animation:**
Wrap card list in `<motion.div variants={staggerContainer} initial="hidden" animate="visible">` with each `<RequestCard>` wrapped in `<motion.div variants={staggerItem}>`.

**Empty state:**
When `requests.length === 0` at any pagination:

```tsx
<EmptyState
  icon={FileText}
  title="No clearance requests yet"
  description="Submit your first request to get started."
  action={{ label: "Submit a request", href: "/portal/requests/new" }}
/>
```

---

### 3. New Request Form (`app/portal/requests/new/page.tsx`)

**Key changes:**

- `<PageHeader title="New Clearance Request" backHref="/portal/dashboard">`
- Wrap all form fields in a single `<Card className="p-6 space-y-5">`
- Replace bare inputs/selects:

  ```tsx
  <Select label="Purpose" {...register('purpose')} error={errors.purpose?.message}>
    {Object.entries(PURPOSE_LABELS).map(([value, label]) => (
      <option key={value} value={value}>{label}</option>
    ))}
  </Select>

  {/* Conditional "Other" field — AnimatePresence expand */}
  <AnimatePresence>
    {watchedPurpose === 'OTHER' && (
      <motion.div {...presenceExpand}>
        <Input label="Please describe your purpose" {...register('purposeOther')} error={errors.purposeOther?.message} />
      </motion.div>
    )}
  </AnimatePresence>

  <Select label="Urgency" {...register('urgency')} error={errors.urgency?.message}>
    <option value="REGULAR">Regular</option>
    <option value="RUSH">Rush (+₱{rushFee})</option>
  </Select>

  <Input label="Number of copies" type="number" min={1} max={5} {...register('copies')} error={errors.copies?.message} />
  <Textarea label="Additional notes (optional)" rows={3} {...register('notes')} />
  ```

- Fee preview card (shown after urgency selected):
  ```tsx
  <Card accentColor="teal" className="p-4">
    <p className="font-geist text-sm text-neutral-600">Estimated fee</p>
    <p className="font-sora font-bold text-2xl text-teal-600">
      ₱{calculatedFee}
    </p>
    <p className="font-geist text-xs text-neutral-400 mt-1">
      {urgency === "RUSH" ? "Rush processing" : "Standard processing"}
    </p>
  </Card>
  ```
- Submit button:
  ```tsx
  <Button
    variant="primary"
    size="lg"
    className="w-full"
    loading={isSubmitting}
    type="submit"
  >
    Submit Request
  </Button>
  ```

**Preserve existing logic:**

- `POST /api/v1/portal/clearances` API call
- React Query mutation + invalidation
- Redirect to `/portal/dashboard` + success toast on completion

---

### 4. Request Detail (`app/portal/requests/[id]/page.tsx`)

**Key changes:**

#### Layout

Two-column on `lg:`:

```tsx
<div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
  <div className="lg:col-span-2 space-y-5">
    {/* Request info card */}
    {/* Redesigned StatusTimeline */}
    {/* Rejection reason card (if REJECTED) */}
  </div>
  <div className="space-y-4">
    {/* Payment info card (if APPROVED + UNPAID) */}
    {/* PDF download card (if RELEASED) */}
  </div>
</div>
```

#### Request info card

```tsx
<Card className="p-6">
  <PageHeader
    title={`Clearance Request`}
    description={clearanceNumber ?? "Number pending"}
  />
  <dl className="grid grid-cols-2 gap-x-6 gap-y-3 mt-4 font-geist text-sm">
    <div>
      <dt className="text-neutral-500">Purpose</dt>
      <dd className="text-neutral-900 font-medium">
        {PURPOSE_LABELS[purpose]}
      </dd>
    </div>
    <div>
      <dt className="text-neutral-500">Urgency</dt>
      <dd>{urgency}</dd>
    </div>
    <div>
      <dt className="text-neutral-500">Copies</dt>
      <dd>{copies}</dd>
    </div>
    <div>
      <dt className="text-neutral-500">Submitted</dt>
      <dd>{formatDate(createdAt)}</dd>
    </div>
  </dl>
</Card>
```

#### StatusTimeline (redesigned — same component as backoffice, shared)

- Always vertical
- Path-draw animation on connecting lines between completed steps
- Steps: For Approval → Approved → Payment → Released
- Rejected: red step with `XCircle` icon, no forward progress

#### Rejection reason card (if `status === 'REJECTED'`)

```tsx
<Card accentColor="red" className="p-5">
  <div className="flex items-center gap-2 mb-2">
    <XCircle className="w-5 h-5 text-red-600" />
    <h4 className="font-sora font-semibold text-sm text-red-700">
      Request Rejected
    </h4>
  </div>
  <p className="font-geist text-sm text-neutral-700">{rejectionReason}</p>
  <Button variant="ghost" size="sm" className="mt-3" onClick={handleResubmit}>
    Resubmit request →
  </Button>
</Card>
```

#### Payment info card (right column — if `APPROVED` + `UNPAID`)

```tsx
<Card accentColor="teal" className="p-5">
  <div className="flex items-center gap-2 mb-3">
    <CreditCard className="w-5 h-5 text-teal-600" />
    <h4 className="font-sora font-semibold text-base text-neutral-900">
      Payment Required
    </h4>
  </div>
  <p className="font-geist text-2xl font-bold text-teal-600 mb-1">
    ₱{fee.toFixed(2)}
  </p>
  <p className="font-geist text-xs text-neutral-500 mb-4">
    Visit the barangay hall to complete payment.
  </p>
  <Badge variant="payment" value="UNPAID" />
</Card>
```

#### PDF download card (right column — if `RELEASED`)

```tsx
<Card accentColor="green" className="p-5">
  <div className="flex items-center gap-2 mb-3">
    <FileCheck className="w-5 h-5 text-green-600" />
    <h4 className="font-sora font-semibold text-base text-neutral-900">
      Clearance Ready
    </h4>
  </div>
  <p className="font-geist text-sm text-neutral-500 mb-4">
    Your clearance has been issued. Download your official document.
  </p>
  <motion.div whileTap={{ scale: 0.97 }}>
    <Button
      variant="primary"
      size="md"
      className="w-full bg-green-600 hover:bg-green-700"
      onClick={downloadPdf}
    >
      <Download className="w-4 h-4 mr-2" /> Download Clearance PDF
    </Button>
  </motion.div>
</Card>
```

**Preserve existing logic:**

- API call for request data (`GET /api/v1/portal/clearances/:id`)
- PDF download blob handler
- Resubmit → `PUT .../resubmit` API call

---

## Definition of Done

- [ ] Portal layout teal sidebar renders and collapses/expands (Phase 13-1 verified)
- [ ] Dashboard welcome banner displays resident first name
- [ ] New Request CTA button is prominent and correctly positioned
- [ ] `RequestCard` renders clearance number in monospace, status `<Badge>`, payment `<Badge>`
- [ ] Cards stagger-enter with `staggerContainer` + `staggerItem` variants
- [ ] Empty state renders `<EmptyState>` with `FileText` icon and "Submit a request" CTA
- [ ] New request form: `purposeOther` field expands/collapses with `AnimatePresence`
- [ ] Fee preview card updates reactively when urgency changes
- [ ] Request detail: two-column layout on `lg:` breakpoint
- [ ] `StatusTimeline` path-draw animation plays; rejected step renders in red
- [ ] Rejection reason card shows (if applicable) with resubmit button
- [ ] Payment info card shows when `APPROVED + UNPAID`
- [ ] PDF download card shows (and only shows) when `RELEASED`
- [ ] All bare `<input>`, `<select>`, `<textarea>`, `<button>` replaced with primitives
- [ ] Existing API calls and React Query hooks fully preserved — no regressions
- [ ] `npm run build` — zero TypeScript errors in all portal files

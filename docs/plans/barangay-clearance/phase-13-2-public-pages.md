# Phase 13-2: Public Pages Redesign

- **Part of:** Phase 13 — UI/UX Redesign
- **Status:** 🔴 Not Started
- **Depends on:** Phase 13-1 (Design System Foundation) — all `components/ui/` primitives and tokens must be complete
- **Blocks:** Phase 13-5 (Polish & QA)
- **Parallel with:** Phase 13-3 (Backoffice Pages), Phase 13-4 (Portal Pages)

---

## Goal

Redesign the three public-facing pages (Login, Register, Change Password) using the design system established in Phase 13-1. These pages form the first impression of the system for both residents and staff — they must project trust, clarity, and official government character.

---

## Dependencies

```
Phase 13-1 (complete) ──► Phase 13-2 ──► Phase 13-5 (QA)
                          (parallel with 13-3 and 13-4)
```

---

## Deliverables

### 1. Login Page (`app/login/page.tsx`)

**Layout**: Split-screen, two columns on `md:` and above; single column (form only) on mobile.

#### Left Panel (`hidden md:flex w-1/2`)

```tsx
// Deep navy gradient panel
<div className="bg-gradient-to-br from-[#062040] to-[#0A4F8F] flex flex-col items-center justify-center p-12 relative overflow-hidden">
  {/* Barangay seal — Shield icon placeholder until settings logo is set */}
  <Shield className="w-16 h-16 text-white/80 mb-6" strokeWidth={1.5} />

  {/* App name */}
  <h1 className="font-sora font-bold text-4xl text-white text-center leading-tight">
    Barangay
    <br />
    Clearance System
  </h1>

  {/* Tagline */}
  <p className="font-sora text-base text-blue-200 mt-4 text-center max-w-xs">
    Ang serbisyo ng barangay,
    <br />
    nasa dulo ng iyong daliri.
  </p>

  {/* Decorative SVG Philippines outline at bottom — opacity-10 */}
  {/* Decorative dot-grid pattern top-right — opacity-5 */}
</div>
```

#### Right Panel (`flex-1 flex items-center justify-center bg-[#F8FAFC] p-8`)

Framer Motion entrance — card `initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4, ease: 'easeOut' }}`:

```tsx
<Card className="w-full max-w-md p-8 shadow-lg rounded-2xl bg-white">
  <h2 className="font-sora font-bold text-2xl text-neutral-900 mb-1">
    Welcome back
  </h2>
  <p className="font-geist text-sm text-neutral-500 mb-8">
    Sign in to your account
  </p>

  {/* React Hook Form + Zod — preserve existing validation logic */}
  <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
    <Input
      label="Email address"
      type="email"
      {...register("email")}
      error={errors.email?.message}
    />
    <Input
      label="Password"
      type="password"
      showToggle
      {...register("password")}
      error={errors.password?.message}
    />

    {/* Global error (invalid credentials) */}
    <AnimatePresence>
      {serverError && (
        <motion.p
          {...presenceExpand}
          className="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2"
        >
          {serverError}
        </motion.p>
      )}
    </AnimatePresence>

    <Button
      variant="primary"
      size="lg"
      className="w-full"
      loading={isSubmitting}
      type="submit"
    >
      Sign In
    </Button>
  </form>

  <p className="mt-6 text-center font-geist text-sm text-neutral-500">
    New resident?{" "}
    <Link
      href="/register"
      className="text-primary-600 font-medium hover:underline"
    >
      Register here
    </Link>
  </p>
</Card>
```

**Preserve existing logic:**

- `useAuth().login()` call
- Role-based redirect after login (`RESIDENT` → `/portal/dashboard`, staff → `/backoffice/dashboard`)
- `mustChangePassword` redirect to `/change-password`

---

### 2. Register Page (`app/register/page.tsx`)

**Layout**: Centered single column, `max-w-2xl` card.

#### Header strip

```tsx
<div className="bg-primary-700 py-4 px-8 flex items-center gap-3">
  <Shield className="w-7 h-7 text-white" />
  <span className="font-sora font-semibold text-lg text-white">
    Barangay Clearance System
  </span>
</div>
```

#### 2-Step stepper indicator

```tsx
// Step dots with connector line between them
// Step 1: Account Info
// Step 2: Personal Info
// Active step: filled primary-700 circle with white number
// Completed step: teal-500 circle with CheckIcon
// Inactive step: neutral-300 circle
```

#### Step content — `AnimatePresence mode="wait"`

**Step 1: Account Info**

```tsx
<motion.div
  key="step1"
  initial={{ opacity: 0, x: 30 }}
  animate={{ opacity: 1, x: 0 }}
  exit={{ opacity: 0, x: -30 }}
  transition={{ duration: 0.25 }}
>
  <Input
    label="Email address"
    type="email"
    {...register("email")}
    error={errors.email?.message}
  />
  <Input
    label="Password"
    type="password"
    showToggle
    {...register("password")}
    error={errors.password?.message}
  />
  <Input
    label="Confirm password"
    type="password"
    showToggle
    {...register("confirmPassword")}
    error={errors.confirmPassword?.message}
  />
</motion.div>
```

**Step 2: Personal Info**

```tsx
<motion.div
  key="step2"
  initial={{ opacity: 0, x: 30 }}
  animate={{ opacity: 1, x: 0 }}
  exit={{ opacity: 0, x: -30 }}
>
  {/* Two-column grid on md: */}
  <Input
    label="First name"
    {...register("firstName")}
    error={errors.firstName?.message}
  />
  <Input label="Middle name (optional)" {...register("middleName")} />
  <Input
    label="Last name"
    {...register("lastName")}
    error={errors.lastName?.message}
  />
  <Input
    label="Date of birth"
    type="date"
    {...register("birthDate")}
    error={errors.birthDate?.message}
  />
  <Select label="Gender" {...register("gender")} error={errors.gender?.message}>
    <option value="">Select gender</option>
    <option value="MALE">Male</option>
    <option value="FEMALE">Female</option>
    <option value="OTHER">Other</option>
  </Select>
  <Input label="Street / House no." {...register("street")} />
  <Input label="Purok" {...register("purok")} />
  <Input label="City / Municipality" {...register("city")} />
</motion.div>
```

#### Navigation row

```tsx
<div className="flex justify-between mt-6">
  {step === 2 && (
    <Button variant="ghost" onClick={() => setStep(1)}>
      ← Back
    </Button>
  )}
  {step === 1 ? (
    <Button variant="primary" onClick={handleNext} className="ml-auto">
      Next →
    </Button>
  ) : (
    <Button
      variant="primary"
      type="submit"
      loading={isSubmitting}
      className="ml-auto"
    >
      Create account
    </Button>
  )}
</div>
```

**Preserve existing logic:**

- Zod schema and field validation (same fields as before)
- `POST /api/v1/auth/register` call
- Redirect to `/login` with success toast on completion

---

### 3. Change Password Page (`app/change-password/page.tsx`)

**Layout**: Minimal centered card, `max-w-md`.

```tsx
<div className="min-h-screen bg-[#F8FAFC] flex items-center justify-center p-6">
  <motion.div
    initial={{ opacity: 0, y: 20 }}
    animate={{ opacity: 1, y: 0 }}
    transition={{ duration: 0.35 }}
  >
    <Card className="w-full max-w-md p-8 shadow-lg rounded-2xl bg-white">
      {/* Amber warning banner */}
      <div className="flex items-start gap-3 rounded-lg bg-amber-50 border border-amber-200 px-4 py-3 mb-6">
        <AlertTriangle className="w-5 h-5 text-amber-600 mt-0.5 flex-shrink-0" />
        <p className="font-geist text-sm text-amber-800">
          For your security, please set a new password before continuing.
        </p>
      </div>

      <h2 className="font-sora font-bold text-xl text-neutral-900 mb-6">
        Set new password
      </h2>

      <form className="space-y-5">
        <Input
          label="Current password"
          type="password"
          showToggle
          {...register("currentPassword")}
          error={errors.currentPassword?.message}
        />
        <Input
          label="New password"
          type="password"
          showToggle
          {...register("newPassword")}
          error={errors.newPassword?.message}
        />
        <Input
          label="Confirm new password"
          type="password"
          showToggle
          {...register("confirmPassword")}
          error={errors.confirmPassword?.message}
        />
        <Button
          variant="primary"
          size="lg"
          className="w-full"
          loading={isSubmitting}
        >
          Update password
        </Button>
      </form>
    </Card>
  </motion.div>
</div>
```

**Preserve existing logic:**

- `PUT /api/v1/auth/change-password` call
- `refreshAuth()` after success
- Redirect to role home after password update

---

## Definition of Done

- [ ] Login page renders split-screen layout on `md:` breakpoints, single-column on mobile
- [ ] Login form card entrance animation plays on mount
- [ ] Floating-label `Input` components animate correctly on focus/blur
- [ ] Loading spinner rotates on "Sign In" button while request is in-flight
- [ ] Server error message animates in/out via `AnimatePresence`
- [ ] Register page 2-step stepper navigates with slide transitions
- [ ] Step 1 → Step 2 slide-out-left / slide-in-right animation plays
- [ ] Step back button slides Step 1 back in from the left
- [ ] Change password warning banner renders with amber styling
- [ ] All three pages use only `components/ui/` primitives (no bare `<input>` or `<button>` tags)
- [ ] Existing validation logic and API calls fully preserved — no regressions
- [ ] `npm run build` — zero TypeScript errors in these three files

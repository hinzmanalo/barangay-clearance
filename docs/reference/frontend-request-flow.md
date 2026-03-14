# Frontend Request Flow: localhost:3000

## Overview

This document explains the exact sequence of files and logic that executes when you navigate to `localhost:3000` in the Barangay Clearance System frontend.

---

## Step-by-Step Sequence

### **Step 1: Next.js Server Loads Configuration**
**File:** `frontend/next.config.mjs`

- Next.js loads the config (API routes, image optimization, redirects, etc.)
- This runs **once at startup**

---

### **Step 2: Middleware Runs (Server-Side) — FIRST LOGIC**
**File:** `frontend/src/middleware.ts` (lines 17–44)

This is the **first logic** that executes for **every request**:

```typescript
// Request: GET http://localhost:3000 (pathname = "/")
const pathname = "/";
const token = getTokenFromCookieOrHeader(request); 
// → looks for request.cookies.get('accessToken')
// → if not logged in, token = null

// Line 44: No token → redirect to login
if (!token) {
  const loginUrl = new URL('/login', request.url);
  loginUrl.searchParams.set('next', pathname);
  return NextResponse.redirect(loginUrl);
  // → Redirects to /login?next=/
}
```

**Result:** Browser is redirected → `/login?next=/`

---

### **Step 3: Root Page Renders (Backup Fallback)**
**File:** `frontend/src/app/page.tsx`

If middleware somehow allowed `/` through, this would render:

```typescript
export default function Home() {
  redirect("/login");  // Client-side redirect to /login
}
```

**Note:** This is a fallback. Middleware already redirects, so this rarely executes.

---

### **Step 4: Root Layout Wraps Everything**
**File:** `frontend/src/app/layout.tsx`

After the middleware redirect, the **login page** loads. The root layout wraps it:

```typescript
// <html> + <body> wrapper
export const metadata: Metadata = {
  title: "Barangay Clearance System",
  description: "Digital barangay clearance issuance system",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={`${sora.variable} ${geist.variable}`}>
      <body className="antialiased">
        <Providers>  {/* ← wraps children */}
          {children} {/* ← this becomes <LoginPage /> */}
        </Providers>
      </body>
    </html>
  );
}
```

---

### **Step 5: Providers Initialize Context**
**File:** `frontend/src/app/providers.tsx`

Wraps the entire app with:
- `AuthProvider` — initializes auth state
- React Query client
- Other providers

```typescript
export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <TanStackQueryProvider>
      <AuthProvider>
        {children}
      </AuthProvider>
    </TanStackQueryProvider>
  );
}
```

---

### **Step 6: AuthContext Hydrates from localStorage**
**File:** `frontend/src/context/AuthContext.tsx` (lines 45–65)

Inside `AuthProvider`, a `useEffect` runs **on first render**:

```typescript
useEffect(() => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    const payload = decodeToken(token);
    if (payload && payload.exp * 1000 > Date.now()) {
      // Token valid → restore auth state, set cookie for middleware
      document.cookie = `accessToken=${token}; ...`;
      setState({
        userId: payload.sub,
        email: payload.email,
        role: payload.role,
        mustChangePassword: payload.mustChangePassword ?? false,
        isAuthenticated: true,
        isLoading: false,
      });
      return;
    }
  }
  // No token or expired → stay on login page
  setState(prev => ({ ...prev, isLoading: false }));
}, []);
```

**Note:** This only runs on **first mount** or **full page reload** — not on client-side navigation.

---

### **Step 7: Login Page Renders**
**File:** `frontend/src/app/login/page.tsx`

The login page component renders with the login form:

```typescript
export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);

  const onSubmit = async (data: LoginFormData) => {
    setServerError(null);
    try {
      const { role, mustChangePassword } = await login(data);
      if (mustChangePassword) {
        router.push('/change-password');
        return;
      }
      if (role === 'RESIDENT') {
        router.push('/portal/dashboard');
      } else {
        router.push('/backoffice/dashboard');
      }
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      setServerError(axiosErr.response?.data?.message ?? 'Login failed. Please try again.');
    }
  };

  return (/* Login form UI */);
}
```

---

## Flow After Successful Login

### **Step 8: User Submits Login Form**

User enters email + password and clicks submit.

### **Step 9: API Request → Backend Login**

**File:** `frontend/src/lib/api.ts` (lines 10–18) — Request interceptor

```typescript
api.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});
```

POST request to `http://localhost:8080/api/v1/auth/login` with credentials.

### **Step 10: Backend Returns Tokens**

Backend responds with:
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxx",
  "mustChangePassword": false
}
```

### **Step 11: Tokens Stored Locally**

**File:** `frontend/src/context/AuthContext.tsx` (lines 67–91) — `login()` function

```typescript
const login = useCallback(async (data: LoginRequest) => {
  const response = await api.post<TokenResponse>('/api/v1/auth/login', data);
  const { accessToken, refreshToken, mustChangePassword } = response.data;

  // Store in localStorage
  localStorage.setItem('accessToken', accessToken);
  if (refreshToken) {
    localStorage.setItem('refreshToken', refreshToken);
  }

  const payload = decodeToken(accessToken);

  // Sync to cookie for middleware
  if (payload) {
    document.cookie = `accessToken=${accessToken}; path=/; SameSite=Lax; max-age=${payload.exp - Math.floor(Date.now() / 1000)}`;
  }
  if (!payload) throw new Error('Invalid token received');

  // Update auth state
  setState({
    userId: payload.sub,
    email: payload.email,
    role: payload.role,
    mustChangePassword: payload.mustChangePassword ?? false,
    isAuthenticated: true,
    isLoading: false,
  });

  return { role: payload.role, mustChangePassword: mustChangePassword ?? false };
}, []);
```

### **Step 12: Navigate to Dashboard**

```typescript
if (role === 'RESIDENT') {
  router.push('/portal/dashboard');
} else {
  router.push('/backoffice/dashboard');
}
```

### **Step 13: Middleware Validates Route Again**

When `router.push()` is called, **middleware runs again** with the new route:

- **Request:** `GET /portal/dashboard`
- **Middleware:** Reads `request.cookies.get('accessToken')` (now set)
- **Token validation:** Checks expiry + role
- **Result:** Allows request through → dashboard page renders

---

## Complete Flow Diagram

```
Browser: GET http://localhost:3000
    ↓
Next.js Dev Server loads next.config.mjs
    ↓
middleware.ts runs (⭐ FIRST LOGIC)
    ├─ pathname = "/"
    ├─ token = null (no cookie)
    └─ → redirect to /login?next=/
    ↓
app/layout.tsx renders (root HTML wrapper)
    ↓
app/providers.tsx wraps children:
    ├─ TanStackQueryProvider
    └─ AuthProvider
    ↓
AuthContext useEffect runs:
    ├─ reads localStorage.getItem('accessToken')
    ├─ if valid → restore auth state + sync cookie
    └─ if none/expired → stay on login
    ↓
app/login/page.tsx renders (login form)
    ↓
════════════════════════════════════════════════════
🔓 USER LOGS IN (after credentials submitted)
════════════════════════════════════════════════════
    ↓
POST /api/v1/auth/login (API request interceptor attaches Bearer token)
    ↓
Backend validates + returns accessToken + refreshToken
    ↓
login() function stores tokens:
    ├─ localStorage.setItem('accessToken', ...)
    ├─ localStorage.setItem('refreshToken', ...)
    ├─ document.cookie = `accessToken=...` (for middleware)
    └─ setState({ isAuthenticated: true, role: ... })
    ↓
router.push('/portal/dashboard') (or /backoffice/dashboard)
    ↓
middleware.ts runs AGAIN
    ├─ pathname = "/portal/dashboard"
    ├─ token = valid (from request.cookies)
    ├─ payload.role = "RESIDENT" ✓
    └─ → allow request through
    ↓
portal/layout.tsx renders
    ↓
portal/dashboard/page.tsx renders (dashboard)
```

---

## Two Sources of Truth

The architecture relies on two separate token storage locations:

| Layer | Source | Purpose |
|-------|--------|---------|
| **Server (middleware)** | `request.cookies.get('accessToken')` | Route protection — happens first on every request |
| **Client (context)** | `localStorage.getItem('accessToken')` | Restores user session, survives page reloads |

### Why Two?

- **Cookies:** Automatically sent by browser to server on every request (middleware can read them)
- **localStorage:** Persists across page reloads; accessible only to JavaScript

### When They Get Out of Sync

If they diverge (e.g., cookie expires but localStorage is still valid), the user can experience **intermittent redirects to login** even though they have a valid refresh token. See [fix-intermittent-login-redirect.md](../plans/barangay-clearance/fix-intermittent-login-redirect.md) for the detailed bug analysis and fix.

---

## Key Files Reference

| File | Purpose |
|------|---------|
| `middleware.ts` | **First logic** — validates token, enforces route access control |
| `app/layout.tsx` | Root HTML wrapper, loads fonts, sets metadata |
| `app/providers.tsx` | Wraps app with TanStackQuery + AuthProvider |
| `context/AuthContext.tsx` | Auth state, login/logout, token hydration from localStorage |
| `lib/api.ts` | Axios instance with request interceptor (Bearer token) + response interceptor (401 → refresh) |
| `app/page.tsx` | Fallback for `/` → redirects to `/login` |
| `app/login/page.tsx` | Login form UI |

---

## Notes

- Middleware is **synchronous** and **stateless** — doesn't know about localStorage or React state
- `AuthContext` only runs on the **client side** after hydration; middleware never sees it
- On SSR/RSC, only cookies are visible to the server — localStorage is always empty server-side
- Client-side navigation (`router.push`) bypasses middleware on the client but triggers an RSC prefetch, which **does** run middleware on the server

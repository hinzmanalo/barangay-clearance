# Deployment Implementation Plan — Free Tier (Vercel + Render + Neon)

**Date:** 2026-03-05
**Based on:** `deployment-design-free-tier.md`
**Target:** Prepare the codebase for deployment; no platform setup (Neon/Render/Vercel dashboards are manual)

---

## Summary

Phases 0–9 are complete. The only code work needed before pushing to production is:

1. Create `backend/Dockerfile`
2. Create `frontend/Dockerfile`
3. Update `frontend/next.config.mjs` — add `output: 'standalone'`
4. Update `backend/src/main/resources/application-prod.yml` — add CORS and payment env-var wiring
5. Update `SecurityConfig.java` — make CORS allowed origins configurable via env var
6. Create `frontend/.env.example` — document the one required frontend env var
7. Create `backend/.env.example` — document all required backend env vars

No database migrations are needed. Flyway runs all 9 existing migrations automatically on first backend startup against the Neon DB.

---

## Files To Create

### 1. `backend/Dockerfile`

**Path:** `backend/Dockerfile`
**Why:** Render builds the Spring Boot service from this file. Does not exist yet — `docker compose up` would fail without it.

Multi-stage build:

- **Stage 1 (`builder`):** `eclipse-temurin:21-jdk-alpine`. Copies `.mvn/`, `mvnw`, and `pom.xml` first, then runs `dependency:go-offline` so subsequent builds use the layer cache. Then copies `src/` and runs `./mvnw clean package -DskipTests -q`.
- **Stage 2 (`runtime`):** `eclipse-temurin:21-jre-alpine` (smaller image — no compiler). Creates a non-root OS group `spring` and user `spring`. Copies the fat JAR from the builder stage as `app.jar`. Switches to `USER spring:spring`. Exposes port `8080`. Entrypoint: `java -jar app.jar`.

**Key notes:**

- `-DskipTests` because integration tests require a live DB (Testcontainers); they should not run inside the image build.
- JRE image keeps the final image significantly smaller than JDK.
- Non-root user is a security baseline.

---

### 2. `frontend/Dockerfile`

**Path:** `frontend/Dockerfile`
**Why:** Not required by Vercel (Vercel builds from source), but kept for parity and for anyone running `docker compose` locally. Does not exist yet.

Multi-stage build:

- **Stage 1 (`builder`):** `node:20-alpine`. Copies `package*.json`, runs `npm ci`, copies the rest, runs `npm run build`. Requires `output: 'standalone'` to be set in `next.config.mjs` (Step 3 below) — otherwise the standalone output directory won't exist.
- **Stage 2 (`runner`):** `node:20-alpine`. Sets `NODE_ENV=production`. Creates non-root OS group `nextjs` and user `nextjs`. Copies from builder:
  - `public/` directory
  - `.next/standalone/` → `./ ` (the self-contained Node server)
  - `.next/static/` → `.next/static/` (static assets)
- Switches to `USER nextjs`. Sets `ENV PORT 3000`. Exposes `3000`. Entrypoint: `node server.js`.

**Key notes:**

- `NEXT_PUBLIC_API_URL` must be passed as a `--build-arg` (`ARG NEXT_PUBLIC_API_URL`) and then set as `ENV` in the builder stage — Next.js bakes public env vars into the JS bundle at build time. This is only relevant for Docker builds; Vercel handles this via its own env var injection at build time.

---

### 3. `frontend/.env.example`

**Path:** `frontend/.env.example`
**Why:** There is currently no `.env.example` inside the `frontend/` directory (only one at the repo root for the VPS stack). Developers cloning the repo need to know what to set for `frontend/.env.local`.

Contents:

```
# Required for local development — points the Next.js app at the backend API
# For production (Vercel), set this in the Vercel dashboard under Settings → Environment Variables
NEXT_PUBLIC_API_URL=http://localhost:8080
```

**Key notes:**

- Only one variable is needed. `api.ts` already falls back to `http://localhost:8080` if this is not set, so local dev works without any `.env.local` at all. The example is for clarity.
- Do NOT commit a `.env.local` file — it should be gitignored.
- Vercel injects `NEXT_PUBLIC_API_URL` at build time, so no file is needed in production.

---

### 4. `backend/.env.example`

**Path:** `backend/.env.example`
**Why:** Documents all env vars that Render needs. Developers can reference this when configuring the Render dashboard. Should also serve as a local override template if anyone runs the backend against a remote Neon DB locally.

Contents (variable names only — no real values):

```
# Spring profile — always 'prod' on Render
SPRING_PROFILES_ACTIVE=prod

# Neon PostgreSQL — from the Neon project dashboard
# Convert: postgresql://user:pass@host/db → jdbc:postgresql://host/db (split user/pass into USERNAME/PASSWORD)
DB_URL=jdbc:postgresql://ep-xxx.us-east-2.aws.neon.tech/barangay_clearance?sslmode=require
DB_USERNAME=
DB_PASSWORD=

# JWT — generate with: openssl rand -hex 32
JWT_SECRET=
JWT_ACCESS_EXPIRY_MS=900000
JWT_REFRESH_EXPIRY_MS=604800000

# Payment stub
PAYMENT_STUB_ALWAYS_SUCCESS=true

# CORS — set to the Vercel frontend URL (no trailing slash)
CORS_ALLOWED_ORIGINS=https://<project>.vercel.app
```

---

## Files To Modify

### 5. `frontend/next.config.mjs`

**Path:** `frontend/next.config.mjs`
**Current state:** Empty config object `{}`.
**Change:** Add `output: 'standalone'`.

After change:

```js
/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "standalone",
};

export default nextConfig;
```

**Why:** Next.js standalone output bundles only the files needed to run the server (`server.js` + minimal `node_modules`). Without this, the Docker runner stage has no `server.js` to start. Vercel does not require it, but the `frontend/Dockerfile` depends on it. This has no negative effect on the Vercel deployment — Vercel ignores this setting and manages its own build output.

---

### 6. `backend/src/main/resources/application-prod.yml`

**Path:** `backend/src/main/resources/application-prod.yml`
**Current state:** Contains datasource, JPA, JWT, and logging config only.

**Changes:**

1. Add `cors.allowed-origins` binding sourced from env var `CORS_ALLOWED_ORIGINS`.
2. Add `payment.stub.always-success` binding sourced from env var `PAYMENT_STUB_ALWAYS_SUCCESS`.
3. Disable Swagger/OpenAPI in production (optional but recommended — prevents publicly browsing the API schema).

After change, the file should look like:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    show-sql: false

app:
  jwt:
    secret: ${JWT_SECRET}
    access-token-expiry-ms: ${JWT_ACCESS_EXPIRY_MS:900000}
    refresh-token-expiry-ms: ${JWT_REFRESH_EXPIRY_MS:604800000}
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:https://localhost:3000}

payment:
  stub:
    always-success: ${PAYMENT_STUB_ALWAYS_SUCCESS:false}

springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false

logging:
  level:
    root: WARN
    com.barangay.clearance: INFO
```

**Why:**

- `CORS_ALLOWED_ORIGINS` must be the Vercel URL. Without this, every API request from the browser will be rejected with a CORS error.
- `PAYMENT_STUB_ALWAYS_SUCCESS` is wired in `application.yml` at the `payment.stub` key but is not currently passed through to `application-prod.yml`. Render must be able to set it.
- Disabling Swagger in prod prevents the API from being publicly browsable — acceptable trade-off for a prototype.

---

### 7. `SecurityConfig.java` — CORS origins

**Path:** `backend/src/main/java/com/barangay/clearance/shared/security/SecurityConfig.java`
**Current state:** `corsConfigurationSource()` hardcodes `List.of("http://localhost:3000")`.

**Change:** Inject an `@Value("${app.cors.allowed-origins:http://localhost:3000}")` field and use it in `corsConfigurationSource()`. Split the value on `,` to support multiple origins if needed.

Before:

```java
config.setAllowedOrigins(List.of("http://localhost:3000"));
```

After (conceptually):

```java
@Value("${app.cors.allowed-origins:http://localhost:3000}")
private String allowedOriginsRaw;

// inside corsConfigurationSource():
List<String> origins = Arrays.asList(allowedOriginsRaw.split(","));
config.setAllowedOrigins(origins);
```

**Why:** Without this change, even if `application-prod.yml` binds the env var, it has no effect because `SecurityConfig` never reads it. Every AJAX call from `https://<project>.vercel.app` to `https://<service>.onrender.com` would be blocked by the browser CORS preflight.

**Impact on existing profiles:**

- `local` profile: `app.cors.allowed-origins` is NOT set in `application-local.yml`, so the `@Value` default `http://localhost:3000` kicks in — no regression.
- `test` profile: same default applies — no regression.
- `no-auth` profile: `SecurityConfig` is not loaded (it has `@Profile("!no-auth")`) — no regression.

---

## Environment Variables Reference

### Render (Backend)

| Variable                      | Where it's read                                        | Notes                                   |
| ----------------------------- | ------------------------------------------------------ | --------------------------------------- |
| `SPRING_PROFILES_ACTIVE`      | Spring Boot                                            | Must be `prod`                          |
| `DB_URL`                      | `application-prod.yml` → `spring.datasource.url`       | JDBC format with `sslmode=require`      |
| `DB_USERNAME`                 | `application-prod.yml` → `spring.datasource.username`  | From Neon                               |
| `DB_PASSWORD`                 | `application-prod.yml` → `spring.datasource.password`  | From Neon                               |
| `JWT_SECRET`                  | `application-prod.yml` → `app.jwt.secret`              | `openssl rand -hex 32`                  |
| `JWT_ACCESS_EXPIRY_MS`        | `application-prod.yml`                                 | Default `900000` (15 min) — optional    |
| `JWT_REFRESH_EXPIRY_MS`       | `application-prod.yml`                                 | Default `604800000` (7 days) — optional |
| `CORS_ALLOWED_ORIGINS`        | `application-prod.yml` → `SecurityConfig`              | Vercel URL, no trailing slash           |
| `PAYMENT_STUB_ALWAYS_SUCCESS` | `application-prod.yml` → `payment.stub.always-success` | Set `true` for prototype                |

### Vercel (Frontend)

| Variable              | Where it's read                       | Notes                                 |
| --------------------- | ------------------------------------- | ------------------------------------- |
| `NEXT_PUBLIC_API_URL` | `frontend/src/lib/api.ts` → `baseURL` | Render backend URL ending with `/api` |

---

## Implementation Order

The changes are independent except for steps 6 and 7 (both touch CORS — do them together in one compile cycle).

```
1. Update next.config.mjs          → trivial, one line
2. Create frontend/.env.example    → documentation only
3. Create backend/.env.example     → documentation only
4. Update application-prod.yml     → add cors + payment + disable swagger
5. Update SecurityConfig.java      → read cors origins from @Value
6. Create backend/Dockerfile       → test locally: docker build -t bc-backend ./backend
7. Create frontend/Dockerfile      → test locally: docker build -t bc-frontend ./frontend
```

After steps 5–6: run `cd backend && ./mvnw clean compile` to confirm MapStruct and annotation processors are happy with the new `@Value` field.

---

## Verification Checklist (After Platform Setup)

- [ ] `https://<service>.onrender.com/actuator/health` returns `{"status":"UP"}`
- [ ] Render logs show `Successfully applied 9 migrations to schema "public"`
- [ ] `https://<project>.vercel.app` loads the login page
- [ ] Browser DevTools → Network → no CORS errors on login POST
- [ ] Log in as `admin@barangay.gov.ph` (temp password in `docs/ADMIN_SETUP.md`)
- [ ] Forced password change completes successfully
- [ ] Configure barangay settings (name, captain, logo)
- [ ] Create a test resident → submit clearance request → approve → download PDF
- [ ] Swagger UI is NOT accessible at `https://<service>.onrender.com/swagger-ui.html`

---

## Out of Scope for This Plan

Per `deployment-design-free-tier.md`, the following are NOT needed:

- Nginx — Vercel and Render provide TLS termination
- `docker-compose.yml` VPS stack — not used by Render/Vercel
- `scripts/backup-db.sh` — Neon handles 7-day point-in-time recovery on free tier
- Let's Encrypt / certbot — platform-managed TLS
- Any new Flyway migration — schema is complete through V9

# Deployment Design — Free Tier (Prototype)

**Date:** 2026-02-27
**Status:** Approved / Ready to Implement
**Scope:** Deploy the full Barangay Clearance System (frontend + backend + database) on free-tier cloud services for prototype/demo use.

---

## Chosen Stack

| Layer      | Service  | Plan             | URL Pattern                      |
| ---------- | -------- | ---------------- | -------------------------------- |
| Frontend   | Vercel   | Hobby/Free       | `https://<project>.vercel.app`   |
| Backend    | Render   | Free Web Service | `https://<service>.onrender.com` |
| Database   | Neon     | Free (0.5 GB)    | Serverless PostgreSQL            |
| TLS/Domain | Included | —                | Platform-provided subdomains     |

No custom domain, no Nginx, no VPS — platforms handle TLS and routing.

---

## Architecture

```
[Browser]
   │
   ├── frontend.vercel.app  ──→  Vercel (Next.js)
   │                                │
   │                                │  NEXT_PUBLIC_API_URL
   │                                ▼
   └── backend.onrender.com  ──→  Render (Spring Boot)
                                      │
                                      │  JDBC (sslmode=require)
                                      ▼
                                 Neon (PostgreSQL 16)
```

- Vercel serves the Next.js app. It calls the Render backend using `NEXT_PUBLIC_API_URL`.
- Render builds the Spring Boot backend from `backend/Dockerfile` on every push to `main`.
- Neon provides persistent PostgreSQL. Flyway migrations run on backend startup.
- No Nginx layer needed — both Vercel and Render provide TLS termination automatically.

---

## CI/CD Flow

```
git push origin main
       │
       ├──→ Vercel detects change in frontend/ → rebuilds Next.js → deploys
       └──→ Render detects change → rebuilds Docker image → redeploys Spring Boot
```

Both deployments are automatic on every push to `main` via GitHub integration.

---

## Free Tier Limitations & Trade-offs

| Concern                  | Detail                                                                                                                              |
| ------------------------ | ----------------------------------------------------------------------------------------------------------------------------------- |
| **Render cold starts**   | Free web services spin down after **15 min of inactivity**. Spring Boot (JVM) takes ~15–30s to wake up. Acceptable for a prototype. |
| **Neon compute pause**   | Neon's serverless compute pauses after ~5 min of no DB connections. Auto-wakes in ~1–2s on next query — negligible.                 |
| **Neon storage**         | 0.5 GB — sufficient for prototype data volume.                                                                                      |
| **Vercel bandwidth**     | 100 GB/month free — more than enough for demos.                                                                                     |
| **Render build minutes** | 500 free build-minutes/month — sufficient for active development.                                                                   |

---

## Required Code Changes

### 1. `frontend/next.config.mjs`

Add `output: 'standalone'` to optimize the Docker image size for Render:

```js
const nextConfig = {
  output: "standalone",
};
export default nextConfig;
```

### 2. `backend/Dockerfile` _(create — does not exist yet)_

Multi-stage build: JDK 21 for compilation, JRE 21 for runtime. Non-root user for security.

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q
COPY src ./src
RUN ./mvnw clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder /app/target/*.jar app.jar
USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 3. `frontend/Dockerfile` _(create — does not exist yet)_

Multi-stage build using Next.js standalone output:

```dockerfile
# Stage 1: Build
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Runtime
FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
RUN addgroup -S nextjs && adduser -S nextjs -G nextjs
COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nextjs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nextjs /app/.next/static ./.next/static
USER nextjs
EXPOSE 3000
ENV PORT 3000
CMD ["node", "server.js"]
```

---

## Environment Variables

### Render (Spring Boot Backend)

Set these in the Render dashboard under **Environment**:

| Variable                      | Value                                                                                 |
| ----------------------------- | ------------------------------------------------------------------------------------- |
| `SPRING_PROFILES_ACTIVE`      | `prod`                                                                                |
| `DB_URL`                      | `jdbc:postgresql://ep-xxx.us-east-2.aws.neon.tech/barangay_clearance?sslmode=require` |
| `DB_USERNAME`                 | _(from Neon dashboard)_                                                               |
| `DB_PASSWORD`                 | _(from Neon dashboard)_                                                               |
| `JWT_SECRET`                  | _(generate: `openssl rand -hex 32`)_                                                  |
| `JWT_ACCESS_EXPIRY_MS`        | `900000`                                                                              |
| `JWT_REFRESH_EXPIRY_MS`       | `604800000`                                                                           |
| `PAYMENT_STUB_ALWAYS_SUCCESS` | `true`                                                                                |

### Vercel (Next.js Frontend)

Set these in the Vercel dashboard under **Settings → Environment Variables**:

| Variable              | Value                                            |
| --------------------- | ------------------------------------------------ |
| `NEXT_PUBLIC_API_URL` | `https://<your-render-service>.onrender.com/api` |

---

## Step-by-Step Setup

### Step 1 — Neon (Database)

1. Create account at [neon.tech](https://neon.tech)
2. Create a new project → name it `barangay-clearance`
3. Copy the **Connection string** from the dashboard (format: `postgresql://user:pass@ep-xxx.region.aws.neon.tech/barangay_clearance?sslmode=require`)
4. Convert to JDBC format for Spring Boot:
   - Replace `postgresql://` with `jdbc:postgresql://`
   - Split the user/password out into separate env vars (`DB_USERNAME`, `DB_PASSWORD`)
5. No manual schema setup needed — Flyway runs migrations on first backend startup.

### Step 2 — Render (Backend)

1. Create account at [render.com](https://render.com) → connect GitHub
2. **New → Web Service** → select the `barangay-clearance` repo
3. Settings:
   - **Root Directory:** `backend`
   - **Dockerfile Path:** `./Dockerfile`
   - **Instance Type:** Free
   - **Branch:** `main`
4. Add all environment variables from the table above
5. Click **Create Web Service** — first build will take ~5–8 min
6. After deploy, verify: `https://<service>.onrender.com/actuator/health` → `{"status":"UP"}`
7. Verify Flyway: check **Logs** for `Successfully applied N migrations`

### Step 3 — Vercel (Frontend)

1. Create account at [vercel.com](https://vercel.com) → connect GitHub
2. **New Project** → import the `barangay-clearance` repo
3. Settings:
   - **Framework Preset:** Next.js
   - **Root Directory:** `frontend`
4. Add environment variable: `NEXT_PUBLIC_API_URL` = `https://<your-render-service>.onrender.com/api`
5. Click **Deploy** — first build takes ~2–3 min
6. After deploy, visit the Vercel URL → login page should load

### Step 4 — Post-Deploy Verification

- [ ] `https://<project>.vercel.app` loads the login page
- [ ] Log in as `admin@barangay.gov.ph` with the temp password from `docs/ADMIN_SETUP.md`
- [ ] Change admin password (forced on first login)
- [ ] Configure barangay settings (name, captain, logo) via back-office
- [ ] Create a test resident and submit a clearance request end-to-end
- [ ] Download the generated PDF

---

## What This Design Does NOT Include

The following from the original Phase 10 VPS plan are **not needed** for this free-tier deployment:

- Nginx (Vercel and Render handle TLS/proxy)
- `docker-compose.yml` production stack (not used — platforms run containers directly)
- `scripts/backup-db.sh` (Neon handles backups automatically on free tier — point-in-time for 7 days)
- Let's Encrypt/certbot (platforms provide managed TLS certificates)

---

## Implementation Checklist

- [ ] Create `backend/Dockerfile`
- [ ] Create `frontend/Dockerfile`
- [ ] Update `frontend/next.config.mjs` — add `output: 'standalone'`
- [ ] Set up Neon project and copy connection string
- [ ] Create Render web service and configure env vars
- [ ] Create Vercel project and configure env vars
- [ ] Run post-deploy verification checklist above
- [ ] Update `project_status.md` — mark Phase 10 complete

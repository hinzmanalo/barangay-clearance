# Phase 10 — Deployment

**Status:** Not Started
**Estimated Timeline:** Week 7–8
**Priority:** Medium

---

## Goal

Containerize both services and provide a production-ready Docker Compose configuration with Nginx TLS termination, health checks, and a daily PostgreSQL backup script.

---

## Dependencies

**Depends on:**
- Phase 9 (Testing & QA) — all QA flows must pass before production deployment
- All feature phases (0–8) — deployment packages the full application

**Can start in parallel with:** Phase 9 — infrastructure setup (Nginx config, Dockerfiles, `docker-compose.yml`) can be done while Phase 9 testing is underway. However, the production deployment checklist requires Phase 9 to be complete.

---

## Deliverables

### Backend Dockerfile (multi-stage)
```dockerfile
# Stage 1: Build (eclipse-temurin:21-jdk-alpine)
# Stage 2: Runtime (eclipse-temurin:21-jre-alpine)
# Non-root user: spring:spring
# ENTRYPOINT: java -jar app.jar
```

### Frontend Dockerfile (multi-stage)
```dockerfile
# Stage 1: Build (node:20-alpine) — npm ci + npm run build
# Stage 2: Runtime (node:20-alpine) — Next.js standalone output
# next.config.ts: output: 'standalone'
```

### `docker-compose.yml` — Production (4 services)
- `postgres` — `postgres:16-alpine` with health check (`pg_isready`)
- `backend` — depends on `postgres` (condition: healthy); environment from `.env`
- `frontend` — `NEXT_PUBLIC_API_URL` from `.env`
- `nginx` — `nginx:1.27-alpine`; ports 80 + 443; mounts `nginx.conf` + TLS certs volume

### `nginx/nginx.conf` — Production
- HTTP (80) → HTTPS (301 redirect)
- HTTPS (443): TLS config (TLSv1.2 + TLSv1.3, HIGH ciphers)
- `/api/` → `proxy_pass http://backend:8080` (30s timeout, forwarded headers)
- `/` → `proxy_pass http://frontend:3000` (WebSocket upgrade support)
- Increased proxy buffers for PDF responses (128k/256k)

### `scripts/backup-db.sh`
- Daily cron: `0 2 * * *`
- `pg_dump` piped through `gzip` → timestamped `.sql.gz` file
- Retention: delete backups older than 30 days
- Make executable: `chmod +x scripts/backup-db.sh`
- Cron log: `/var/log/barangay-backup.log`

### Production Deployment Checklist
See below in Implementation Notes.

---

## Key Implementation Notes

### Backend Dockerfile Security
```dockerfile
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
```
Non-root user reduces container attack surface.

### Next.js Standalone Output
Configure in `next.config.ts`:
```typescript
const nextConfig = { output: 'standalone' };
```
This bundles only the necessary files, reducing image size.

### Health Check
```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U ${DB_USER} -d ${DB_NAME:-barangay_clearance}"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 30s
```
Backend `depends_on: postgres: condition: service_healthy` ensures DB is ready before Spring starts.

### TLS Setup (Let's Encrypt)
```bash
certbot certonly --standalone -d your-domain.com
cp /etc/letsencrypt/live/your-domain.com/fullchain.pem nginx/certs/
cp /etc/letsencrypt/live/your-domain.com/privkey.pem nginx/certs/
```
For intranet: use self-signed certificate.

### Production Environment Variables (Required)
| Variable | Description |
|---|---|
| `DB_USER` | Non-default PostgreSQL username |
| `DB_PASSWORD` | Strong random password (≥20 chars) |
| `DB_NAME` | Database name |
| `JWT_SECRET` | 256-bit hex (`openssl rand -hex 32`) — never reuse |
| `NEXT_PUBLIC_API_URL` | `https://your-domain.com/api/v1` |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `PAYMENT_PROVIDER` | `stub` (change to real provider in Phase 2) |
| `PAYMENT_STUB_ALWAYS_SUCCESS` | `true` |

### Build Commands
```bash
docker build -t barangay-clearance-api:latest backend/
docker build -t barangay-clearance-web:latest frontend/
docker compose --env-file .env up -d
```

### Post-Launch Steps
1. Verify Flyway migrations: `docker logs barangay-clearance-backend-1 | grep "Flyway"`
2. Log in as `admin@barangay.gov.ph`, change password
3. Configure barangay settings (name, logo, captain name)
4. Set production fees
5. Configure cron for daily database backup

---

## Definition of Done

- [ ] `docker compose --env-file .env up -d` starts all 4 services with no errors
- [ ] `docker compose ps` shows all services as healthy
- [ ] `https://your-domain.com` loads the login page
- [ ] `https://your-domain.com/api/v1/auth/login` responds with 200 for valid credentials
- [ ] `http://your-domain.com` redirects to HTTPS (301)
- [ ] `./scripts/backup-db.sh` creates a `.sql.gz` file in the backup directory
- [ ] All Phase 9 QA flows pass on the Docker Compose stack

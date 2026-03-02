# Phase 9.3 — E2E Tests + Docker Compose + CI Pipeline

## Overview

Adds Cypress E2E tests (3 spec files), a Docker Compose test environment (`docker-compose.test.yml`), seed data, and a GitHub Actions CI pipeline that gates PRs on backend + frontend tests and runs E2E on push to main.

**PRD Reference:** [phase-09-testing-prd.md](phase-09-testing-prd.md) — Sections 4.18–4.24, 4.21

---

## Prerequisites

- Phase 9.1 (backend tests) and 9.2 (frontend tests) are complete and passing
- Docker and Docker Compose v2 installed locally
- Backend has a working `Dockerfile` (or can be built via `./mvnw clean package -DskipTests`)
- Frontend has a working `Dockerfile` for production builds

---

## Step 1: Install Cypress

**File:** `frontend/package.json`

```bash
cd frontend && npm install -D cypress
```

Add scripts:

```json
{
  "scripts": {
    "cypress:run": "cypress run",
    "cypress:open": "cypress open"
  }
}
```

---

## Step 2: Create Cypress configuration

**File:** `frontend/cypress.config.ts`

```typescript
import { defineConfig } from 'cypress';

export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:3000',
    specPattern: 'cypress/e2e/**/*.cy.ts',
    supportFile: 'cypress/support/e2e.ts',
    video: true,
    screenshotOnRunFailure: true,
    defaultCommandTimeout: 10000,
    viewportWidth: 1280,
    viewportHeight: 720,
  },
});
```

---

## Step 3: Create Cypress support file + custom commands

**File:** `frontend/cypress/support/e2e.ts`

```typescript
// Custom command: login as a specific role via API (bypasses UI)
Cypress.Commands.add('loginAs', (role: 'RESIDENT' | 'CLERK' | 'APPROVER' | 'ADMIN') => {
  const credentials: Record<string, { email: string; password: string }> = {
    RESIDENT: { email: 'resident@test.com', password: 'Test1234!' },
    CLERK:    { email: 'clerk@test.com',    password: 'Test1234!' },
    APPROVER: { email: 'approver@test.com', password: 'Test1234!' },
    ADMIN:    { email: 'admin@test.com',    password: 'Test1234!' },
  };

  const { email, password } = credentials[role];

  cy.request('POST', 'http://localhost:8080/api/v1/auth/login', { email, password })
    .then((response) => {
      const { accessToken, refreshToken } = response.body;
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      // Set cookie for middleware
      document.cookie = `accessToken=${accessToken}; path=/; SameSite=Lax`;
    });
});
```

**File:** `frontend/cypress/support/index.d.ts` (TypeScript declarations)

```typescript
declare namespace Cypress {
  interface Chainable {
    loginAs(role: 'RESIDENT' | 'CLERK' | 'APPROVER' | 'ADMIN'): Chainable<void>;
  }
}
```

---

## Step 4: Create seed SQL script

**File:** `frontend/cypress/fixtures/seed.sql`

This script inserts test users (one per role) with known credentials. Must be compatible with the Flyway schema (V1–V3).

```sql
-- Seed test users for Cypress E2E
-- Passwords are BCrypt hashes of "Test1234!"
-- All users are ACTIVE status

INSERT INTO users (id, email, password, role, status, must_change_password, created_at, updated_at)
VALUES
  ('a0000000-0000-0000-0000-000000000001', 'admin@test.com',    '$2a$10$...', 'ADMIN',    'ACTIVE', false, NOW(), NOW()),
  ('a0000000-0000-0000-0000-000000000002', 'clerk@test.com',    '$2a$10$...', 'CLERK',    'ACTIVE', false, NOW(), NOW()),
  ('a0000000-0000-0000-0000-000000000003', 'approver@test.com', '$2a$10$...', 'APPROVER', 'ACTIVE', false, NOW(), NOW()),
  ('a0000000-0000-0000-0000-000000000004', 'resident@test.com', '$2a$10$...', 'RESIDENT', 'ACTIVE', false, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

-- Seed a resident profile linked to the resident user
INSERT INTO residents (id, user_id, first_name, last_name, date_of_birth, gender, civil_status, purok, street, status, created_at, updated_at)
VALUES
  ('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000004',
   'Juan', 'Dela Cruz', '1990-01-15', 'MALE', 'SINGLE', 'Purok 1', '123 Main St', 'ACTIVE', NOW(), NOW())
ON CONFLICT (user_id) DO NOTHING;
```

**Implementation note:** Generate the actual BCrypt hash for "Test1234!" using:
```bash
cd backend && ./mvnw exec:java -Dexec.mainClass="org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder" -pl backend
```
Or use an online BCrypt generator / a small Java main method.

---

## Step 5: Create docker-compose.test.yml

**File:** `docker-compose.test.yml` (project root)

```yaml
version: '3.9'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: barangay_test
      POSTGRES_USER: barangay
      POSTGRES_PASSWORD: barangay_test
    ports:
      - "5432:5432"
    tmpfs:
      - /var/lib/postgresql/data   # ephemeral — clean state each run
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U barangay -d barangay_test"]
      interval: 3s
      timeout: 5s
      retries: 10

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: test
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/barangay_test
      SPRING_DATASOURCE_USERNAME: barangay
      SPRING_DATASOURCE_PASSWORD: barangay_test
      SPRING_FLYWAY_ENABLED: "true"
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval: 5s
      timeout: 10s
      retries: 20
      start_period: 30s

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    environment:
      NEXT_PUBLIC_API_URL: http://backend:8080
    ports:
      - "3000:3000"
    depends_on:
      backend:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:3000 || exit 1"]
      interval: 5s
      timeout: 10s
      retries: 15
      start_period: 20s

networks:
  default:
    name: barangay-test-net
```

**Key decisions:**
- `tmpfs` for PostgreSQL data — ephemeral, no volume cleanup needed
- Health checks on all services — enables `docker compose up --wait`
- Backend profile `test` uses the same `application-test.yml` with fixed JWT secret
- Frontend uses `NEXT_PUBLIC_API_URL` pointing to the backend container

---

## Step 6: Cypress E2E Spec — Auth Flow

**File:** `frontend/cypress/e2e/auth.cy.ts`

**PRD Reference:** Section 4.18

```typescript
describe('Authentication Flow', () => {

  it('registers a new user and shows pending confirmation', () => {
    cy.visit('/register');
    cy.get('[name="firstName"]').type('Test');
    cy.get('[name="lastName"]').type('User');
    cy.get('[name="email"]').type(`testuser_${Date.now()}@test.com`);
    cy.get('[name="password"]').type('Test1234!');
    // Fill other required fields...
    cy.get('button[type="submit"]').click();
    // Assert redirect to login or pending page
    cy.url().should('match', /\/(login|register)/);
  });

  it('logs in a pre-seeded ACTIVE resident', () => {
    cy.visit('/login');
    cy.get('[name="email"]').type('resident@test.com');
    cy.get('[name="password"]').type('Test1234!');
    cy.get('button[type="submit"]').click();
    cy.url().should('include', '/portal');
  });

  it('shows logged-in user name in navbar', () => {
    cy.loginAs('RESIDENT');
    cy.visit('/portal/dashboard');
    cy.get('[data-testid="navbar"]').should('contain.text', 'Juan');
  });

  it('logs out and redirects to login', () => {
    cy.loginAs('RESIDENT');
    cy.visit('/portal/dashboard');
    cy.get('[data-testid="logout-button"]').click();
    cy.url().should('include', '/login');
    cy.window().then((win) => {
      expect(win.localStorage.getItem('accessToken')).to.be.null;
    });
  });

  it('maintains session on re-login', () => {
    cy.visit('/login');
    cy.get('[name="email"]').type('resident@test.com');
    cy.get('[name="password"]').type('Test1234!');
    cy.get('button[type="submit"]').click();
    cy.url().should('include', '/portal');
  });

  it('shows error on wrong password', () => {
    cy.visit('/login');
    cy.get('[name="email"]').type('resident@test.com');
    cy.get('[name="password"]').type('WrongPass!');
    cy.get('button[type="submit"]').click();
    cy.get('[role="alert"], .text-red-500, [data-testid="error-message"]')
      .should('be.visible');
  });

});
```

**Implementation notes:**
- Selectors (e.g., `[name="email"]`) must match actual form field names. Verify against the login/register page components.
- Add `data-testid` attributes to key UI elements (navbar user name, logout button, error messages) if not already present.
- Use Cypress retry semantics (`.should()`) — no `cy.wait(ms)`.

---

## Step 7: Cypress E2E Spec — Clearance Lifecycle

**File:** `frontend/cypress/e2e/clearance-lifecycle.cy.ts`

**PRD Reference:** Section 4.19

```typescript
describe('Clearance Lifecycle', () => {
  let clearanceId: string;

  it('resident submits a clearance request', () => {
    cy.loginAs('RESIDENT');
    cy.visit('/portal/new-request');
    // Fill clearance form
    cy.get('[name="purpose"]').select('EMPLOYMENT');
    cy.get('button[type="submit"]').click();
    // Assert redirect to my-requests with new clearance
    cy.url().should('include', '/portal');
    cy.contains('FOR_APPROVAL').should('be.visible');
    // Capture clearance ID for subsequent steps
    // (extract from URL or data attribute)
  });

  it('approver approves the clearance', () => {
    cy.loginAs('APPROVER');
    cy.visit('/backoffice/clearances');
    // Find the submitted clearance
    cy.contains('FOR_APPROVAL').first().click();
    cy.get('[data-testid="approve-button"]').click();
    cy.contains('APPROVED').should('be.visible');
  });

  it('clerk records cash payment', () => {
    cy.loginAs('CLERK');
    cy.visit('/backoffice/clearances');
    cy.contains('APPROVED').first().click();
    cy.get('[data-testid="record-payment-button"]').click();
    // Fill payment form if needed
    cy.get('[data-testid="mark-paid-button"]').click();
    cy.contains('PAID').should('be.visible');
  });

  it('clerk releases the clearance', () => {
    cy.loginAs('CLERK');
    cy.visit('/backoffice/clearances');
    cy.contains('APPROVED').first().click();
    cy.get('[data-testid="release-button"]').click();
    cy.contains('RELEASED').should('be.visible');
  });

  it('resident downloads PDF without error', () => {
    cy.loginAs('RESIDENT');
    cy.visit('/portal/my-requests');
    cy.contains('RELEASED').first().click();
    cy.get('[data-testid="download-pdf-button"]').click();
    // Verify no error occurred (PDF download triggers)
    cy.get('[role="alert"]').should('not.exist');
  });

});
```

**Implementation notes:**
- This spec depends on state across tests. Use `before()` or sequential test ordering.
- Finding the correct clearance across role switches requires either:
  - Using `cy.intercept()` to capture the clearance ID from the creation response
  - Or using known data attributes/URLs
- Adjust selectors to match actual UI components — add `data-testid` attributes where needed.
- The lifecycle may need the tests to be in a single `it()` block or use shared state via `Cypress.env()`.

---

## Step 8: Cypress E2E Spec — Role Guards

**File:** `frontend/cypress/e2e/role-guards.cy.ts`

**PRD Reference:** Section 4.20

```typescript
describe('Role Guards', () => {

  it('redirects unauthenticated user from /backoffice to /login', () => {
    cy.clearAllLocalStorage();
    cy.clearAllCookies();
    cy.visit('/backoffice/clearances');
    cy.url().should('include', '/login');
  });

  it('redirects unauthenticated user from /portal to /login', () => {
    cy.clearAllLocalStorage();
    cy.clearAllCookies();
    cy.visit('/portal/my-requests');
    cy.url().should('include', '/login');
  });

  it('redirects RESIDENT from /backoffice to /portal', () => {
    cy.loginAs('RESIDENT');
    cy.visit('/backoffice/clearances');
    cy.url().should('include', '/portal');
  });

  it('redirects CLERK from /backoffice/admin to /backoffice/dashboard', () => {
    cy.loginAs('CLERK');
    cy.visit('/backoffice/admin/users');
    cy.url().should('include', '/backoffice/dashboard');
  });

  it('redirects APPROVER from /backoffice/admin to /backoffice/dashboard', () => {
    cy.loginAs('APPROVER');
    cy.visit('/backoffice/admin/users');
    cy.url().should('include', '/backoffice/dashboard');
  });

});
```

---

## Step 9: Create GitHub Actions CI workflow

**File:** `.github/workflows/ci.yml`

```yaml
name: CI

on:
  pull_request:
    branches: [main]
  push:
    branches: [main]

jobs:
  backend-tests:
    name: Backend Tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Run backend tests
        working-directory: backend
        run: ./mvnw test

      - name: Upload Surefire reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: surefire-reports
          path: backend/target/surefire-reports/
          retention-days: 7

  frontend-tests:
    name: Frontend Tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Node.js 20
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        working-directory: frontend
        run: npm ci

      - name: Run Vitest
        working-directory: frontend
        run: npm run test

  e2e-tests:
    name: E2E Tests
    runs-on: ubuntu-latest
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Set up Node.js 20
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Build backend JAR
        working-directory: backend
        run: ./mvnw clean package -DskipTests

      - name: Start test environment
        run: docker compose -f docker-compose.test.yml up -d --wait

      - name: Seed test data
        run: |
          docker compose -f docker-compose.test.yml exec -T postgres \
            psql -U barangay -d barangay_test -f /dev/stdin < frontend/cypress/fixtures/seed.sql

      - name: Install Cypress dependencies
        working-directory: frontend
        run: npm ci

      - name: Run Cypress
        working-directory: frontend
        run: npx cypress run

      - name: Stop test environment
        if: always()
        run: docker compose -f docker-compose.test.yml down -v

      - name: Upload Cypress artifacts
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: cypress-artifacts
          path: |
            frontend/cypress/screenshots/
            frontend/cypress/videos/
          retention-days: 7
```

**Key design decisions:**
- `backend-tests` and `frontend-tests` run in parallel on every PR and push to main
- `e2e-tests` only runs on push to main (too slow/expensive for every PR)
- Testcontainers handles its own PostgreSQL in the backend-tests job — no service container needed
- Cypress artifacts (screenshots + videos) uploaded only on failure
- Maven and npm caches enabled for faster subsequent runs

---

## Step 10: Add data-testid attributes to UI components

Before E2E tests can work reliably, ensure these `data-testid` attributes exist in the frontend:

| Component | Attribute | Location |
|-----------|-----------|----------|
| Navbar user display | `data-testid="navbar"` | Layout component |
| Logout button | `data-testid="logout-button"` | Navbar or sidebar |
| Approve button | `data-testid="approve-button"` | Clearance detail page |
| Reject button | `data-testid="reject-button"` | Clearance detail page |
| Release button | `data-testid="release-button"` | Clearance detail page |
| Record Payment button | `data-testid="record-payment-button"` | Clearance detail page |
| Mark Paid button | `data-testid="mark-paid-button"` | Payment section |
| Download PDF button | `data-testid="download-pdf-button"` | Clearance detail page |
| Error message | `data-testid="error-message"` | Login form |

**Approach:** Only add `data-testid` where CSS class selectors would be fragile. Prefer semantic selectors (`button[type="submit"]`, `[name="email"]`) where stable.

---

## Step 11: Create backend Dockerfile (if not exists)

Check if `backend/Dockerfile` exists. If not, create:

**File:** `backend/Dockerfile`

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Similarly, check `frontend/Dockerfile`. If missing:

**File:** `frontend/Dockerfile`

```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:20-alpine
WORKDIR /app
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./
COPY --from=builder /app/public ./public
EXPOSE 3000
CMD ["npm", "start"]
```

---

## Step 12: End-to-end verification

### Local E2E run:

```bash
# 1. Build backend
cd backend && ./mvnw clean package -DskipTests && cd ..

# 2. Start test environment
docker compose -f docker-compose.test.yml up -d --wait

# 3. Seed data
docker compose -f docker-compose.test.yml exec -T postgres \
  psql -U barangay -d barangay_test < frontend/cypress/fixtures/seed.sql

# 4. Run Cypress
cd frontend && npx cypress run

# 5. Cleanup
cd .. && docker compose -f docker-compose.test.yml down -v
```

### CI verification:

1. Push a test branch and open a PR → verify `backend-tests` and `frontend-tests` jobs run and pass
2. Merge to main → verify `e2e-tests` job runs, Docker Compose starts, Cypress specs pass

---

## Definition of Done

- [ ] Cypress is installed and configured in `frontend/`
- [ ] `cypress/support/e2e.ts` defines `cy.loginAs(role)` custom command
- [ ] `cypress/fixtures/seed.sql` seeds 4 test users (one per role) + 1 resident profile
- [ ] `cypress/e2e/auth.cy.ts` — 6 scenarios pass
- [ ] `cypress/e2e/clearance-lifecycle.cy.ts` — 5 scenarios pass (full DRAFT→RELEASED flow)
- [ ] `cypress/e2e/role-guards.cy.ts` — 5 scenarios pass
- [ ] `docker-compose.test.yml` brings up postgres + backend + frontend with health checks
- [ ] `docker compose -f docker-compose.test.yml up -d --wait` completes without error
- [ ] `.github/workflows/ci.yml` exists with 3 jobs
- [ ] CI `backend-tests` and `frontend-tests` jobs pass on PR
- [ ] CI `e2e-tests` job passes on push to main
- [ ] Cypress failure artifacts are uploaded on CI failure
- [ ] Required `data-testid` attributes are added to frontend components

---

## File Summary

| File | Type |
|------|------|
| `frontend/cypress.config.ts` | Config |
| `frontend/cypress/support/e2e.ts` | Support |
| `frontend/cypress/support/index.d.ts` | Types |
| `frontend/cypress/fixtures/seed.sql` | Seed data |
| `frontend/cypress/e2e/auth.cy.ts` | E2E spec |
| `frontend/cypress/e2e/clearance-lifecycle.cy.ts` | E2E spec |
| `frontend/cypress/e2e/role-guards.cy.ts` | E2E spec |
| `docker-compose.test.yml` | Infrastructure |
| `.github/workflows/ci.yml` | CI pipeline |
| `backend/Dockerfile` | Infrastructure (if missing) |
| `frontend/Dockerfile` | Infrastructure (if missing) |

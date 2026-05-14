# Scheduling System

A full-stack task scheduling application built with **Spring Boot**, **Quartz Scheduler**, **PostgreSQL**, **React**, **TypeScript**, and **Tailwind CSS**.

---

## Table of Contents

1. [Quick Start with Docker Compose](#quick-start)
2. [Local Development](#local-development)
3. [System Architecture](#system-architecture)
4. [API Reference](#api-reference)
5. [Design Decisions](#design-decisions)
6. [Assumptions](#assumptions)
7. [Use of AI Tools](#use-of-ai-tools)

---

## Quick Start

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) ≥ 24
- [Docker Compose](https://docs.docker.com/compose/) ≥ 2.20

### Run the full stack

```bash
git clone <repo-url>
cd SchedulingSystem

docker compose up --build
```

| Service  | URL                         |
|----------|-----------------------------|
| Frontend | http://localhost:3000       |
| Backend API | http://localhost:8080/api |
| PostgreSQL  | localhost:5432 (internal) |

### Stop

```bash
docker compose down          # stop containers
docker compose down -v       # stop and remove volumes (wipes DB)
```

---

## Local Development

### Backend (Java 17 + Maven)

```bash
# Start PostgreSQL first (or use Docker)
docker run -d \
  -e POSTGRES_DB=schedulingdb \
  -e POSTGRES_USER=scheduler \
  -e POSTGRES_PASSWORD=scheduler123 \
  -p 5432:5432 \
  postgres:16-alpine

cd backend
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

### Run backend tests

```bash
cd backend
./mvnw test
```

### Frontend (Node 20 + npm)

```bash
cd frontend
cp .env.example .env     # VITE_API_URL=http://localhost:8080
npm install
npm run dev              # http://localhost:5173
```

---

## System Architecture

```
┌──────────────────────────────────────────────────────────┐
│                     Docker Network                        │
│                                                           │
│  ┌─────────────┐    /api/*     ┌─────────────────────┐   │
│  │   Frontend  │ ──────────▶  │   Backend (8080)     │   │
│  │  React/Vite │  (Nginx proxy) │  Spring Boot + JPA  │   │
│  │  Nginx :80  │              └──────────┬────────────┘   │
│  └─────────────┘                         │ JDBC            │
│                                          ▼                 │
│                                ┌───────────────────┐      │
│                                │  PostgreSQL (5432) │      │
│                                │  - schedulings     │      │
│                                │  - parameter_schemas│     │
│                                │  - QRTZ_* tables   │      │
│                                └───────────────────┘      │
└──────────────────────────────────────────────────────────┘
```

### Backend layers

```
controller/   REST endpoints (HTTP ↔ DTO)
service/      Business logic, parameter validation, Quartz orchestration
repository/   Spring Data JPA interfaces
entity/       JPA entities (Scheduling, ParameterSchema)
dto/          Request / Response objects
job/          Quartz Job implementations (LogJob, EmailJob)
exception/    Global error handling
config/       CORS, data seeding
```

### Frontend structure

```
src/
  api/          Axios wrappers (schedules.ts, parameterSchemas.ts)
  components/   Dashboard, ScheduleTable, ScheduleForm, StatusBadge, ConfirmDialog
  types/        TypeScript interfaces mirroring backend DTOs
  utils/        Date formatters, label helpers
```

---

## API Reference

### Schedules

| Method | Path                          | Description         |
|--------|-------------------------------|---------------------|
| GET    | `/api/schedules`              | List all schedules  |
| GET    | `/api/schedules/{id}`         | Get by ID           |
| POST   | `/api/schedules`              | Create schedule     |
| PUT    | `/api/schedules/{id}`         | Update schedule     |
| DELETE | `/api/schedules/{id}`         | Delete schedule     |
| POST   | `/api/schedules/{id}/pause`   | Pause active job    |
| POST   | `/api/schedules/{id}/resume`  | Resume paused job   |

### Parameter Schemas (read-only)

| Method | Path                                      | Description                  |
|--------|-------------------------------------------|------------------------------|
| GET    | `/api/parameter-schemas`                  | All schemas                  |
| GET    | `/api/parameter-schemas/task/{taskType}`  | Schemas for a specific task  |

### Schedule request body

```json
{
  "taskName": "Daily Digest",
  "taskType": "EMAIL_TASK",
  "scheduleType": "RECURRING_HOURS",
  "scheduleExpression": "24",
  "parameters": {
    "to": "user@example.com",
    "subject": "Daily Report"
  }
}
```

**`scheduleType` values:**

| Value              | Meaning                     | Required fields              |
|--------------------|-----------------------------|------------------------------|
| `ONE_TIME`         | Execute once                | `scheduledAt` (ISO datetime) |
| `RECURRING_MINUTES`| Repeat every N minutes      | `scheduleExpression` (int)   |
| `RECURRING_HOURS`  | Repeat every N hours        | `scheduleExpression` (int)   |
| `WEEKLY`           | Weekly on a specific day    | `dayOfWeek` (0–6), `timeOfDay` (HH:mm) |
| `CRON`             | Quartz cron expression      | `scheduleExpression`         |

---

## Design Decisions

### Quartz JDBC persistence
Quartz is configured with `JobStoreTX` backed by PostgreSQL. This ensures scheduled jobs survive backend restarts and enables future clustering with `isClustered: true`.

### Parameter Schema as a separate entity
Task parameter definitions (`ParameterSchema`) are stored in the database rather than hard-coded in source, enabling dynamic UI rendering and easy extension without code changes.

### Validation at two layers
- **Backend**: `ParameterSchemaService` validates required parameters against the schema before persisting. Returns `400 Bad Request` with a descriptive message.
- **Frontend**: `ScheduleForm` validates required fields before submission to give immediate user feedback without a round-trip.

### Nginx as a reverse proxy
The frontend Nginx container proxies `/api/*` requests to the backend container by name (`backend:8080`). This means the browser never needs to know the backend's host — the frontend Docker image builds with a single `VITE_API_URL` defaulting to the Nginx proxy, keeping the setup portable.

### In-memory Quartz for tests
The test profile (`application-test.yml`) switches Quartz to in-memory mode and uses H2, avoiding a real PostgreSQL dependency in unit/integration tests.

### Pause / Resume
Quartz natively supports pausing and resuming triggers, so those operations are exposed as dedicated endpoints rather than requiring a DELETE + re-create cycle.

---

## Assumptions

1. **Email delivery is simulated.** `EmailJob` logs the sent email to the application log; no SMTP client is wired.
2. **No authentication.** The API is open; a real deployment would add Spring Security + JWT.
3. **Single-instance scheduler.** Quartz clustering is disabled (`isClustered: false`). To scale horizontally, flip that flag and add a second backend instance.
4. **Quartz cron format.** The `CRON` schedule type uses Quartz's 6-part cron syntax (`sec min hour day month weekday`), not the standard 5-part Unix cron.
5. **ONE_TIME fallback.** If `scheduledAt` is omitted for a one-time schedule, the job fires 5 seconds after creation as a safe default.
6. **Day-of-week convention.** Frontend sends `0 = Sunday … 6 = Saturday`; the backend adds 1 before passing to Quartz (which uses `1 = Sunday`).

---

## Use of AI Tools

This project was developed using a multi-model AI workflow to maximise efficiency and maintain full alignment with the assessment requirements.

### Google Gemini
Utilised for the **initial analysis** of the Home Assessment PDF. Gemini was used to:
- Decompose the technical requirements into discrete implementation tasks.
- Assist in architectural planning and technology selection.
- Engineer the "Master Prompt" used to drive the implementation phase.

### Claude Code (Anthropic)
Utilised to **execute the implementation** based on the generated Master Prompt. This included:
- **Boilerplate generation**: Spring Boot project structure, Maven `pom.xml`, Vite/React scaffolding, Tailwind configuration.
- **Configuration**: Quartz JDBC persistence setup, PostgreSQL datasource in `application.yml`, Nginx reverse-proxy config, Docker multi-stage builds.
- **UI scaffolding**: Initial component skeletons for `ScheduleTable`, `ScheduleForm`, and `Dashboard`.

### Human-Driven Logic & Oversight
While AI tools accelerated boilerplate and configuration generation, all core architectural decisions and business logic were manually driven and reviewed, including:

- Designing the **Parameter Schema as a dynamic DB entity** (Bonus Requirement) rather than a hard-coded enum.
- Choosing **Quartz JDBC persistence** over in-memory for production reliability and future clustering support.
- Implementing the **two-layer validation strategy** (Frontend enforcement + Backend enforcement).
- Designing the **test profile** using H2 + in-memory Quartz to eliminate infrastructure dependencies in CI.
- Structuring the Nginx reverse-proxy to fully decouple the frontend build from the backend host.

### Final QA & Testing
All unit and integration tests were executed and verified manually to ensure 100% compliance with the functional requirements. Edge cases (timezone handling, Quartz trigger lifecycle, parameter validation) were identified and resolved through manual review.

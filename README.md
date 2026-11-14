# PlaceMate — Smart Placement Management System

PlaceMate is a role-based placement management platform for students, recruiters, and campus placement teams.

## Repository structure

```text
.
├── src/                         # React + Vite frontend
│   ├── pages/                   # Job, application, interview, notification and admin pages
│   ├── services/api.js          # Axios API clients and JWT handling
│   ├── App.jsx                  # Landing page, authentication, routes and dashboards
│   └── styles.css
├── backend/                     # Java 17+ Spring Boot REST API
│   ├── src/main/java/com/placemate/
│   ├── src/test/java/
│   ├── Dockerfile
│   └── pom.xml
├── .github/workflows/ci.yml     # Frontend and backend CI workflow
├── Dockerfile                   # Frontend production image
├── docker-compose.yml           # MySQL + backend + frontend
├── nginx.conf
└── README.md
```

## Technology stack

- **Frontend:** React, Vite, React Router, Axios, Framer Motion, Recharts, Lucide React
- **Backend:** Java 17, Spring Boot, Spring Security, Spring Data JPA, Bean Validation
- **Database:** MySQL 8+
- **Authentication:** BCrypt password hashing and stateless JWT
- **Deployment:** Docker, Docker Compose, Nginx
- **CI:** GitHub Actions (frontend production build and backend Maven verification)

## Run locally

### Requirements
- Node.js 22+ and npm
- JDK 17+
- Maven 3.9+
- MySQL 8+

### 1. Database
Create a database and configure your local MySQL credentials:

```sql
CREATE DATABASE placemate_db;
```

### 2. Backend
From the repository root:

```bash
cd backend
mvn spring-boot:run
```

The API defaults to `http://localhost:8080`. Configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` as environment variables when needed.

### 3. Frontend
In a second terminal, from the repository root:

```bash
npm install
npm run dev
```

The Vite app defaults to `http://localhost:5173`. The API client defaults to `http://localhost:8080/api`; override it with `VITE_API_BASE_URL` in a local `.env` file if needed.

## Main capabilities implemented in code

| Area | Current implementation |
|---|---|
| Authentication | Student/recruiter registration, login, current-user endpoint, BCrypt, JWT, role-based authorization; public Admin registration blocked |
| Job management | Recruiter-owned create/edit/delete, public approved-job search, filters, Admin approve/reject workflow |
| Applications | Student apply flow, duplicate application protection, student tracker, recruiter applicant review and status updates |
| Student profile | CGPA, graduation year, degree, branch, skills and resume reference |
| Eligibility matching | Deterministic rule-based eligibility and match percentage with explanations; not an AI prediction |
| Resume files | PDF/DOC/DOCX upload (5 MB limit), student download and recruiter download restricted to applicants for the recruiter's own jobs |
| Interviews | Recruiter schedules and reschedules an interview with date/time, duration, mode, meeting link/location and notes; recruiter can mark it completed or cancelled; students can view their schedule |
| Notifications | In-app notification centre, unread count, mark-one-read and mark-all-read; generated for new applications, application status changes and interview events |
| Admin workspace | Student/recruiter lists, recruiter company verification, company summaries, placement metrics, department hiring report and CSV application export |
| Dashboards | Student, recruiter and admin summary cards and recent activity now request API data |
| Deployment | Frontend/backend Dockerfiles, Nginx SPA/API proxy, Docker Compose MySQL persistence and resume volume |
| CI | GitHub Actions workflow configured for frontend build and backend Maven verification |

## Important API groups

All protected routes require `Authorization: Bearer <JWT>`.

- `/api/auth/**` — register, login, current user
- `/api/jobs/**` — public approved job search and details
- `/api/student/**` — profile, job matches, applications, interviews and resume
- `/api/recruiter/**` — recruiter jobs, applicants, application status, interviews and applicant resume access
- `/api/admin/**` — job approvals, user/company management and placement reports
- `/api/notifications/**` — authenticated user's notification centre
- `/api/health` — service health

See [backend/README.md](backend/README.md) for endpoint details and example payloads.

## Docker deployment

Set these environment variables before starting the stack:

- `DB_PASSWORD` — password for the application MySQL user
- `MYSQL_ROOT_PASSWORD` — MySQL root password
- `JWT_SECRET` — unique Base64-encoded secret with at least 32 random bytes
- Optional: `APP_PORT`, `JWT_EXPIRATION_MS`, `CORS_ALLOWED_ORIGINS`

Then run:

```bash
docker compose up --build -d
```

Open `http://localhost` (or the configured `APP_PORT`). MySQL data and uploaded resumes are stored in named Docker volumes. Back up these volumes before upgrades or migration. Nginx adds baseline browser security headers, applies an edge rate limit to login/registration, and permits the configured 5 MB resume uploads. The Compose stack serves HTTP; put it behind a TLS-terminating reverse proxy before exposing it publicly.

## Development log

- **Foundation:** responsive landing page, role-aware dashboard layouts, navigation and base UI.
- **Authentication:** backend registration/login, BCrypt, JWT, role checks and frontend session integration.
- **Jobs:** recruiter job CRUD, public approved listings, Admin review queue and filters.
- **Applications:** student applications, duplicate protection, recruiter applicant lists and status changes.
- **Profiles and matching:** persistent student academic profile and deterministic eligibility/match explanations.
- **Resume files:** authenticated upload/download endpoints and recruiter ownership check.
- **Interviews and notifications:** interview scheduling/status and event-triggered in-app notifications.
- **Admin workspace:** user/company lists, company verification, overview metrics and placement reporting.
- **Operational setup:** CI workflow, health endpoint, Dockerfiles, Compose and Nginx configuration.
- **README refresh:** setup, API groups, current feature scope and deployment notes updated.

## Known limitations and verification status

- GitHub Actions passed on commit `55399ce27830`: frontend production build, backend Maven verification against MySQL (including the health endpoint test), Docker Compose configuration validation, and Nginx configuration validation. Browser-based end-to-end flows and a deployed production environment have not yet been exercised.
- The CI workflow currently includes a frontend build and backend Maven verification plus a backend health smoke test; domain-level automated tests are still limited.
- Interview scheduling, rescheduling and completion/cancellation are implemented; students receive an in-app notification when interview details change.
- Notifications are in-app only. Email/SMS delivery, push notifications and scheduled reminders are not implemented.
- Resume upload validates file size, extension and basic PDF/DOC/DOCX signatures/container structure; downloads are scoped to the owning student and recruiter-owned applicant records. Production hardening should add comprehensive MIME inspection, malware scanning, object storage, retention/deletion controls, and backups.
- Dashboard metrics use live API data, but the landing-page mock preview and sample illustration content remain static.
- Email verification, password reset, audit logs, broader API rate limiting, production HTTPS/reverse-proxy hardening and a full automated security review remain outstanding. Nginx currently applies an edge rate limit to login and registration only.
- JWT is stored in browser local storage in this MVP. Revisit token storage and refresh/revocation strategy before production.

## Security configuration

Never commit real credentials. Use a unique secret for every environment; the JWT fallback in the local configuration is for development only. Configure the allowed CORS origins explicitly for the deployed frontend.

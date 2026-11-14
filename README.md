# PlaceMate — Smart Placement Management System

PlaceMate is a placement-management platform for students, recruiters, and campus placement teams.

## Project structure

```text
PlaceMate/
├── src/                    # React + Vite frontend
│   ├── services/api.js     # Axios client and JWT header handling
│   ├── App.jsx              # Routes, dashboards, and authentication UI
│   └── styles.css
├── backend/                 # Java Spring Boot REST API
│   ├── src/main/java/com/placemate/
│   └── pom.xml
├── .env.example
└── README.md
```

## Technology stack

**Frontend**
- React + Vite
- React Router DOM
- Axios
- Framer Motion
- Recharts
- Lucide React

**Backend**
- Java 17+
- Spring Boot
- Spring Security
- Spring Data JPA
- MySQL
- BCrypt password hashing
- JWT authentication (JJWT)

## Run locally

### 1. Start MySQL

Create a database:

```sql
CREATE DATABASE placemate_db;
```

Make sure your local MySQL service is running and note your username/password.

### 2. Start the backend

Open a terminal in the repository root:

```bash
cd backend
mvn spring-boot:run
```

Backend default URL: `http://localhost:8080`

Configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` as environment variables if your local settings differ. For any shared or production environment, generate a unique Base64 JWT secret (at least 32 random bytes); never use the development fallback.

### 3. Start the frontend

Open a second terminal in the repository root:

```bash
npm install
npm run dev
```

Frontend default URL: `http://localhost:5173`

The frontend API base URL defaults to `http://localhost:8080/api`. To override it, copy `.env.example` to `.env` and set `VITE_API_BASE_URL`.

## Authentication API

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Register a Student or Recruiter |
| POST | `/api/auth/login` | Verify credentials and issue JWT |
| GET | `/api/auth/me` | Return the authenticated user's profile |
| GET | `/api/health` | Backend health check |

Registration accepts Student profile fields (`enrollmentNumber`, `department`) or Recruiter profile fields (`companyName`, optional `companyWebsite`). Public Admin registration is blocked. Passwords are stored as BCrypt hashes; the API response does not expose password hashes.

The frontend stores the returned access token in browser local storage and sends it as `Authorization: Bearer <token>` on API requests. Use HTTPS and a stronger token-storage approach before production deployment.

## Current implementation status

| Area | Status |
|---|---|
| Landing page and responsive dashboard UI | Implemented |
| Student / Recruiter registration UI | Connected to backend API |
| Login UI | Connected to backend API |
| BCrypt password hashing | Implemented in backend |
| JWT generation and request authentication | Implemented in backend |
| Role-based API authorization | Implemented for student/recruiter/admin API path prefixes |
| Current-user endpoint | Implemented |
| Job create/edit/delete and approval APIs | Implemented in code; local testing pending |
| Public approved-job search and filters | Implemented in code; local testing pending |
| Student job applications and duplicate prevention | Implemented in code; local testing pending |
| Recruiter applicant review and status updates | Implemented in code; local testing pending |
| Interviews, notifications and placement analytics APIs | Not implemented yet |
| Live dashboard data | Still uses clearly illustrative frontend fixtures |
| Email verification and password reset | Not implemented |
| Automated build / integration tests | Pending local execution |

## Development log

- **Frontend foundation:** landing page, responsive dashboards, navigation, charts, and UI components.
- **Backend foundation:** Spring Boot, MySQL/JPA configuration, CORS, and health endpoint.
- **Authentication:** registration, login, BCrypt, JWT, current-user endpoint, and role-based API authorization.
- **Frontend integration:** Axios API client, real registration/login requests, token persistence, session restoration, logout, and API error/loading states.
- **Job management:** recruiter-owned job creation, editing and deletion; public approved-job search; admin approve/reject queue; job review notes. React screens are connected to these endpoints.
- **Applications:** student apply action, unique student/job application constraint, student application tracker, recruiter applicant list, and recruiter status updates.

## Important notes

- Admin accounts must be provisioned by the placement team; there is no public Admin signup.
- Job listings and application tracking now use backend APIs. Student profile editing, persistent CGPA/skills and automatic eligibility matching, interviews, notifications, and dashboard metrics still need implementation; some dashboard data remains illustrative.
- Compilation, database connectivity, and end-to-end flows (authentication, job management, and applications) have **not yet been verified on a local machine**. Run both apps locally and test before treating this as production-ready.
- Before production: configure HTTPS, unique secrets, rate limiting, email verification, password recovery, audit logging, and secure token storage.


## Remaining development roadmap

1. **Student profile and eligibility:** persistent education, CGPA, skills, resume upload and automatic matching against job criteria.
2. **Interviews and notifications:** interview scheduling, status notifications and reminders.
3. **Admin workspace:** student/recruiter management, company verification, placement analytics and reports.
4. **Production readiness:** automated backend/frontend tests, validation and security review, deployment configuration, logging and documentation.

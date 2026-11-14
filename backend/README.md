# PlaceMate Backend

Spring Boot REST API for PlaceMate Smart Placement Management.

## Requirements

- JDK 17+
- Maven 3.9+
- MySQL 8+

## Configuration

Create the database:

```sql
CREATE DATABASE placemate_db;
```

Environment variables:

| Variable | Default / purpose |
|---|---|
| `DB_URL` | Local MySQL URL for `placemate_db` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | Empty for local development; set securely |
| `SERVER_PORT` | `8080` |
| `JWT_SECRET` | Development fallback only; replace with a unique Base64 secret (at least 32 random bytes) |
| `JWT_EXPIRATION_MS` | `86400000` (24 hours) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` |
| `PLACEMATE_UPLOAD_DIR` | `uploads/resumes` |

Never commit real credentials or production secrets. Keep resume storage persistent and access-controlled. The local JWT fallback must never be used in production.

## Run locally

```bash
cd backend
mvn spring-boot:run
```

Health check: `GET http://localhost:8080/api/health`

## Authentication

### Student registration — POST /api/auth/register

```json
{
  "fullName": "Aarav Sharma",
  "email": "aarav@example.com",
  "password": "StrongPass123",
  "role": "STUDENT",
  "enrollmentNumber": "CGC2026CS001",
  "department": "Computer Science"
}
```

### Recruiter registration — POST /api/auth/register

```json
{
  "fullName": "Recruiter Name",
  "email": "recruiter@example.com",
  "password": "StrongPass123",
  "role": "RECRUITER",
  "companyName": "Example Technologies",
  "companyWebsite": "https://example.com"
}
```

### Login — POST /api/auth/login

```json
{
  "email": "aarav@example.com",
  "password": "StrongPass123"
}
```

- GET `/api/auth/me` — current authenticated user.
- Passwords are BCrypt-hashed. Successful registration/login returns a JWT and safe user summary.
- Public registration accepts STUDENT and RECRUITER only. Admin accounts must be provisioned through a controlled process.

## Authorization

- `/api/student/**` — STUDENT role
- `/api/recruiter/**` — RECRUITER role
- `/api/admin/**` — ADMIN role
- `/api/auth/**`, `/api/health`, and public approved job GET endpoints are permitted without a JWT.
- `/api/notifications/**` requires any authenticated user; each query/read operation is scoped to the authenticated user.

Send `Authorization: Bearer <JWT>` on protected requests.

## Job management

| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/jobs?q=&location=&type=` | Public approved-job search |
| GET | `/api/jobs/{id}` | Public approved job details |
| POST | `/api/recruiter/jobs` | Recruiter creates a job |
| GET | `/api/recruiter/jobs` | Recruiter lists own jobs |
| PUT | `/api/recruiter/jobs/{id}` | Recruiter updates own job; returns to PENDING |
| DELETE | `/api/recruiter/jobs/{id}` | Recruiter deletes own job |
| GET | `/api/admin/jobs/pending` | Admin pending queue |
| PATCH | `/api/admin/jobs/{id}/review` | Admin approves/rejects |

Only approved and non-expired jobs are publicly discoverable. Recruiters can modify only their own jobs.

Review example:

```json
{"status":"APPROVED","reviewNote":"Reviewed by placement team"}
```

## Applications

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/student/jobs/{jobId}/applications` | Student applies |
| GET | `/api/student/applications` | Student's own application history |
| GET | `/api/recruiter/jobs/{jobId}/applications` | Applicants for recruiter's own job |
| PATCH | `/api/recruiter/applications/{applicationId}/status` | Recruiter updates own applicant |

Statuses: `APPLIED`, `UNDER_REVIEW`, `SHORTLISTED`, `REJECTED`, `HIRED`. Duplicate student/job applications are prevented by a service check and database unique constraint. Recruiters cannot inspect or update another recruiter's applicants.

Example:

```json
{"status":"SHORTLISTED","recruiterNote":"Selected for the technical interview round"}
```

## Student profile and eligibility matching

- GET `/api/student/profile`
- PUT `/api/student/profile`
- GET `/api/student/jobs/matches`

The profile stores CGPA, graduation year, degree, branch, skills and a resume URL or generated uploaded-file key. Eligibility checks minimum CGPA and whether all required skills are present (case-insensitive exact skill-name comparison). The match score is a deterministic rule-based score: 50% CGPA criterion and 50% required-skill coverage. Missing profile data is treated as unknown/not satisfied and explained to the student. This is not an AI prediction.

## Resume upload and access

- POST `/api/student/profile/resume` — multipart form field `file`; accepts PDF, DOC or DOCX with a 5 MB maximum.
- GET `/api/student/resume` — authenticated student downloads their own uploaded resume.
- GET `/api/recruiter/applications/{applicationId}/resume` — recruiter can download the resume only when the application belongs to a job owned by that recruiter.

Files are stored under `PLACEMATE_UPLOAD_DIR` using generated filenames. Keep this directory on persistent storage and back it up. Current validation checks size and filename extension; production deployment must add content/MIME inspection and malware scanning.

## Interviews

- POST `/api/recruiter/applications/{applicationId}/interviews` — schedule for an applicant on a recruiter's own job.
- GET `/api/recruiter/interviews` — list interviews scheduled by the recruiter.
- GET `/api/student/interviews` — list interviews for the authenticated student's applications.
- PATCH `/api/recruiter/interviews/{id}/status?status=COMPLETED` — update status to `SCHEDULED`, `COMPLETED`, or `CANCELLED`.

The schedule request contains `scheduledAt` (future ISO-8601 instant), `durationMinutes` (15–480), `mode` (`ONLINE`, `IN_PERSON`, or `PHONE`), optional `meetingLink`, `location`, and `notes`. Existing interview date/details editing and rescheduling are not exposed yet.

## Notifications

- GET `/api/notifications` — latest 100 notifications for the signed-in user.
- GET `/api/notifications/unread-count`
- PATCH `/api/notifications/{id}/read`
- POST `/api/notifications/read-all`

Notifications are generated for new applications (to the recruiter), application status changes (to the student), interview scheduling and interview status changes (to the student). Access is scoped to the authenticated recipient. Delivery is in-app only; email, SMS, push delivery and scheduled reminders are not implemented.

## Admin workspace and reports

- GET `/api/admin/overview` — live user, company, job and application counts.
- GET `/api/admin/users?role=STUDENT` or `role=RECRUITER`
- GET `/api/admin/companies`
- PATCH `/api/admin/recruiters/{id}/company-verification` with `{"verified":true}`
- GET `/api/admin/applications`
- GET `/api/admin/reports/placements` — distinct hired students grouped by department.

## Configuration and deployment

- `application.yml` supports environment-based database, JWT, CORS, upload directory, server port and multipart size configuration.
- `backend/Dockerfile` builds and runs the API as a non-root user.
- Root `docker-compose.yml` starts MySQL, backend and frontend. It persists MySQL data and resume uploads in named volumes.
- Root `.github/workflows/ci.yml` configures a frontend production build and backend Maven verification with a MySQL service.

## Verification status and remaining hardening

- The repository code was reviewed through GitHub. A successful CI run or local Maven/npm build was not available during this review; compile/runtime correctness is therefore **not yet confirmed**.
- Current automated backend coverage includes a health endpoint smoke test; add domain/service/controller tests for authentication, authorization boundaries, job review, duplicate applications, profile matching, resume access, interviews and notifications.
- Interview rescheduling/editing, email verification, password reset, email/SMS/push notifications, scheduled reminders and audit logs remain future work.
- Add request rate limits, stronger token lifecycle/storage, HTTPS, monitoring, backup/retention policies and full security review before production.

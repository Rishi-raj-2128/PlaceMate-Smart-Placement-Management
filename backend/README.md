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

Files are stored under `PLACEMATE_UPLOAD_DIR` using generated filenames that include the owning student ID. Upload validation checks size, extension, and basic PDF/DOC/DOCX file signatures/container structure. Stored-file downloads verify the owner ID, and recruiter access is limited to applications for jobs owned by that recruiter. Keep this directory on persistent storage and back it up. Production deployment should still add comprehensive MIME inspection and malware scanning.

## Interviews

- POST `/api/recruiter/applications/{applicationId}/interviews` — schedule for an applicant on a recruiter's own job.
- GET `/api/recruiter/interviews` — list interviews scheduled by the recruiter.
- GET `/api/student/interviews` — list interviews for the authenticated student's applications.
- PUT `/api/recruiter/interviews/{id}` — reschedule a scheduled interview or update its date/time and meeting details (recruiter owner only).
- PATCH `/api/recruiter/interviews/{id}/status?status=COMPLETED` — update status to `SCHEDULED`, `COMPLETED`, or `CANCELLED`.

The schedule request contains `scheduledAt` (future ISO-8601 instant), `durationMinutes` (15–480), `mode` (`ONLINE`, `IN_PERSON`, or `PHONE`), optional `meetingLink`, `location`, and `notes`. Rescheduling is available only while an interview is in SCHEDULED status; the student receives an in-app notification when it changes.

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

## Remaining work

The following are not represented as completed features and should be addressed before production use:

- Add automated browser end-to-end tests for complete student, recruiter and admin journeys.
- Add broader API integration coverage for profile updates/job matching, resume upload/download, interview scheduling, notifications and admin reporting.
- Add email verification, password reset and account recovery.
- Replace local-storage JWT handling with a hardened token lifecycle/storage strategy and add refresh/revocation controls.
- Add comprehensive upload MIME inspection, malware scanning, retention/deletion policies and production object storage.
- Add audit logs, broader API rate limiting, monitoring/alerting, database backup/restore drills and deployment HTTPS verification.
- Complete a production security review and verify migrations/operations against a deployed environment.

## Configuration and deployment

- `application.yml` supports environment-based database, JWT, CORS, upload directory, server port and multipart size configuration.
- `backend/Dockerfile` builds and runs the API as a non-root user.
- Root `docker-compose.yml` starts MySQL, backend and frontend. It persists MySQL data and resume uploads in named volumes.
- Root `.github/workflows/ci.yml` configures a frontend production build and backend Maven verification with a MySQL service.

## Verification status and remaining hardening

- GitHub Actions passed on commit `55399ce27830`: Maven verification against MySQL, the backend health endpoint test, frontend production build, Docker Compose validation, and Nginx configuration validation. Full API/browser end-to-end testing and production deployment verification remain outstanding.
- Automated backend coverage includes the health endpoint smoke test, focused `JobService` tests for recruiter-role enforcement, job ownership checks, salary-range validation and invalid admin review decisions; `AuthService` tests for registration normalization/password hashing, Admin registration blocking, duplicate email, required student details and invalid login credentials; and `ApplicationService` tests for duplicate, expired and unapproved applications, recruiter ownership boundaries and status restrictions. `StudentProfileService` tests also cover full and partial skill matches, CGPA eligibility, missing profile data, expired-job filtering, role restrictions and uploaded-resume ownership. `ResumeController` tests cover unsupported extensions, invalid PDF signatures, valid upload/storage, student download, recruiter ownership boundaries and role restrictions. InterviewService tests cover scheduling eligibility, recruiter ownership, rejected applications, student/recruiter listings, rescheduling restrictions, status changes and student notifications. `NotificationService` tests cover recipient persistence, null-recipient handling, per-user listing and unread counts, ownership-scoped mark-read, missing/foreign notification rejection, and mark-all-read behavior. `NotificationController` tests verify authenticated-user scoping for list, unread count, mark-read and mark-all-read endpoints, plus unknown-account rejection. The security filter chain returns HTTP 401 for missing/invalid authentication and HTTP 403 for authenticated users lacking the required role. `AuthSecurityIntegrationTest` exercises the real Spring Security/JWT filter chain and registration endpoints with MockMvc: anonymous and invalid-token rejection, successful student registration/token use, public admin-registration blocking, and student/recruiter role isolation from other dashboards.
- Interview rescheduling is implemented for scheduled interviews. Email verification, password reset, email/SMS/push notifications, scheduled reminders and audit logs remain future work.
- Nginx currently rate limits login and registration; add broader API rate limits, stronger token lifecycle/storage, HTTPS, monitoring, backup/retention policies and a full security review before production.

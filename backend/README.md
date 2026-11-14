# PlaceMate Backend

Spring Boot REST API for PlaceMate.

## Requirements
- JDK 17+
- Maven 3.9+
- MySQL 8+

## Configure MySQL
Create the database (or let the configured local MySQL user create it):

```sql
CREATE DATABASE placemate_db;
```

Set environment variables:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `SERVER_PORT` (default 8080)
- `JWT_SECRET` (unique Base64 secret containing at least 32 random bytes outside local development)
- `JWT_EXPIRATION_MS` (default 86400000 / 24 hours)

Never commit real credentials or production secrets. The configured JWT fallback is for local development only.

## Run
From the repository root:

```bash
cd backend
mvn spring-boot:run
```

Health endpoint: `GET http://localhost:8080/api/health`

## Authentication endpoints

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

Successful registration/login returns a signed JWT, expiry duration, and safe user summary. Password hashes are never returned.

### Current user — GET /api/auth/me
Send `Authorization: Bearer <token>`.

## Authorization
- `/api/student/**` requires STUDENT.
- `/api/recruiter/**` requires RECRUITER.
- `/api/admin/**` requires ADMIN.
- Public registration accepts STUDENT and RECRUITER only. Admin accounts must be provisioned through a controlled process.

## Job management API

- GET /api/jobs?q=&location=&type= — public search of approved, non-expired jobs.
- GET /api/jobs/{id} — approved job details.
- POST /api/recruiter/jobs — create a job (Recruiter JWT required). New jobs start as PENDING.
- GET /api/recruiter/jobs — list the authenticated recruiter's postings.
- PUT /api/recruiter/jobs/{id} — update an owned posting; edits return it to PENDING review.
- DELETE /api/recruiter/jobs/{id} — delete an owned posting.
- GET /api/admin/jobs/pending — list pending postings (Admin JWT required).
- PATCH /api/admin/jobs/{id}/review — approve or reject with an optional review note.

Example review body:

```json
{"status":"APPROVED","reviewNote":"Reviewed by placement team"}
```

Only approved jobs are visible through public browse endpoints. Recruiters can only modify their own postings. Minimum CGPA and required skills are stored as job criteria; personal eligibility calculation awaits persistent student profile fields.

## Application management API

- POST /api/student/jobs/{jobId}/applications — apply to an approved, non-expired job (Student JWT required).
- GET /api/student/applications — list the authenticated student's applications and current statuses.
- GET /api/recruiter/jobs/{jobId}/applications — list applicants for a job owned by the authenticated recruiter.
- PATCH /api/recruiter/applications/{applicationId}/status — update an application status and optional recruiter note.

Supported statuses: APPLIED, UNDER_REVIEW, SHORTLISTED, REJECTED, HIRED. The initial status is APPLIED. Duplicate applications for the same student/job pair are prevented by both a service check and a database unique constraint. Recruiters cannot inspect or update applications for another recruiter's jobs.

Example status update body:

```json
{"status":"SHORTLISTED","recruiterNote":"Selected for the technical interview round"}
```

Student-facing application history includes the job, company, applied date, current status and recruiter note. Student profile fields for CGPA and skills are not yet persisted, so automatic eligibility scoring is not implemented in this milestone.

## Security and remaining work
- Passwords are hashed using BCrypt.
- JWT request authentication is stateless.
- Frontend CORS currently allows local Vite origins only.
- Add email verification, password reset, rate limiting, HTTPS, secure secret storage, and monitoring before public deployment.
- Job posting, recruiter ownership, public approved-job search, and admin review endpoints are implemented in code; local compilation and API tests are pending.
- Student applications, duplicate prevention, recruiter applicant lists, and status updates are implemented in code; local compile and API tests remain pending.
- Interviews, student eligibility calculation, and placement analytics endpoints are not implemented yet.
- Local compilation and API integration tests are still pending.

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

## Security and remaining work
- Passwords are hashed using BCrypt.
- JWT request authentication is stateless.
- Frontend CORS currently allows local Vite origins only.
- Add email verification, password reset, rate limiting, HTTPS, secure secret storage, and monitoring before public deployment.
- Jobs, applications, interviews, and placement analytics endpoints are not implemented yet.
- Local compilation and API integration tests are still pending.

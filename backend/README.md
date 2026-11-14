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

Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and optionally `SERVER_PORT` (default 8080).
Set `JWT_SECRET` to a unique Base64-encoded secret containing at least 32 random bytes. `JWT_EXPIRATION_MS` defaults to 86400000 (24 hours). Never commit real secrets.

## Run
```bash
cd backend
mvn spring-boot:run
```
Health: `GET http://localhost:8080/api/health`

## Authentication API

### Student registration — POST /api/auth/register
```json
{"fullName":"Aarav Sharma","email":"aarav@example.com","password":"StrongPass123","role":"STUDENT","enrollmentNumber":"CGC2026CS001","department":"Computer Science"}
```

### Recruiter registration — POST /api/auth/register
```json
{"fullName":"Recruiter Name","email":"recruiter@example.com","password":"StrongPass123","role":"RECRUITER","companyName":"Example Technologies","companyWebsite":"https://example.com"}
```

### Login — POST /api/auth/login
```json
{"email":"aarav@example.com","password":"StrongPass123"}
```
Returns a signed JWT, expiry duration, and safe user summary. Password hashes are never returned.

### Current account — GET /api/auth/me
Send `Authorization: Bearer <token>`.

## Role authorization
- `/api/student/**` requires STUDENT.
- `/api/recruiter/**` requires RECRUITER.
- `/api/admin/**` requires ADMIN.
- Public registration only accepts STUDENT and RECRUITER. Admin accounts must be provisioned through a controlled process.

## Security status
Passwords use BCrypt and API authentication is stateless with signed JWTs. Before public deployment, add email verification, password recovery, rate limiting, HTTPS, secure secret storage, and operational monitoring. The JWT secret fallback is for local development only.

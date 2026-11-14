# PlaceMate Backend

Spring Boot REST API foundation for PlaceMate.

## Requirements

- JDK 17 or newer
- Maven 3.9+
- MySQL 8+

## Configure MySQL

Create a local database if you prefer to create it manually:

```sql
CREATE DATABASE placemate_db;
```

Set the environment variables before starting the API:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `SERVER_PORT` (optional; defaults to `8080`)

The default local URL creates the database automatically when the MySQL user has permission. Never commit real credentials.

## Run

From the repository root:

```bash
cd backend
mvn spring-boot:run
```

Health check:

```
GET http://localhost:8080/api/health
```

Expected response includes `"status":"UP"`.

## Current scope

This commit establishes the Spring Boot application, MySQL/JPA configuration, development CORS policy, and a health endpoint. Authentication, JWT, role authorization, domain entities, and business APIs will be added in subsequent implementation steps. No real login or user data is implemented yet.

# ShortLink — High-Performance URL Shortener & Analytics Monolith

[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Redis-Cache--Aside-red?logo=redis)](https://redis.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-336791?logo=postgresql)](https://www.postgresql.org/)

An enterprise-grade, high-throughput URL shortening and clickstream analytics platform built as a unified Spring Boot application with **Java 25**, **Spring Boot 4.1.0**, **Redis**, and **PostgreSQL**.

---

## 🏛️ Monolith Architecture

```
                                  [ Client / Browser / Apps ]
                                               │
                                               ▼
                              ┌─────────────────────────────────┐
                              │    ShortLink Spring Boot App    │
                              │           (Port 8080)           │
                              └───────────────┬─────────────────┘
                                              │
               ┌──────────────────────────────┼──────────────────────────────┐
               │                              │                              │
               ▼                              ▼                              ▼
  ┌────────────────────────┐    ┌────────────────────────┐    ┌────────────────────────┐
  │      com.shortlink     │    │      com.shortlink     │    │      com.shortlink     │
  │    .controller / .user │    │       .redirect        │    │       .analytics       │
  │  - Auth & Admin Login  │    │  - Base62 Resolution   │    │  - Click Ingestion     │
  │  - Refresh Token Rot.  │    │  - Cache-Aside (Redis) │    │  - Idempotent Storage  │
  │  - URL CRUD Management │    │  - HTTP 302 Redirect   │    │  - Click Count Queries │
  └────────────────────────┘    └────────────────────────┘    └────────────────────────┘
```

---

## 📦 Package Organization

```text
com.shortlink
├── config          # CORS, Redis, and application properties (JWT, Cookie, Frontend)
├── controller      # REST controllers for Auth and URL management
├── redirect        # Short code resolution and 302 redirection endpoint
├── analytics       # URL click analytics entity, repository, service, and controller
├── user            # User entity, Role enum, and UserRepository
├── entity          # Url and RefreshToken JPA entities
├── repository      # UrlRepository and RefreshTokenRepository
├── dto             # Request/response records, page response, and cache models
├── service         # AuthService, UrlService, RedisCacheService, ShortCodeGenerator, UrlMapper
├── security        # Spring Security config, JWT validation, cookie helpers, and entry point
├── exception       # Global exception handler and domain exception types
└── ShortlinkApplication.java
```

---

## 🚀 Getting Started

### Prerequisites
- **Java 25+**
- **PostgreSQL database**
- **Redis instance**

### Environment Variables
Configure the following environment variables or provide them in `application-dev.yml` / `application-prod.yml`:
- `DATABASE_URL` — PostgreSQL connection string
- `DATABASE_USERNAME` — PostgreSQL username
- `DATABASE_PASSWORD` — PostgreSQL password
- `REDIS_URL` — Redis connection URI (e.g. `redis://localhost:6379`)
- `JWT_SECRET` — 256-bit+ HMAC secret key
- `FRONTEND_URL` — Allowed frontend CORS origin (default: `http://localhost:3000`)
- `BASE_URL` — Base short URL domain (default: `http://localhost:8080`)

### Running Locally
```bash
./mvnw clean spring-boot:run
```

---

## 📡 REST API Summary

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Admin credentials login & JWT issuance | Public |
| `POST` | `/api/auth/refresh` | Rotates refresh token & issues new access token | Public (Cookie) |
| `POST` | `/api/auth/logout` | Revokes current session refresh token | Authenticated |
| `POST` | `/api/auth/logout-all` | Revokes all active user sessions | Authenticated |
| `POST` | `/api/v1/urls` | Creates a new short URL | Public |
| `GET` | `/api/v1/urls` | Retrieves paginated and sorted short URLs | Public |
| `GET` | `/api/v1/urls/{shortCode}` | Retrieves URL details by short code | Public |
| `DELETE` | `/api/v1/urls/{id}` | Deletes short URL by ID and evicts cache | Public |
| `GET` | `/{shortCode}` | 302 redirect to destination URL & tracks click | Public |
| `GET` | `/api/analytics/{shortCode}/clicks` | Retrieves total click count for short code | Role: `ADMIN` |

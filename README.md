# ShortLink — High-Performance URL Shortener & Analytics Platform

[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Redis-Cache--Aside-red?logo=redis)](https://redis.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-336791?logo=postgresql)](https://www.postgresql.org/)

An enterprise-grade, high-throughput URL shortening and clickstream analytics platform built as a unified Spring Boot application with **Java 25**, **Spring Boot 4.1.0**, **Redis**, and **PostgreSQL**.

---

## 🏛️ Application Architecture

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
  │  - Refresh Token Rot.  │    │  - Cache-Aside (Redis) │    │  - Geo / Device / OS   │
  │  - URL CRUD Management │    │  - HTTP 302 Redirect   │    │  - Browser & Referrer  │
  │  - Account Deletion    │    │                        │    │  - Time-Series Charts  │
  └────────────────────────┘    └────────────────────────┘    └────────────────────────┘
```

---

## 📦 Package Organization

```text
com.shortlink
├── config          # CORS, Redis, and application properties (JWT, Cookie, Frontend)
├── constants       # Security endpoint constants and reserved alias lists
├── controller      # REST controllers for Auth, URL management, and Admin
├── redirect        # Short code resolution and 302 redirection endpoint
├── analytics       # Clickstream analytics: entity, repository, service, controller, DTOs
│   ├── controller  # AnalyticsController (user) & AdminAnalyticsController (admin)
│   ├── dto         # TimeSeriesPoint, DistributionItem, LinkAnalyticsResponse, Admin DTOs
│   ├── repository  # ClickEventRepository with geolocation & device queries
│   └── service     # AnalyticsService (per-link) & AdminAnalyticsService (platform-wide)
├── user            # User entity, Role enum, and UserRepository
├── entity          # Url, RefreshToken, PendingRegistration, PasswordResetToken JPA entities
├── repository      # UrlRepository, RefreshTokenRepository, PasswordResetTokenRepository
├── dto             # Request/response records, page response, and cache models
├── service         # AuthService, UrlService, RedisCacheService, AccountDeletionService, EmailService
├── security        # Spring Security config, JWT validation, cookie helpers, and entry point
├── exception       # Global exception handler and domain exception types
└── ShortlinkApplication.java
```

---

## 📊 Link Analytics

Every short link click is captured with rich metadata for detailed analytics:

### Per-Link Analytics (Authenticated Users)
- **Click Time-Series**: Daily click counts over configurable ranges (`7d`, `30d`, `90d`, `all`)
- **Geographic Distribution**: Country-level breakdown of clicks (zero-Redis IP geolocation)
- **Device Breakdown**: Desktop, Mobile, Tablet distribution
- **Browser Distribution**: Chrome, Safari, Firefox, Edge, etc.
- **Referrer Sources**: Direct, social media, search engines, websites

### Platform-Wide Admin Analytics
- **Overview Dashboard**: Total users, total links, total clicks, new users in period
- **Growth Charts**: Time-series for clicks, new links, and new users
- **Top Links**: Highest-performing short links ranked by click count
- **Top Users**: Most active users by link creation and clicks
- **Global Geography**: Platform-wide country distribution
- **Device Analytics**: Platform-wide device type distribution
- **Recent Activity Feed**: Real-time feed of latest clicks, link creations, and user registrations

---

## 🔐 Authentication & Account Management

- **User Registration**: Email verification with single-use tokens and welcome emails
- **Login**: JWT access token + HttpOnly refresh token cookie (rotating)
- **Password Reset**: Forgot password flow with secure reset tokens
- **Change Password**: Authenticated password modification
- **Profile Management**: Update display name
- **Account Deletion**: 7-day grace period with scheduled purge of all user data
- **Cancel Deletion & Restore**: Users can cancel pending deletion on login via confirmation dialog
- **Multi-Session Support**: Independent refresh tokens per device/session
- **Logout / Logout All**: Revoke current or all active sessions
- **Adaptive Email Templates**: Dark/light mode support for all transactional emails

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
- `MAIL_HOST` — SMTP host
- `MAIL_USERNAME` — SMTP username
- `MAIL_PASSWORD` — SMTP password

### Running Locally
```bash
./mvnw clean spring-boot:run
```

---

## 📡 REST API Summary

### Authentication (`/api/auth`)
| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | User registration with email verification | Public |
| `GET` | `/api/auth/verify-email?token=` | Email verification via token | Public |
| `POST` | `/api/auth/resend-verification` | Resend verification email | Public |
| `POST` | `/api/auth/login` | Credentials login & JWT issuance | Public |
| `POST` | `/api/auth/cancel-deletion` | Cancel pending deletion & restore account | Public |
| `POST` | `/api/auth/refresh` | Rotates refresh token & issues new access token | Public (Cookie) |
| `POST` | `/api/auth/forgot-password` | Send password reset email | Public |
| `POST` | `/api/auth/reset-password` | Reset password with token | Public |
| `GET` | `/api/auth/me` | Get current user profile | Authenticated |
| `PUT` | `/api/auth/profile` | Update user profile name | Authenticated |
| `PUT` | `/api/auth/change-password` | Change password | Authenticated |
| `POST` | `/api/auth/request-deletion` | Request 7-day account deletion | Authenticated |
| `DELETE` | `/api/auth/account` | Alias for account deletion request | Authenticated |
| `POST` | `/api/auth/logout` | Revoke current session refresh token | Authenticated |
| `POST` | `/api/auth/logout-all` | Revoke all active user sessions | Authenticated |

### URL Management (`/api/v1/urls`)
| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/urls` | Create a new short URL | Authenticated |
| `GET` | `/api/v1/urls` | Paginated and sorted short URLs | Authenticated |
| `GET` | `/api/v1/urls/{shortCode}` | Get URL details by short code | Public |
| `DELETE` | `/api/v1/urls/{id}` | Delete short URL and evict cache | Authenticated |
| `GET` | `/{shortCode}` | 302 redirect & click tracking | Public |

### User Analytics (`/api/analytics`)
| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/analytics/overview` | User dashboard overview (total links, clicks) | Authenticated |
| `GET` | `/api/analytics/clicks` | User click time-series | Authenticated |
| `GET` | `/api/analytics/urls/{id}` | Individual link analytics summary | Authenticated |
| `GET` | `/api/analytics/urls/{id}/clicks` | Link click time-series | Authenticated |
| `GET` | `/api/analytics/urls/{id}/geography` | Link geographic distribution | Authenticated |
| `GET` | `/api/analytics/urls/{id}/devices` | Link device breakdown | Authenticated |
| `GET` | `/api/analytics/urls/{id}/browsers` | Link browser distribution | Authenticated |
| `GET` | `/api/analytics/urls/{id}/referrers` | Link referrer sources | Authenticated |
| `GET` | `/api/analytics/{shortCode}/clicks` | Simple click count by short code | Authenticated |

### Admin Analytics (`/api/admin/analytics`)
| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/analytics/overview` | Platform-wide overview metrics | ADMIN |
| `GET` | `/api/admin/analytics/growth` | Growth time-series (clicks, users, links) | ADMIN |
| `GET` | `/api/admin/analytics/top-links` | Top performing links | ADMIN |
| `GET` | `/api/admin/analytics/top-users` | Most active users | ADMIN |
| `GET` | `/api/admin/analytics/geography` | Platform geographic distribution | ADMIN |
| `GET` | `/api/admin/analytics/devices` | Platform device distribution | ADMIN |
| `GET` | `/api/admin/analytics/activity` | Recent platform activity feed | ADMIN |

---

## 📄 License
This project is open-source and available under the **MIT License**.

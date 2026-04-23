# 🔗 Referral Backend

A **production-grade job referral platform backend** that manages the complete lifecycle of job referrals — from post creation and referral requests to real-time chat and reputation tracking.

> Built with Java 17 + Spring Boot · PostgreSQL · Redis · WebSocket (STOMP) · GitHub OAuth2 · Bucket4j · Flyway

---

## 📌 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Features](#features)
- [API Reference](#api-reference)
- [Database Schema](#database-schema)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Running Locally](#running-locally)
- [Project Status](#project-status)

---

## Overview

**Referral Backend** solves the real-world problem of connecting job seekers with employees who can refer them inside companies.

### The Core Flow

```
User Signs Up (GitHub OAuth2)
        │
        ▼
  Complete Onboarding
  (role, experience, company verification)
        │
        ▼
  Create a Post
  ┌─────┴──────┐
OFFER        REQUEST
(I can refer) (I need referral)
        │
        ▼
  Another User Applies
        │
        ▼
  Referrer Accepts / Rejects
        │
        ▼ (on accept)
  Chat Unlocked → Real-time Messaging
        │
        ▼
  Referral Submitted → Feedback & Reputation
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Database | PostgreSQL |
| Caching | Redis |
| Real-time | WebSocket — STOMP Protocol |
| Auth | GitHub OAuth2 + JWT (Access + Refresh tokens) |
| Rate Limiting | Bucket4j |
| DB Migrations | Flyway |
| Build Tool | Maven |
| Containerization | Docker (Basic) |
| Docs | Swagger / OpenAPI |
| Logging | SLF4J + Logback |

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│                   Client / Frontend              │
└──────────────────────┬──────────────────────────┘
                       │ HTTP / WebSocket
┌──────────────────────▼──────────────────────────┐
│              Spring Boot Application             │
│                                                  │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │   Auth   │  │   REST   │  │   WebSocket   │  │
│  │ (OAuth2/ │  │ Controllers│  │  (STOMP Chat) │  │
│  │  JWT)    │  │          │  │               │  │
│  └──────────┘  └──────────┘  └───────────────┘  │
│                                                  │
│  ┌──────────────────────────────────────────┐   │
│  │              Service Layer               │   │
│  │  Posts │ Referrals │ Chat │ Notifications│   │
│  │  Users │ Feed      │ Feedback            │   │
│  └──────────────────────────────────────────┘   │
│                                                  │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │   JPA /  │  │  Redis   │  │   Bucket4j    │  │
│  │ Hibernate│  │  Cache   │  │ Rate Limiter  │  │
│  └──────────┘  └──────────┘  └───────────────┘  │
└──────────────────────┬──────────────────────────┘
                       │
          ┌────────────▼────────────┐
          │       PostgreSQL        │
          │   (Flyway migrations)   │
          └─────────────────────────┘
```

---

## Features

### 🔐 Authentication
- GitHub OAuth2 login — no password management
- JWT access + refresh token flow
- Secure session invalidation on logout
- Token refresh endpoint for seamless re-authentication

### 👤 User & Onboarding
- Multi-step onboarding (role, experience, company)
- Company verification via OTP email + document upload
- Resume upload, update, and delete
- Public profile view with referrer reputation score

### 📝 Posts
- Create OFFER (I can refer) or REQUEST (I need referral) posts
- Edit and soft-delete — owner only
- View count tracking per post
- Browse by type: Offers tab / Requests tab
- Search by company, role, and experience range
- Cursor-based pagination (createdAt + id) — no slow OFFSET queries

### 🤝 Referrals
- Apply or refer based on post type — single endpoint handles both
- Referrer can accept or reject incoming requests
- Applicant can withdraw their own request
- Optimistic locking to prevent race conditions on referral limits
- Idempotency keys to prevent duplicate submissions

### 💬 Real-time Chat
- WebSocket (STOMP) — unlocked only after referral is accepted
- Per-message status tracking: sent → delivered → read
- Typing indicators
- Unread count badge per chat
- Mark referral as submitted directly from chat

### 🔔 Notifications
- Event-driven: triggered on new referral, accept/reject, new message
- Per-notification read/unread state
- Bulk mark-all-read
- Unread count for bell badge

### ⭐ Feedback & Reputation
- Job seekers can rate referrers after referral completion
- Referrer reputation score visible on public profile
- Encourages platform accountability and quality referrals

### ⚡ Performance & Reliability
- Redis caching — first-page feed cached with selective eviction
- Multi-tier rate limiting via Bucket4j (separate limits for auth / chat / referrals)
- User-based + IP-based fallback for rate limiting
- Database indexing and query optimization for low latency under load
- Flyway versioned migrations — no production schema mismatch

---

## API Reference

Full Swagger docs available at `/swagger-ui.html` when running locally.

### Auth — `/api/v1/auth`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/github` | Initiate GitHub OAuth2 login |
| POST | `/logout` | Logout — invalidates current session |
| GET | `/me` | Get currently authenticated user |
| POST | `/refresh` | Refresh access token using refresh token |

### Posts — `/api/v1/posts`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create a post |
| GET | `/{postId}` | Get a single post — increments view count |
| PUT | `/{postId}` | Edit a post — owner only, partial update |
| DELETE | `/{postId}` | Delete a post — owner only, soft delete |
| GET | `/me` | My posts — management view |
| GET | `/offers` | Referrer tab — OFFER posts |
| GET | `/requests` | Seeker tab — REQUEST posts |
| GET | `/search` | Search by company, role, experience range |
| GET | `/user/{authorId}` | Posts by user — public profile view |

### Referrals — `/api/v1/referrals`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/{postId}` | Apply or Refer based on post type |
| PATCH | `/{referralId}/accept` | Accept referral request — referrer only |
| PATCH | `/{referralId}/reject` | Reject referral request — referrer only |
| PATCH | `/{referralId}/withdraw` | Withdraw request — applicant only |
| GET | `/incoming` | Incoming referral requests — referrer view |
| GET | `/my` | My referral requests — job seeker view |
| POST | `/{referralId}/feedback` | Submit referral feedback |
| GET | `/reputation/{referrerId}` | Get referrer reputation score |

### Chat — `/api/v1/chats`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | My chat inbox — all active chats with unread counts |
| POST | `/referral/{referralId}` | Open chat for an accepted referral |
| GET | `/{chatId}/messages` | Get messages for a chat |
| POST | `/{chatId}/messages` | Send a message |
| PATCH | `/{chatId}/read` | Mark all messages in a chat as read |
| PATCH | `/{chatId}/mark-referred` | Mark referral as submitted |
| GET | `/unread-count` | Total unread message count — notification badge |

### Notifications — `/api/v1/notifications`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Get all notifications — latest first |
| GET | `/unread` | Get unread notifications only |
| GET | `/unread-count` | Unread count — for bell badge |
| PATCH | `/{notificationId}/read` | Mark a single notification as read |
| PATCH | `/read-all` | Mark all notifications as read |

### Feed — `/api/v1/feed`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Get personalized feed |

### Users — `/api/v1/users`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/{username}` | Get public profile |
| GET | `/me` | Get my profile |
| PUT | `/me` | Update profile |
| DELETE | `/me` | Delete account |
| POST | `/me/onboard` | Complete onboarding |
| GET | `/me/onboarding-status` | Get onboarding status |
| POST | `/me/resume` | Upload resume |
| DELETE | `/me/resume` | Delete resume |
| GET | `/me/resume/url` | Get my resume URL |
| GET | `/{username}/resume/url` | Get public resume URL |
| POST | `/me/experience` | Add work experience |
| POST | `/me/company/send-otp` | Send company verification OTP |
| POST | `/me/company/verify` | Verify company OTP |
| POST | `/me/company/document` | Upload verification document |
| POST | `/me/company/public` | Add public profile proof |
| GET | `/me/verifications` | Get verification history |
| GET | `/referrers` | Find referrers |
| GET | `/search` | Search users |

---

## Database Schema

### Core Entities

```
users
  id, username, email, role, bio, company,
  verified, reputation_score, created_at

posts
  id, author_id, type (OFFER/REQUEST),
  company, role, experience_required,
  description, view_count, deleted, created_at

referrals
  id, post_id, requester_id, referrer_id,
  status (PENDING/ACCEPTED/REJECTED/WITHDRAWN/SUBMITTED),
  version (optimistic lock), idempotency_key, created_at

chats
  id, referral_id, requester_id, referrer_id,
  created_at

messages
  id, chat_id, sender_id, content,
  status (SENT/DELIVERED/READ), created_at

notifications
  id, user_id, type, reference_id,
  message, read, created_at

feedback
  id, referral_id, reviewer_id, referrer_id,
  rating, comment, created_at
```

---

## Getting Started

### Prerequisites

- Java 17+
- PostgreSQL 14+
- Redis 7+
- Maven 3.8+
- Docker (optional)

### Clone the Repository

```bash
git clone https://github.com/AditShh-git/referral-backend.git
cd referral-backend
```

---

## Environment Variables

Create a `.env` file or set the following in `application.yml`:

```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/referral_db
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=your_jwt_secret_key
JWT_ACCESS_EXPIRY=900000
JWT_REFRESH_EXPIRY=604800000

# GitHub OAuth2
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret

# App
APP_BASE_URL=http://localhost:8080
FRONTEND_URL=http://localhost:3000
```

---

## Running Locally

### Option 1 — Maven

```bash
# Start PostgreSQL and Redis first, then:
mvn spring-boot:run
```

### Option 2 — Docker Compose

```bash
docker-compose up --build
```

### Swagger UI

```
http://localhost:8080/swagger-ui.html
```

---

## Project Status

> 🚧 **Active Development** — Core flows are functional. Test coverage in progress.

| Module | Status |
|--------|--------|
| Auth (OAuth2 + JWT) | ✅ Complete |
| Posts (CRUD + Search) | ✅ Complete |
| Referrals (lifecycle) | ✅ Complete |
| Real-time Chat (WebSocket) | ✅ Complete |
| Notifications | ✅ Complete |
| Feed + Redis Caching | ✅ Complete |
| Rate Limiting (Bucket4j) | ✅ Complete |
| Company Verification | ✅ Complete |
| Feedback & Reputation | ✅ Complete |
| Unit & Integration Tests | 🔄 In Progress |
| Deployment (AWS EC2) | 🔄 Planned |

---

## Author

**Aditya Kumar**
[GitHub](https://github.com/AditShh-git) · [LinkedIn](https://linkedin.com/in/aditya-kumar-5003b2259/) · [Email](mailto:ak2057338@gmail.com)

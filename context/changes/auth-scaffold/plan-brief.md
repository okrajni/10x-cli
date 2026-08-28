# Auth Scaffold — Plan Brief

> Full plan: `context/changes/auth-scaffold/plan.md`

## What & Why

Build email/password registration and login endpoints for the household task management system. Users need separate, authenticated accounts to distinguish task assignments and track contributions. This is critical path — every downstream feature (household creation, task assignment, Telegram integration) depends on authenticated users.

## Starting Point

Spring Boot 4.1.1 is initialized with PostgreSQL, Hibernate/JPA, and build tooling in place. No Spring Security, User entity, or authentication code exists yet. The frontend (React) will consume `/auth/register` and `/auth/login` endpoints once they're available.

## Desired End State

Users can register with email + password and log in to receive a JWT token. Each request includes the token via `Authorization: Bearer <token>` header. Tokens are stateless (no server session storage) and expire after 24 hours. Users can maintain multiple concurrent sessions across devices, with optional session tracking (device, IP, login time).

---

## Key Decisions Made

| Decision                       | Choice            | Why  |
| ------------------------------ | ----------------- | ---- |
| Session strategy               | JWT tokens, stateless | Scales horizontally, no session store needed, natural fit for REST/mobile |
| Email verification             | Optional, not blocking login | Supports <5 min onboarding guardrail; email validation happens later in S-01 |
| Password rules                 | 8+ chars, no complexity | Modern security guidance (NIST); reduces user friction |
| Error messages                 | Specific but privacy-aware | Usable UX ("Invalid email or password") without leaking user enumeration |
| Testing approach               | Mix of unit + integration | Fast feedback on crypto logic; confidence in end-to-end flow |
| Concurrent sessions            | Allowed, with metadata tracking | Multi-device UX; users can see active sessions and revoke later |
| Rate limiting                  | 5 login attempts per 15 min | Blocks brute-force; threshold loose enough not to block legitimate users |

---

## Scope

**In scope:**
- User entity, repository, password hashing (BCrypt)
- Spring Security configuration and JWT filter
- Registration endpoint with email/password validation
- Login endpoint with session metadata tracking
- Error handling (401, 409, 422 status codes with clear messages)
- Rate limiting on login/registration
- Unit + integration test suite (~80% coverage)

**Out of scope:**
- Email verification requirement (email sending exists in S-01)
- OAuth or third-party identity providers
- Multi-factor authentication or TOTP
- Password reset flow (v1.1)
- Session persistence across server restarts
- Advanced JWT features (audience validation, claim filtering)

---

## Architecture / Approach

**Stateless JWT + Metadata Tracking:**
- Server generates short-lived JWT on login (24-hour expiry)
- Each request includes token in Authorization header
- Server validates token cryptographically (no DB lookup required)
- Optional `UserSession` entity tracks login context (device, IP, timestamp) for user visibility
- Rate limiting via in-memory counter (5 failed logins per email+IP → 15-min lockout)

**Request/Response Contract:**
```
POST /auth/register
{ "email": "user@example.com", "password": "password123" }
↓
200 OK
{ "userId": "uuid", "email": "...", "token": "jwt...", "expiresAt": "ISO-8601" }

POST /auth/login
{ "email": "user@example.com", "password": "password123" }
↓
200 OK
{ same as registration }
```

**Error Responses:**
- 401 Unauthorized: invalid credentials (unified message: "Invalid email or password")
- 409 Conflict: email already registered
- 422 Unprocessable Entity: validation failed (short password, invalid email format)
- 429 Too Many Requests: rate limit exceeded (Retry-After header included)

---

## Phases at a Glance

| Phase     | What it delivers       | Key risk                  |
| --------- | ---------------------- | ------------------------- |
| 1. Foundation | User entity, Spring Security, JWT utils | JWT secret management; password encoder setup |
| 2. Registration | /auth/register endpoint, user creation | Email validation edge cases; duplicate detection race condition |
| 3. Login & Sessions | /auth/login endpoint, session metadata | Token expiry handling; concurrent session consistency |
| 4. Hardening | Rate limiting, logout, security tests | Rate limiter state consistency; brute-force threshold tuning |

**Prerequisites:** 
- Spring Security and JWT library (io.jsonwebtoken) added to pom.xml
- JWT_SECRET environment variable available (generated on deployment)
- PostgreSQL database connection working (already configured)

**Estimated effort:** ~2-3 development sessions across 4 phases (~16-24 hours for thorough implementation + testing)

---

## Open Risks & Assumptions

- **JWT secret management:** Must be set as environment variable before deployment; no fallback should be provided. Application will fail to start without it.
- **Password hashing latency:** BCrypt with 10 salt rounds takes ~100-200ms per login. Acceptable for MVP; monitor if users complain about slow signup.
- **In-memory rate limiter:** Doesn't work across multiple server instances. If multi-instance deployment is needed, switch to database-backed rate limiter or Redis.
- **Session metadata tracking:** Optional update to `last_activity_at` on every request adds DB latency. Recommend batching or skipping for MVP.
- **Token revocation on logout:** If implemented, requires maintaining a token blacklist (DB or Redis). Simpler approach: let expired tokens expire naturally.

---

## Success Criteria (Summary)

- Users can register with email + password and immediately log in (no email verification blocking)
- Users receive a valid JWT token on login that they can use on protected endpoints
- Invalid credentials return 401 with message "Invalid email or password" (no user enumeration)
- Duplicate email registration returns 409
- Rate limiting prevents brute-force: 5 failed logins → 429 on 6th attempt
- Multi-device login creates separate session records in database
- All endpoints respond in <500ms under normal load
- >80% test coverage for auth-related code

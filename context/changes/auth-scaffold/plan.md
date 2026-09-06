# Auth Scaffold — Email/Password Registration and Login

## Overview

Build email/password registration and login endpoints for the done yet? household task management system. Users will register with email + password, log in to receive JWT tokens, and maintain concurrent sessions across multiple devices. This foundation is critical path — all downstream user-facing features (household creation, task assignment, Telegram integration) depend on authenticated users.

## Current State Analysis

**Spring Boot Infrastructure:**
- Spring Boot 4.1.1 with Java 21 is initialized and running
- PostgreSQL with Hibernate/JPA is configured (datasource, dialect, schema auto-creation via `create-drop`)
- Spring Data JPA and PostgreSQL driver are in pom.xml
- Test framework (JUnit 5) is set up with basic `@SpringBootTest` test case
- No Spring Security dependency; no authentication code exists

**Frontend:**
- React UI exists but has no registration or login forms yet (F-04 is a parallel workstream)
- Frontend will consume `/auth/register` and `/auth/login` endpoints

**Database:**
- No user entity or schema exists yet
- Hibernate `ddl-auto=create-drop` will auto-generate schema from entities

**Dependency Gap:**
- Spring Security must be added to pom.xml (it's not currently included)

### Key Discoveries:

- **Spring Security not yet in pom.xml** — adding `spring-boot-starter-security` is the first step
- **Password hashing:** Spring Security's `BCryptPasswordEncoder` is the standard; use 10 salt rounds (default)
- **JWT handling:** No built-in library; use `io.jsonwebtoken:jjwt` (0.12+) or similar, OR implement via Spring Security's token-based auth
- **Session tracking:** Will need a `UserSession` entity to track concurrent logins (device, IP, login time)
- **Rate limiting:** Can be implemented via `io.github.bucket4j:bucket4j-core` or Spring Cloud's rate limiter
- **Error handling:** Spring already maps HTTP status codes; auth endpoints will use 401 (unauthorized), 422 (validation), 409 (conflict/duplicate email)

## Desired End State

After this plan is complete:

1. Users can register with email + password via `POST /auth/register`
   - Validation: email format, password ≥8 characters
   - Response: JWT token, user ID, token expiry time
   - User is immediately logged in (no email verification required for login)

2. Users can log in with email + password via `POST /auth/login`
   - Response: JWT token, user ID, token expiry time
   - Each login creates a session record (device tracking)

3. Frontend can use tokens by sending `Authorization: Bearer <token>` on protected endpoints

4. Tokens are validated on every request; expired or invalid tokens return 401

5. Users can see their active sessions and revoke individual sessions (optional; Phase 4 candidate)

**Verification:**
- All automated tests pass: `mvn test`
- Endpoints are callable via curl/Postman with valid request/response
- Tokens decode correctly and contain user claims
- Expired tokens are rejected
- Duplicate email registration is rejected with 409

## What We're NOT Doing

- Email verification requirement (email is sent/validated later in S-01; immediate access post-registration)
- OAuth or third-party identity providers (email/password only)
- Multi-factor authentication or TOTP
- Password reset flow (scope: MVP, add in v1.1)
- Rate limiting via Redis (in-memory or database-backed rate limiter acceptable)
- Session persistence across server restarts (sessions can be lost on redeploy)
- Advanced JWT features (claims filtering, audience validation beyond user ID)

## Implementation Approach

**Technology Stack:**
- **Authentication:** Spring Security + JWT (stateless, no server sessions)
- **Password hashing:** `BCryptPasswordEncoder` (Spring Security built-in)
- **JWT library:** `io.jsonwebtoken:jjwt` version 0.12.3 (latest)
- **Rate limiting:** In-memory map-based or simple database counts (keep it simple for MVP)
- **Testing:** JUnit 5 + MockMvc for integration tests, standard Spring `@WebMvcTest` for controller tests

**Architectural Decision:**
- **Stateless JWT:** Server does not maintain session state (no HttpSession). Each request includes the token, which is validated cryptographically.
- **Session metadata tracking:** Optional `UserSession` entity stores login context (device, IP, token ID) for user visibility and optional session revocation later.
- **Error messages:** Unified 401 for invalid credentials (no email enumeration); 422 for validation errors (missing fields); 409 for duplicate email.

**Request/Response Contract:**

```
POST /auth/register
{
  "email": "user@example.com",
  "password": "securePassword123"
}
↓
200 OK
{
  "userId": "abc-123",
  "email": "user@example.com",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": "2026-08-29T10:30:00Z"
}

POST /auth/login
{
  "email": "user@example.com",
  "password": "securePassword123"
}
↓
200 OK
{
  "userId": "abc-123",
  "email": "user@example.com",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": "2026-08-29T10:30:00Z"
}

POST /auth/logout (Phase 4)
{
  "sessionId": "session-456"
}
↓
204 No Content
```

## Critical Implementation Details

**JWT Secret & Expiry:** Store JWT secret in environment variable `JWT_SECRET` (generated via `openssl rand -hex 32`). Token expiry: 24 hours for access token, refresh token (if using hybrid) can be 7 days.

**Password Storage:** Always hash with BCryptPasswordEncoder before storing. Never store plaintext or salted-but-not-hashed passwords. The `@EntityListeners` pattern is overkill; hash in the service layer when `User.setPassword()` is called.

**Concurrent Sessions:** Multiple tokens allowed per user (multi-device). Store minimal metadata in `UserSession` entity (user_id, token_jti, device_name, ip_address, created_at, last_activity_at). On each request, optionally update `last_activity_at` for session tracking (performance trade-off: every request hits DB vs manual session invalidation).

---

## Phase 1: User Entity & Spring Security Foundation

### Overview

Create the `User` entity, Spring Security configuration, and JWT infrastructure. By end of phase, the application can encode passwords, validate JWT tokens, and reject unauthorized requests with proper error responses.

### Changes Required:

#### 1. Add Spring Security & JWT Dependencies

**File:** `pom.xml`

**Intent:** Include Spring Security framework and JWT library in the project.

**Contract:** Add two `<dependency>` blocks in the `<dependencies>` section:
1. `spring-boot-starter-security` version matching Spring Boot 4.1.1
2. `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson` version 0.12.3

(No code snippet needed — standard Maven dependency declarations.)

#### 2. Create User Entity

**File:** `src/main/java/com/example/doneyet/domain/User.java`

**Intent:** Model a user with email, hashed password, and audit timestamps. This entity is the primary key for all downstream features (households, tasks, assignments).

**Contract:** JPA entity with:
- `@Entity @Table(name = "users")`
- Fields: `id` (UUID primary key), `email` (unique, not null), `passwordHash` (not null, bcrypt encoded), `createdAt`, `updatedAt`
- Getters, setters, hashCode/equals based on `id`
- No setPassword() method in the entity; password is hashed in the service layer

```java
@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  // getters, equals/hashCode
}
```

#### 3. Create UserRepository

**File:** `src/main/java/com/example/doneyet/repository/UserRepository.java`

**Intent:** Provide database access for user queries (find by email, find by ID).

**Contract:** Spring Data JPA repository interface extending `JpaRepository<User, UUID>` with custom query methods:
- `Optional<User> findByEmail(String email)`
- `boolean existsByEmail(String email)`

#### 4. Create JwtTokenProvider

**File:** `src/main/java/com/example/doneyet/security/JwtTokenProvider.java`

**Intent:** Centralize JWT creation, validation, and claim extraction. This is the single source of truth for token format and expiry.

**Contract:** Service class with methods:
- `String generateToken(UUID userId, String email)` — creates a signed JWT with user claims
- `UUID extractUserId(String token)` — extracts user ID from token; throws if invalid/expired
- `boolean isTokenValid(String token)` — true if token is valid and not expired
- Private method to load JWT secret from `JWT_SECRET` environment variable (or throw if missing)

**Token payload:**
```json
{
  "sub": "<user-id-uuid>",
  "email": "<email>",
  "iat": <issued-at>,
  "exp": <expiry-24-hours-later>
}
```

#### 5. Create Spring Security Configuration

**File:** `src/main/java/com/example/doneyet/config/SecurityConfig.java`

**Intent:** Wire Spring Security into the application: password encoder, authentication provider, JWT filter, and security filter chain.

**Contract:** `@Configuration` class with beans:
- `@Bean PasswordEncoder passwordEncoder()` — returns `new BCryptPasswordEncoder(10)`
- `@Bean SecurityFilterChain filterChain(HttpSecurity http)` — configure:
  - CSRF disabled (stateless API)
  - Session creation policy: `STATELESS`
  - `/auth/register` and `/auth/login` are public (permit all)
  - All other endpoints require authentication
  - Add custom JWT filter before `UsernamePasswordAuthenticationFilter`
  - Exception handling: 401 for auth failures

#### 6. Create JWT Authentication Filter

**File:** `src/main/java/com/example/doneyet/security/JwtAuthenticationFilter.java`

**Intent:** Intercept every request, extract and validate JWT token, populate Spring Security context so endpoints know who is authenticated.

**Contract:** Extends `OncePerRequestFilter`, override `doFilterInternal()`:
- Extract Bearer token from `Authorization` header
- Validate token via `JwtTokenProvider`
- Extract user ID and load User entity from database
- Set Spring Security `Authentication` context
- On invalid/missing token: pass through (let `@PreAuthorize` or controller handle it)

#### 7. Update Application Properties

**File:** `src/main/resources/application.properties`

**Intent:** Add JWT secret configuration and adjust Spring Security defaults.

**Contract:** Add properties:
- `jwt.secret=${JWT_SECRET:dev-secret-key-change-in-production}` (environment override)
- `server.servlet.session.tracking-modes=NONE` (disable default session tracking; we use JWT)

### Success Criteria:

#### Automated Verification:

- [ ] `mvn compile` succeeds (no syntax or dependency errors)
- [ ] `mvn test` runs existing test suite without failures
- [ ] Spring Security autoconfiguration is triggered: `@EnableWebSecurity` implied or explicitly set
- [ ] JwtTokenProvider unit tests pass: token creation, validation, expiry checks
- [ ] SecurityFilterChain bean is created without errors

#### Manual Verification:

- [ ] Start the application: `mvn spring-boot:run`
- [ ] Access protected endpoint (e.g., `GET /api/household`): receive 401 (unauthorized)
- [ ] Send request with invalid JWT: receive 401
- [ ] Send request with expired JWT: receive 401
- [ ] No runtime errors in logs related to missing beans or filter configuration

---

## Phase 2: Registration Endpoint

### Overview

Implement `/auth/register` endpoint and supporting service. Users can register with email + password, passwords are hashed, and users receive a JWT token immediately (no email verification required for login).

### Changes Required:

#### 1. Create Registration DTOs

**File:** `src/main/java/com/example/doneyet/dto/AuthDto.java`

**Intent:** Define request and response shapes for auth endpoints (registration and login).

**Contract:** Inner classes or separate classes:
- `RegisterRequest`: fields `email` (string), `password` (string)
- `LoginRequest`: fields `email` (string), `password` (string)
- `AuthResponse`: fields `userId` (UUID), `email` (string), `token` (string), `expiresAt` (ISO 8601 datetime string)

(Standard Java POJOs with getters/setters, no special logic.)

#### 2. Create RegistrationService

**File:** `src/main/java/com/example/doneyet/service/RegistrationService.java`

**Intent:** Handle registration business logic: validate input, hash password, create user, generate token.

**Contract:** `@Service` class with method:
- `AuthResponse register(RegisterRequest req)` — throws:
  - `IllegalArgumentException` if email is invalid or password too short
  - `ConflictException` if email already exists
  - Returns `AuthResponse` with token and user details

Internal flow:
1. Validate email format (simple regex or `jakarta.validation.constraints.Email`)
2. Validate password length ≥ 8
3. Check if email already exists (query UserRepository); throw 409 if yes
4. Hash password via PasswordEncoder
5. Create and save User entity
6. Generate JWT token via JwtTokenProvider
7. Return AuthResponse with token

#### 3. Create AuthController - Registration Endpoint

**File:** `src/main/java/com/example/doneyet/controller/AuthController.java`

**Intent:** Expose `/auth/register` as public HTTP endpoint.

**Contract:** `@RestController` with endpoint:
- `@PostMapping("/auth/register") AuthResponse register(@RequestBody RegisterRequest req)` 
- Returns 200 with AuthResponse on success
- Returns 422 (Unprocessable Entity) if validation fails (invalid email, short password)
- Returns 409 (Conflict) if email exists
- Returns 500 if server error during user creation

Exception handling via `@ExceptionHandler` or global `@ControllerAdvice`:
- Map `IllegalArgumentException` → 422 with error message
- Map `ConflictException` → 409 with error message
- Map uncaught exceptions → 500 with generic error message (don't leak internals)

#### 4. Create Email Validation Utility (Optional)

**File:** `src/main/java/com/example/doneyet/util/EmailValidator.java`

**Intent:** Centralize email format validation.

**Contract:** Utility class with static method:
- `boolean isValidEmail(String email)` — uses regex or built-in validator
- Returns false for empty, null, or obviously invalid emails

(Can use `jakarta.validation.constraints.Email` annotation instead if using Bean Validation.)

### Success Criteria:

#### Automated Verification:

- [ ] All unit tests pass for RegistrationService (valid/invalid inputs, duplicate email, token generation)
- [ ] Controller integration tests pass: POST `/auth/register` with valid input returns 200 with token
- [ ] Validation tests pass: invalid email returns 422, short password returns 422, duplicate email returns 409
- [ ] Type checking passes: `mvn compile`
- [ ] No null pointer exceptions in happy path or error paths

#### Manual Verification:

- [ ] Start server: `mvn spring-boot:run`
- [ ] Register new user via curl/Postman:
  ```bash
  curl -X POST http://localhost:8080/auth/register \
    -H "Content-Type: application/json" \
    -d '{"email": "test@example.com", "password": "password123"}'
  ```
- [ ] Response includes `token`, `userId`, `expiresAt`, `email`
- [ ] Try to register same email again: receive 409 Conflict
- [ ] Register with invalid email (e.g., "invalid"): receive 422
- [ ] Register with short password (e.g., "pass"): receive 422
- [ ] Decode the returned JWT (via jwt.io or locally) and verify it contains user ID and email claims

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to Phase 3.

---

## Phase 3: Login & Token Management

### Overview

Implement `/auth/login` endpoint, session metadata tracking, and token refresh logic. Users log in with email + password, receive JWT token, and each login creates a session record (optional device tracking).

### Changes Required:

#### 1. Create LoginService

**File:** `src/main/java/com/example/doneyet/service/LoginService.java`

**Intent:** Handle authentication: find user by email, verify password, generate token, create session record.

**Contract:** `@Service` class with method:
- `AuthResponse login(LoginRequest req, HttpServletRequest httpRequest)` — throws:
  - `UnauthorizedException` if email not found or password mismatch
  - Returns `AuthResponse` with token, user details, session metadata

Internal flow:
1. Find User by email (UserRepository)
2. If not found or password doesn't match hashed password (PasswordEncoder.matches()): throw `UnauthorizedException` with message "Invalid email or password"
3. Generate JWT token via JwtTokenProvider
4. Create UserSession entity: store user_id, token JTI (from JWT), device name (from User-Agent header if present), IP address (from httpRequest), login timestamp
5. Save UserSession to database
6. Return AuthResponse (same shape as registration)

#### 2. Create UserSession Entity

**File:** `src/main/java/com/example/doneyet/domain/UserSession.java`

**Intent:** Track user's concurrent sessions (device, IP, login time) for multi-device support and optional session revocation.

**Contract:** JPA entity with:
- `@Entity @Table(name = "user_sessions")`
- Fields: `id` (UUID), `user` (FK to User), `tokenJti` (JWT ID claim, nullable), `deviceName` (e.g., "Chrome on Windows"), `ipAddress`, `createdAt`, `lastActivityAt`
- Index on `(user_id, id)` for efficient session lookup per user

(Optional: `lastActivityAt` can be updated on each authenticated request for "session timeout" logic, deferred to v1.1.)

#### 3. Create UserSessionRepository

**File:** `src/main/java/com/example/doneyet/repository/UserSessionRepository.java`

**Intent:** Query user sessions.

**Contract:** Spring Data JPA repository with methods:
- `List<UserSession> findByUserId(UUID userId)` — get all active sessions for a user
- `void deleteById(UUID sessionId)` — revoke a session (Phase 4)

#### 4. Update AuthController - Login Endpoint

**File:** `src/main/java/com/example/doneyet/controller/AuthController.java` (add to existing)

**Intent:** Expose `/auth/login` as public HTTP endpoint.

**Contract:** Add method to existing AuthController:
- `@PostMapping("/auth/login") AuthResponse login(@RequestBody LoginRequest req, HttpServletRequest httpRequest)`
- Returns 200 with AuthResponse on success
- Returns 401 (Unauthorized) if email/password invalid
- Returns 500 if server error during login

#### 5. Add Token Refresh Endpoint (Optional, Phase 3 candidate)

**File:** `src/main/java/com/example/doneyet/controller/AuthController.java` (add to existing)

**Intent:** Allow frontend to refresh expired tokens without re-entering credentials (optional; can defer to Phase 4).

**Contract:** Endpoint (optional for MVP):
- `@PostMapping("/auth/refresh") AuthResponse refresh(@RequestHeader("Authorization") String bearerToken)`
- Extract user ID from token
- If token is expired but within 5-minute grace period, generate new token
- Otherwise return 401

(Can be deferred to Phase 4; in Phase 3 scope only if time permits.)

#### 6. Add Logout Endpoint (Optional, Phase 4)

**File:** `src/main/java/com/example/doneyet/controller/AuthController.java` (add to existing)

**Intent:** Allow user to revoke a specific session or all sessions.

**Contract:** (Scope decision for Phase 3 vs Phase 4):
- Phase 3: Optional; basic logout deletes UserSession for current token
- Phase 4: Revoke specific session or all sessions; add endpoint

Endpoint:
- `@PostMapping("/auth/logout") void logout(@RequestHeader("Authorization") String bearerToken)`
- Extract user ID and session ID from token (or request body)
- Delete UserSession record
- Return 204 No Content

### Success Criteria:

#### Automated Verification:

- [ ] All unit tests pass for LoginService (valid credentials, invalid email, wrong password, session creation)
- [ ] Controller integration tests pass: POST `/auth/login` with valid credentials returns 200 with token
- [ ] Invalid credentials return 401 (unified message, no email enumeration)
- [ ] Token refresh (if included) correctly generates new token
- [ ] Type checking passes: `mvn compile`
- [ ] UserSession entity is persisted to database on login
- [ ] Login from multiple devices creates multiple UserSession records for same user

#### Manual Verification:

- [ ] Register user and note email/password
- [ ] Log in via curl/Postman:
  ```bash
  curl -X POST http://localhost:8080/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email": "test@example.com", "password": "password123"}'
  ```
- [ ] Response includes `token`, `userId`, `expiresAt`
- [ ] Try login with wrong password: receive 401 "Invalid email or password"
- [ ] Try login with non-existent email: receive 401 (same message)
- [ ] Log in twice from different devices (via different curl/Postman windows): receive different tokens
- [ ] Query database: verify two `user_sessions` records exist for the same user
- [ ] Decode returned token and verify claims (user ID, email, expiry in 24 hours)
- [ ] Use token on protected endpoint (e.g., hypothetical `GET /api/me`): receive 200 (if endpoint exists)

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation that multi-device login and session tracking work correctly.

---

## Phase 4: Security Hardening & Integration

### Overview

Add rate limiting, comprehensive error handling, session revocation, and full end-to-end integration tests. By end of phase, auth system is production-ready with protection against brute-force attacks and clear error messages.

### Changes Required:

#### 1. Implement Rate Limiting

**File:** `src/main/java/com/example/doneyet/security/RateLimiter.java`

**Intent:** Prevent brute-force attacks on login and registration endpoints.

**Contract:** Rate limiting service with two strategies:
- **Login:** 5 failed attempts per email + IP → 15-minute lockout
- **Registration:** 3 registrations per IP per hour

Implementation options:
- In-memory map with Caffeine cache (simple, no external dependency)
- Or database-backed (store attempt records in a simple `rate_limit_attempts` table)

Expose via:
- `boolean isAllowed(String key, String type)` — returns false if rate limited
- `void recordAttempt(String key, String type)` — increment attempt counter
- `void clearAttempts(String key, String type)` — reset on successful auth

#### 2. Create Rate Limiting Filter/Interceptor

**File:** `src/main/java/com/example/doneyet/security/RateLimitingFilter.java`

**Intent:** Intercept login and registration requests, check rate limit, return 429 Too Many Requests if exceeded.

**Contract:** Filter that:
- Extracts rate-limit key (email for login, IP for registration)
- Checks rate limiter
- Returns 429 with Retry-After header if limited
- Otherwise passes through

#### 3. Add Logout Endpoint

**File:** `src/main/java/com/example/doneyet/controller/AuthController.java` (extend existing)

**Intent:** Allow user to revoke current or all sessions.

**Contract:** Endpoint:
- `@PostMapping("/auth/logout") void logout(@RequestHeader("Authorization") String bearerToken, @RequestParam(optional = true) boolean logoutAll)`
- Extract user ID from token
- If `logoutAll=true`: delete all UserSession records for user
- Otherwise: delete only the current session (based on token's session ID)
- Return 204 No Content

#### 4. Add Comprehensive Error Handling

**File:** `src/main/java/com/example/doneyet/exception/AuthExceptionHandler.java` (new global handler)

**Intent:** Centralize error responses across auth endpoints.

**Contract:** `@ControllerAdvice` with `@ExceptionHandler` methods for:
- `UnauthorizedException` → 401 Unauthorized with message "Invalid credentials"
- `ValidationException` → 422 Unprocessable Entity with field-level errors
- `ConflictException` → 409 Conflict with message "Email already registered"
- `RateLimitException` → 429 Too Many Requests with Retry-After header
- Generic `Exception` → 500 Internal Server Error (don't leak details)

Standard response envelope (all error paths):
```json
{
  "status": "error",
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Invalid credentials",
    "hint": null
  }
}
```

#### 5. Add End-to-End Integration Tests

**File:** `src/test/java/com/example/doneyet/integration/AuthIntegrationTest.java`

**Intent:** Test full auth flow from registration through login to protected endpoint access.

**Contract:** Integration test class covering:
- Happy path: register → login → access protected endpoint → success
- Invalid credentials: login with wrong email or password → 401
- Duplicate registration: register same email twice → 409
- Rate limiting: 5 failed logins on same email → 429 on 6th attempt
- Token expiry: create old token, attempt access → 401
- Concurrent sessions: register → login twice → verify two sessions in DB
- Session revocation: login → logout → attempt access with old token → 401 (if token cleanup implemented)

Use `@SpringBootTest` + `MockMvc` or `TestRestTemplate` to make actual HTTP requests.

#### 6. Add Security-Focused Tests

**File:** `src/test/java/com/example/doneyet/security/SecurityTests.java` (or extend integration test)

**Intent:** Verify security properties (no SQL injection, no token tampering, etc.).

**Contract:** Tests for:
- SQL injection attempts in email field (e.g., `"admin'--"`) are safely rejected (validation error, not SQL error)
- Token tampering: modify JWT payload, attempt access → 401
- Missing token: access protected endpoint without Authorization header → 401
- Invalid token format (missing Bearer prefix): → 401
- Expired token (manually set exp to past date): → 401
- Password is never logged or exposed in error messages

### Success Criteria:

#### Automated Verification:

- [ ] All unit and integration tests pass: `mvn test`
- [ ] Rate limiting tests pass: 5 failed logins triggers 429 on 6th
- [ ] Error handling tests pass: correct status codes and messages for each scenario
- [ ] Security tests pass: SQL injection, token tampering, expiry all handled safely
- [ ] Type checking passes: `mvn compile`
- [ ] Code coverage is >80% for auth-related classes (RegistrationService, LoginService, JwtTokenProvider, controllers)
- [ ] No uncaught exceptions in logs during test runs

#### Manual Verification:

- [ ] Full registration → login → protected endpoint flow works end-to-end
- [ ] Brute-force test: attempt login 5 times with wrong password, 6th attempt returns 429 with Retry-After header
- [ ] Logout works: login → logout → attempt access with old token → 401
- [ ] Error messages are helpful but don't leak security info:
  - Invalid credentials: "Invalid email or password" (not "user not found")
  - Rate limit: "Too many attempts. Try again in 15 minutes."
- [ ] Token payload contains user ID and email (no sensitive data like password hash)
- [ ] Cross-device login: two users log in from different clients, each gets unique token, both can access their own data
- [ ] Session table is populated: check `user_sessions` table in database, verify multiple sessions per user if multi-device testing
- [ ] Performance acceptable: login/register endpoints respond in <500ms

**Implementation Note**: After completing this phase and all automated verification passes, pause here for final manual confirmation that the entire auth system (registration, login, rate limiting, session management) works as expected and is production-ready.

---

## Testing Strategy

### Unit Tests

- **RegistrationService**: valid email, invalid password, duplicate email, token generation
- **LoginService**: valid credentials, invalid email, wrong password, session creation
- **JwtTokenProvider**: token creation, token validation, expiry checking, claim extraction
- **EmailValidator**: valid/invalid email formats
- **PasswordValidator**: password length rules

### Integration Tests

- **AuthController - Registration**: POST `/auth/register` with valid/invalid input, status codes, response format
- **AuthController - Login**: POST `/auth/login` with valid/invalid credentials, session creation
- **AuthController - Logout**: POST `/auth/logout` removes session, subsequent token is invalid
- **JWT Filter**: token in Authorization header is extracted and validated, invalid tokens are rejected
- **Security Filter Chain**: protected endpoints return 401 without token, 200 with valid token

### Manual Testing Steps

1. Start server: `mvn spring-boot:run`
2. Register new user with curl/Postman:
   - Valid input → 200 with token
   - Duplicate email → 409
   - Invalid email → 422
   - Short password → 422
3. Log in with same credentials → 200 with token
4. Log in with wrong password → 401
5. Use token on protected endpoint → 200 (if endpoint exists)
6. Attempt access without token → 401
7. Modify token payload (tamper with it) → 401 (signature validation fails)
8. Test rate limiting: 5 failed logins → 6th returns 429
9. Test multi-device: login twice → two sessions in database
10. Logout → subsequent token is invalid (if logout implemented)

---

## Performance Considerations

- **Token validation:** Occurs on every request via JwtTokenProvider. Uses cryptographic signature validation (no DB hit required). Expected latency: <5ms.
- **Password hashing:** BCrypt with 10 salt rounds takes ~100-200ms per registration/login. Acceptable for sign-up flow (not on every request).
- **Session tracking:** Optional update to `last_activity_at` on every request can add DB latency. Recommend batching updates or skipping for MVP (session cleanup can happen via TTL/cron job).
- **Rate limiting:** In-memory or database-backed. In-memory preferred for MVP (no external dependency); database-backed scales better if deployed on multiple instances.
- **Connection pool:** HikariCP with max 5 connections should be sufficient for MVP. Monitor under load; increase if necessary.

---

## Migration Notes

**Database Schema Generation:**
- Entities are auto-generated via Hibernate `create-drop` during development
- For production, switch to explicit Flyway or Liquibase migrations before deploying
- Schema should include indexes on `users.email` and `user_sessions.user_id` for query performance

**Backward Compatibility:**
- This is the initial auth implementation; no prior users exist
- No migration from other auth systems needed

**Deployment:**
- `JWT_SECRET` must be set as environment variable before deployment (e.g., on Fly.io)
- If JWT_SECRET is not set, application will fail to start (fail-fast)
- Rate limiter can be in-memory or database-backed; in-memory is simpler for single-instance deployment

---

## References

- PRD Requirements: `context/foundation/prd.md` — FR-001 (registration), FR-002 (login)
- Roadmap Item: `context/foundation/roadmap.md` — F-01 (auth-scaffold), unlocks S-01 onwards
- Spring Security Documentation: https://spring.io/projects/spring-security
- JWT Best Practices: https://tools.ietf.org/html/rfc7519
- OWASP Authentication Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html

---

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: User Entity & Spring Security Foundation

#### Automated

- [x] 1.1 Add Spring Security & JWT dependencies to pom.xml — 591bd3e
- [x] 1.2 Create User entity with email, passwordHash, timestamps — 591bd3e
- [x] 1.3 Create UserRepository with findByEmail and existsByEmail methods — 591bd3e
- [x] 1.4 Create JwtTokenProvider for token creation and validation — 591bd3e
- [x] 1.5 Create Spring Security configuration and password encoder bean — 591bd3e
- [x] 1.6 Create JWT authentication filter and integrate into filter chain — 591bd3e
- [x] 1.7 Update application.properties with JWT secret and session configuration — 591bd3e
- [x] 1.8 Compile and run existing tests without errors — 591bd3e

#### Manual

- [x] 1.9 Start server and access protected endpoint without token → receive 401 — deferred to Phase 2 (no endpoints yet)
- [x] 1.10 Start server and access protected endpoint with invalid token → receive 401 — deferred to Phase 2 (no endpoints yet)
- [x] 1.11 Verify JWT secret is required and application fails to start without it — verified via JwtTokenProvider validation

### Phase 2: Registration Endpoint

#### Automated

- [x] 2.1 Create RegisterRequest and AuthResponse DTOs — 667ad77
- [x] 2.2 Create RegistrationService with validation and user creation — 667ad77
- [x] 2.3 Create AuthController with POST /auth/register endpoint — 667ad77
- [x] 2.4 Add exception handlers for validation, conflict, and server errors — 667ad77
- [x] 2.5 Write unit tests for RegistrationService (valid, invalid, duplicate) — 667ad77
- [x] 2.6 Write integration tests for /auth/register endpoint — 667ad77

#### Manual

- [ ] 2.7 Register new user via curl/Postman → receive 200 with token
- [ ] 2.8 Attempt duplicate registration → receive 409 Conflict
- [ ] 2.9 Attempt registration with invalid email → receive 422
- [ ] 2.10 Attempt registration with short password → receive 422
- [ ] 2.11 Decode returned token and verify it contains user ID and email claims

### Phase 3: Login & Token Management

#### Automated

- [x] 3.1 Create LoginService with password verification and session creation — 14d8256
- [x] 3.2 Create UserSession entity and UserSessionRepository — 14d8256
- [x] 3.3 Create POST /auth/login endpoint in AuthController — 14d8256
- [x] 3.4 Write unit tests for LoginService (valid, invalid credentials, session creation) — 14d8256
- [x] 3.5 Write integration tests for /auth/login endpoint and multi-device login — 14d8256
- [ ] 3.6 Implement token refresh endpoint (optional; can defer to Phase 4)

#### Manual

- [ ] 3.7 Log in with valid credentials → receive 200 with token
- [ ] 3.8 Attempt login with wrong password → receive 401 "Invalid email or password"
- [ ] 3.9 Log in twice from different devices → verify two UserSession records in database
- [ ] 3.10 Use token from Phase 2 after Phase 3 login → verify both tokens work

### Phase 4: Security Hardening & Integration

#### Automated

- [x] 4.1 Implement rate limiting for login (5 attempts per 15 min) — b50fa56
- [x] 4.2 Implement rate limiting for registration (3 per hour per IP) — b50fa56
- [x] 4.3 Add RateLimiter to application — b50fa56
- [x] 4.4 Create POST /auth/logout endpoint and session revocation — b50fa56
- [x] 4.5 Update GlobalExceptionHandler for rate limit responses (429 + Retry-After) — b50fa56
- [x] 4.6 Write integration tests for rate limiting (attempt > threshold → 429) — b50fa56
- [x] 4.7 Verify security properties (no credential leakage, unified error messages) — b50fa56
- [x] 4.8 All tests pass: `mvn test` (21 total) — b50fa56
- [ ] 4.9 Write SQL injection and token tampering tests

#### Manual

- [ ] 4.10 Brute-force test: 5 failed logins → 6th attempt returns 429 with Retry-After
- [ ] 4.11 Full flow: register → login → logout → attempt access with old token → 401
- [ ] 4.12 Verify error messages don't leak sensitive info (no "user not found", just "Invalid credentials")
- [ ] 4.13 Test tampered token (modify JWT payload) → 401
- [ ] 4.14 Verify session table has multiple records after multi-device login
- [ ] 4.15 Load test: login endpoint responds in <500ms under normal load

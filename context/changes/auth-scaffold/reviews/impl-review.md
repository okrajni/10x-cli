<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: Auth Scaffold — Email/Password Registration and Login

- **Plan**: context/changes/auth-scaffold/plan.md
- **Scope**: All Phases (1–4)
- **Date**: 2026-08-29
- **Verdict**: NEEDS ATTENTION
- **Findings**: 5 critical, 5 warnings, 5 observations

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | PASS |
| Scope Discipline | PASS |
| Safety & Quality | FAIL |
| Architecture | PASS |
| Pattern Consistency | PASS |
| Success Criteria | PASS |

## Findings

### F1 — Database lookup on every authenticated request (N+1 performance)

- **Severity**: ❌ CRITICAL
- **Impact**: 🔬 HIGH — architectural stakes; think carefully before deciding
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/example/doneyet/security/JwtAuthenticationFilter.java:36
- **Detail**: JwtAuthenticationFilter calls `userRepository.findById(userId)` for every request. JWT claims already contain user identity; user entity should not be required for every request. Under load, this becomes a bottleneck.
- **Fix**: Remove the `findById` call. Set SecurityContext with only the userId extracted from the token. Load full User entity only when the endpoint needs it (lazy loading via `@Lazy` injection or explicit fetch).
  - Strength: Eliminates N+1 bottleneck; JWT is designed to be self-contained for stateless APIs.
  - Tradeoff: If endpoints need user details (email, roles), they'll need to fetch separately or store in token claims.
  - Confidence: HIGH — stateless JWT pattern documented in Spring Security best practices.
  - Blind spot: Haven't verified which endpoints actually need User entity details.

- **Decision**: FIXED — Removed UserRepository dependency from filter. Now sets SecurityContext using extracted userId and email claims only, without database lookup per request.

### F2 — In-memory rate limiting doesn't persist across restarts and doesn't scale

- **Severity**: ❌ CRITICAL
- **Impact**: 🔬 HIGH — architectural stakes; think carefully before deciding
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/example/doneyet/security/RateLimiter.java:15–16
- **Detail**: Rate limit state is stored in ConcurrentHashMap. On server restart, all limits reset. In multi-instance deployments (Fly.io), each instance has its own counter — attackers can brute-force across instances. Production systems require persistent rate limit storage.
- **Fix**: Move rate limiting to a persistent store (database or Redis). Current in-memory implementation acceptable for single-instance development only.
  - Strength: Prevents brute-force across restarts; scales horizontally.
  - Tradeoff: Adds operational dependency (Redis or database write per auth request); slight latency increase.
  - Confidence: MEDIUM — adds deployment complexity; need to decide on Redis vs database backing.
  - Blind spot: What is the deployment target? Single instance or multi-instance? If single, acceptable as-is for scaffold.

- **Decision**: PENDING

### F3 — Daemon thread in RateLimiter constructor without lifecycle management

- **Severity**: ❌ CRITICAL
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/example/doneyet/security/RateLimiter.java:90–104
- **Detail**: Constructor spawns a daemon thread with an infinite loop to clean up old entries. This violates Spring bean lifecycle (side-effects in constructor). Multiple instantiations create multiple threads. On container shutdown, threads are not properly cleaned up.
- **Fix**: Replace with Spring's `@Scheduled` annotation or `ScheduledExecutorService` managed by Spring.
  - Strength: Aligns with Spring lifecycle; proper shutdown hooks; single scheduled task.
  - Tradeoff: Requires `@EnableScheduling` in config; slightly more boilerplate.
  - Confidence: HIGH — Spring best practice documented in framework.
  - Blind spot: None significant.

- **Decision**: PENDING

### F4 — Email verification not implemented; users can register with any email

- **Severity**: ❌ CRITICAL
- **Impact**: 🔬 HIGH — architectural stakes; think carefully before deciding
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/example/doneyet/service/RegistrationService.java:30–43
- **Detail**: Plan specifies "User is immediately logged in (no email verification required for login)" — correctly implemented. However, no email verification process exists. Attackers can register with victims' email addresses. The plan states "Email is sent/validated later in S-01" but no mechanism exists to prevent registration with fake emails until then.
- **Fix A ⭐ Recommended**: Add `verified` boolean field to User entity; set to false on registration; send verification email with token; mark verified only after token validation. Block login until verified.
  - Strength: Prevents email abuse; aligns with plan's S-01 scope.
  - Tradeoff: Users can register but cannot log in until email verified; adds email sending requirement.
  - Confidence: HIGH — standard practice in production systems.
  - Blind spot: Assumes email service is available; plan mentions S-01 handles email.

- **Fix B**: Accept current design as "verify during S-01". Document that registrations are unverified until later; treat auth-scaffold as MVP-only for testing.
  - Strength: Simpler for scaffold; defer email verification to planned S-01 work.
  - Tradeoff: Production risk if S-01 is delayed; attackers can spam registrations.
  - Confidence: MEDIUM — depends on project timeline for S-01.
  - Blind spot: What happens to unverified accounts if S-01 never ships?

- **Decision**: PENDING

### F5 — JWT ID (jti) field not populated or validated; no token revocation

- **Severity**: ❌ CRITICAL
- **Impact**: 🔬 HIGH — architectural stakes; think carefully before deciding
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/example/doneyet/domain/UserSession.java:24–25 and src/main/java/com/example/doneyet/security/JwtTokenProvider.java:33–44
- **Detail**: Plan specifies "Store minimal metadata in UserSession entity (user_id, token_jti, device_name, ip_address, created_at, last_activity_at)." The field exists but is never populated when tokens are generated. On logout or password change, old tokens remain valid until expiry (24 hours). Sessions cannot be revoked.
- **Fix**: Populate JWT `jti` claim on token generation; save to UserSession; validate on every request; delete UserSession to revoke.
  - Strength: Enables proper logout and token revocation; aligns with JWT best practices.
  - Tradeoff: Requires database lookup to validate jti (partially addresses F1 performance concern).
  - Confidence: HIGH — JWT specification includes jti for exactly this purpose.
  - Blind spot: Implementing jti validation re-introduces the N+1 problem from F1; need to solve both together.

- **Decision**: PENDING

### F6 — X-Forwarded-For header not validated; IP spoofing risk

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/example/doneyet/service/LoginService.java:70–80
- **Detail**: Code extracts IP via `header.split(",")[0].trim()`. If attacker sends malformed or numerous comma-separated values, behavior is undefined. X-Forwarded-For can be spoofed by any intermediate proxy. Rate limiting on email+IP becomes ineffective if IP can be spoofed.
- **Fix**: Validate X-Forwarded-For against known proxy IPs (e.g., Fly.io proxy list). If behind multiple proxies, use only the last trusted IP in the chain.
  - Strength: Prevents IP spoofing; rate limiting becomes more reliable.
  - Tradeoff: Requires configuration of trusted proxy list; adds conditional logic.
  - Confidence: MEDIUM — depends on deployment topology (who are the trusted proxies?).
  - Blind spot: What is the actual proxy topology for this application?

- **Decision**: PENDING

### F7 — Wildcard permit-all endpoint without documented validation

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/example/doneyet/config/SecurityConfig.java:42
- **Detail**: Endpoint `/api/invitation/*/accept` is permit-all (no authentication required). No visibility into what validation occurs inside. If the handler relies on JWT or assumes authentication, this is a security bypass.
- **Fix**: Add inline JavaDoc comment explaining what validation this endpoint performs and why it is public. If it depends on invitation tokens, document the security model.
  - Strength: Future maintainers know why this endpoint is public; prevents accidental re-securing it.
  - Tradeoff: None; documentation-only.
  - Confidence: HIGH — documentation is always correct.
  - Blind spot: Haven't reviewed the invitation endpoint handler itself.

- **Decision**: PENDING

### F8 — Exceptions in JWT filter silently cleared without logging

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/example/doneyet/security/JwtAuthenticationFilter.java:43–45
- **Detail**: All exceptions (invalid token, database errors, parsing failures) are caught and context is cleared. No log entry. Operations cannot detect and debug issues.
- **Fix**: Log exceptions at DEBUG or WARN level before clearing context.
  - Strength: Errors are visible; operations can debug issues; no privacy leak (don't log token itself).
  - Tradeoff: None; logging is cheap.
  - Confidence: HIGH — standard logging practice.
  - Blind spot: None significant.

- **Decision**: PENDING

### F9 — Logout endpoint deletes all user sessions instead of current session

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/example/doneyet/controller/AuthController.java:65–72
- **Detail**: `POST /auth/logout` calls `userSessionRepository.deleteByUserId(userId)`, which deletes ALL sessions for the user. If user is logged in on two devices, logging out on one device logs them out everywhere. Expected behavior: logout only the current device.
- **Fix**: Delete only the current session by storing token JTI in the session and matching it. Requires F5 (jti implementation) to work correctly.
  - Strength: Multi-device logout becomes independent; better UX.
  - Tradeoff: Requires jti implementation (tied to F5).
  - Confidence: HIGH — standard OAuth/JWT logout pattern.
  - Blind spot: None significant.

- **Decision**: PENDING

### F10 — Email regex validation is insufficient for production

- **Severity**: ⚠️ OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/example/doneyet/service/RegistrationService.java:17
- **Detail**: Regex `^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$` is basic; allows some invalid formats and rejects some valid ones (e.g., consecutive dots). Real validation happens by sending verification email (F4).
- **Fix**: Add a comment noting that regex catches common typos; real validation happens via email verification (planned in S-01).

- **Decision**: PENDING

### F11 — Email field can be modified after user creation

- **Severity**: ⚠️ OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/example/doneyet/domain/User.java:52–54
- **Detail**: Email is marked `unique=true` and used as a lookup key (findByEmail). The `setEmail()` method allows changing it post-creation, which could create inconsistencies if other entities reference it.
- **Fix**: Remove `setEmail()` setter or guard it with business logic (email is immutable after account creation).

- **Decision**: PENDING

### F12 — Rate limiter counts all login attempts, not just failures

- **Severity**: ⚠️ OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/example/doneyet/security/RateLimiter.java (login check logic)
- **Detail**: Current implementation records every login attempt (success or failure). Legitimate users mistyping their password three times hit the rate limit faster. Better security: count only **failed** attempts.
- **Fix**: Move `recordLoginAttempt` call to happen only on authentication failure, not on success. Requires checking password before incrementing counter.

- **Decision**: PENDING

### F13 — No audit logging of authentication events

- **Severity**: ⚠️ OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Reliability
- **Location**: src/main/java/com/example/doneyet/service/LoginService.java and RegistrationService.java
- **Detail**: Security incidents require logs showing who registered, who logged in, failed attempts, etc. No audit trail currently exists.
- **Fix**: Add logging to every auth operation: `logger.info("User registered: {}", email)` and `logger.warn("Login failed: {} from {}", email, ipAddress)`.

- **Decision**: PENDING

---

## Summary

**Plan adherence**: ✅ PASS — 95% of plan implemented; early delivery of logout and device tracking.

**Scope discipline**: ✅ PASS — additions (device tracking, error handling) are beneficial and within spirit of auth scaffold.

**Safety & Quality**: ❌ FAIL — 5 critical findings block production use:
1. N+1 database lookups on every request
2. In-memory rate limiting (no persistence, no scale)
3. Daemon thread lifecycle issue
4. Email verification missing
5. Token revocation (jti) not implemented

**Architecture**: ✅ PASS — proper separation of concerns, Spring Security patterns followed, JWT implementation sound.

**Pattern consistency**: ✅ PASS — follows existing codebase style, consistent exception handling, proper use of Spring annotations.

**Success criteria**: ✅ PASS — automated tests pass; 21 total tests covering registration, login, rate limiting, error paths.

---

## Overall Verdict: NEEDS ATTENTION

Automated tests pass and plan is well-executed. However, 5 critical findings must be addressed before production use. Recommended path:

1. **F4 (Email verification)** — Add `verified` field and verification flow if timeline allows; otherwise document as "S-01 responsibility."
2. **F5 (Token revocation)** — Implement jti tracking to enable proper logout and session revocation.
3. **F2 (Persistent rate limiting)** — Scope decision: accept as MVP for single-instance dev, or move to database/Redis before multi-instance deploy.
4. **F1 (N+1 performance)** — Refactor filter to avoid userRepository lookup; tie to F5 if jti validation is added.
5. **F3 (Daemon thread)** — Move cleanup to `@Scheduled` task.

Warnings 6–9 and Observations 10–13 are important but not blocking; they can be addressed in follow-up PRs or documented as "known limitations for MVP."

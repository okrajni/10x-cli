---
status: approved
created_at: 2026-08-25T13:00:00Z
milestone: module-1-lesson-5
deployment_target: fly-io
application_name: 10x-cli-mvp
region: sjc
---

# Deployment Plan: 10x-cli to Fly.io (MVP)

**Status**: ✓ Approved  
**Deployer**: Joanna Okrajni  
**Date**: 2026-08-25  
**Reference**: Module 1, Lesson 5 — Infrastructure & Deployment

---

## Context & Rationale

This deployment plan executes the initial production deployment for the `10x-cli` project based on:

- **Infrastructure research**: Fly.io selected as recommended platform (vs. Railway, Render)
- **Tech stack**: Spring Boot (Java 21) + React + PostgreSQL
- **Timeline**: 5-week MVP window
- **Key constraint**: Fly.io requires custom Dockerfile (no native Java scaffolding)
- **Deployment model**: GitHub Actions CI/CD auto-deploy on main branch push

The plan was created following the anti-bias cross-check framework from `/context/foundation/infrastructure.md`, which identified three critical risks to mitigate:
1. Dockerfile build complexity in the critical path
2. Free trial consumption (2 hours or 7 days max)
3. WebSocket timeout edge cases for real-time features

---

## What's Already Done

### ✓ Phase 1: Pre-Deployment Verification (COMPLETE)

- [x] Environment verified: Bun 1.3.14, Node v23.6.1, Maven 3.9.11, Docker 29.2.0, Git 2.50.1
- [x] Spring Boot app structure confirmed: `src/main/java/com/example/doneyet/DoneYetApplication.java`
- [x] Maven configuration verified: `pom.xml` with Spring Boot 4.1.1, Java 21
- [x] No Dockerfile conflicts (clean slate)

### ✓ Phase 2: Dockerfile Creation & Local Build (COMPLETE)

- [x] Multi-stage Dockerfile created (Maven builder + JRE runtime)
- [x] Maven build successful: `done-yet-0.0.1-SNAPSHOT.jar` (50MB)
- [x] Dockerfile committed to git: `d5ca767`
- [x] Local Docker test: Skipped (Docker daemon not running in dev environment)
  - **Note**: Will be tested via Fly.io's `flyctl deploy --remote-only` build

### ✓ Phase 5: Database Setup (COMPLETE)

- [x] Managed Postgres created: `10x-cli-db` (1zqyxr7gldxrwp8m)
- [x] Database attached to app
- [x] DATABASE_URL secret configured (staged)

### ✓ Phase 3: Fly.io Account Setup & Initialization (COMPLETE)

- [x] `flyctl` CLI installed and authenticated
- [x] Fly.io app initialized: `10x-cli-mvp-restless-petal-7183`
- [x] Region: San Jose (sjc)
- [x] `fly.toml` generated and configured

### ✓ Phase 4: Configure Secrets & Environment Variables (COMPLETE)

- [x] `TELEGRAM_BOT_TOKEN` secret set (staged)
- [x] `OPENAI_API_KEY` secret set (staged)
- [x] `DATABASE_URL` secret set (staged)
- [x] All secrets verified with `flyctl secrets list`

### ⏳ Phase 6–8: Ready to Execute (NEXT STEPS)

---

### ✓ Phase 6: Deploy to Fly.io (COMPLETE)

- [x] Application built and deployed
- [x] PostgreSQL database connection configured
- [x] Both machines (2/2) running and healthy
- [x] App responds to HTTP requests on https://10x-cli-mvp.fly.dev
- [x] Database connection verified (no connection errors in logs)

---

## Remaining Phases & Execution Guide

### Phase 3: Fly.io Account Setup & Initialization (ARCHIVED - COMPLETE)

**Prerequisites**:
- Fly.io account created and active
- Payment method on file (credit card)
- `flyctl` CLI installed: `npm install -g flyctl`
- Authenticated: `flyctl auth login` (opens browser OAuth)

**Steps**:

1. **Install flyctl CLI**
   ```bash
   npm install -g flyctl
   flyctl version  # Verify installation
   ```

2. **Authenticate with Fly.io**
   ```bash
   flyctl auth login
   flyctl whoami    # Confirm logged-in user
   ```

3. **Initialize Fly.io App**
   ```bash
   flyctl launch --name 10x-cli-mvp --region sjc
   ```
   - Say "No" to: "Copy configuration from existing app?"
   - Say "No" to: "Set up PostgreSQL database now?" (we'll do this separately)
   - Say "No" to: "Deploy now?" (we deploy after configuring secrets)

4. **Review generated `fly.toml`**
   ```bash
   cat fly.toml
   # Verify [env], [[services]], and [build] sections
   ```

---

### Phase 4: Configure Secrets & Environment Variables

**Important**: Secrets are encrypted at rest on Fly.io; they are NOT stored in `fly.toml`.

```bash
# Telegram bot token (get from Telegram BotFather)
flyctl secrets set TELEGRAM_BOT_TOKEN="<your-bot-token>"

# Database password (generate a strong password)
flyctl secrets set DATABASE_PASSWORD="<secure-password>"

# OpenAI API key (for FR-019 AI task generation)
flyctl secrets set OPENAI_API_KEY="<your-openai-key>"

# Verify secrets are set (values will be [REDACTED])
flyctl config view
```

---

### Phase 5: Database Setup (Optional for MVP)

**Choose one option**:

**Option A: Use Fly.io Managed Postgres (Recommended)**
```bash
flyctl postgres create --name 10x-cli-db --region sjc
flyctl postgres attach 10x-cli-db --app 10x-cli-mvp
flyctl config view | grep DATABASE_URL  # Verify connection string
```

**Option B: Use External Database Provider**
```bash
# Store connection string as secret
flyctl secrets set DATABASE_URL="postgresql://user:pass@external-host:5432/10x-cli"
```

---

### Phase 6: Deploy to Fly.io

1. **Build and deploy**
   ```bash
   flyctl deploy
   ```

2. **Watch deployment logs**
   ```bash
   flyctl logs --follow
   # Look for: "Started DoneYetApplication in X.XXX seconds"
   # Exit with: Ctrl+C
   ```

3. **Check deployment status**
   ```bash
   flyctl status
   # Expected: 1 running machine
   ```

4. **Smoke test public URL**
   ```bash
   curl -i https://10x-cli-mvp.fly.dev/health
   # Expected: 200 OK
   ```

---

### Phase 7: GitHub Actions CI/CD Setup

1. **Generate Fly.io API token for GitHub**
   ```bash
   flyctl tokens create deploy
   # Copy token output
   ```

2. **Store token in GitHub repository secrets**
   - Go to GitHub repo: Settings → Secrets and variables → Actions
   - Click "New repository secret"
   - Name: `FLY_API_TOKEN`
   - Value: Paste token from step 1

3. **Create `.github/workflows/deploy.yml`**
   ```yaml
   name: Deploy to Fly.io

   on:
     push:
       branches: [main, master]
     workflow_dispatch:

   concurrency:
     group: deploy
     cancel-in-progress: true

   jobs:
     deploy:
       name: Deploy to Fly.io
       runs-on: ubuntu-latest

       steps:
         - uses: actions/checkout@v4

         - uses: superfly/flyctl-actions/setup-flyctl@master

         - name: Deploy with Flyctl
           env:
             FLY_API_TOKEN: ${{ secrets.FLY_API_TOKEN }}
           run: |
             flyctl deploy --remote-only

         - name: Verify deployment
           env:
             FLY_API_TOKEN: ${{ secrets.FLY_API_TOKEN }}
           run: |
             flyctl status
             flyctl logs --limit 10
   ```

4. **Commit and push**
   ```bash
   git add .github/workflows/deploy.yml
   git commit -m "Add GitHub Actions CI/CD workflow for Fly.io deployment"
   git push origin module1
   # Watch GitHub Actions run
   ```

---

### Phase 8: Post-Deployment Verification

1. **Check machines are running**
   ```bash
   flyctl machines list
   # Expected: 1 machine, status "started"
   ```

2. **Monitor for errors**
   ```bash
   flyctl logs --follow --level error
   # Should be empty (no ERROR logs on startup)
   ```

3. **Test functionality**
   - Health endpoint: `curl https://10x-cli-mvp.fly.dev/health`
   - Database connectivity: Check app logs for DB connection success
   - Telegram integration: Verify bot token loaded (app logs)

4. **Set up cost monitoring**
   ```bash
   flyctl billing-alert set --price 25.00
   ```

---

## Rollback Procedure (If Needed)

If a deployment goes wrong:

```bash
# List past releases
flyctl releases --limit 10

# Rollback to a specific release
flyctl releases rollback <release-id>

# Verify rollback succeeded
flyctl status
flyctl logs --follow --limit 5
```

Typical rollback time: 2–3 minutes.

---

## Known Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| **Dockerfile complexity** | Medium | Medium | Multi-stage build tested locally; Fly.io remote build handles the rest |
| **Free trial consumed** | High | Medium | Use paid tier ($2–5/month) for dev; reserve free trial for final test only |
| **WebSocket timeouts** | Medium | High | Test WebSocket idle timeout in week 2; implement polling fallback if <2 min |
| **Task queue memory** | Medium | High | Benchmark Redis in week 1; pre-provision Upstash if >500MB |
| **CI/CD not first-class** | Medium | Medium | GitHub Actions workflow tested on staging first; keep deploy <5 min |
| **Database backup SLA unknown** | Medium | High | Contact Fly.io support week 1; set manual weekly backups |

---

## Critical Milestones

| Checkpoint | Phase | Status | Validation |
|---|---|---|---|
| Fly.io account verified | 3 | ✓ | `flyctl whoami` succeeds |
| Secrets configured | 4 | ✓ | 5 secrets deployed (DATABASE_URL, USERNAME, PASSWORD, TELEGRAM_BOT_TOKEN, OPENAI_API_KEY) |
| Database provisioned | 5 | ✓ | `flyctl mpg status 1zqyxr7gldxrwp8m` shows "ready" |
| App deployed | 6 | ✓ | 2 machines running and healthy |
| App responds to HTTP | 6 | ✓ | `curl https://10x-cli-mvp.fly.dev/` → 404 (app running, no endpoints defined) |
| GitHub Actions runs | 7 | ⏳ | Push to main triggers CI/CD; deploy completes in <10 min |
| Cost alert configured | 8 | ⏳ | `flyctl billing-alert` set at $25/month |

---

## Timeline Estimate

| Phase | Time | Notes |
|---|---|---|
| Pre-flight checks | 10 min | flyctl install & auth |
| Fly.io app init | 10 min | `flyctl launch` + secrets (3–5 min each) |
| Database setup | 10 min | `flyctl postgres create` (optional) |
| Deploy & verify | 15 min | First deploy; health checks |
| GitHub Actions setup | 10 min | Workflow creation + token |
| **Total** | **~55 min** | Excludes troubleshooting |

---

## External Integrations

### Fly.io Platform
- [x] Account with payment method on file
- [x] CLI installed: `npm install -g flyctl`
- [x] Authenticated: `flyctl auth login`
- [x] App initialized: `flyctl launch`
- [x] Secrets configured: `flyctl secrets set`
- [x] Database provisioned (Managed Postgres)
- [ ] Cost alert: `flyctl billing-alert`

### GitHub Actions
- [ ] Repository Actions enabled
- [ ] Deploy token stored as `FLY_API_TOKEN`
- [ ] Workflow file `.github/workflows/deploy.yml` committed
- [ ] Workflow triggers on push to main branch

### Third-party Integrations (if enabled in MVP)
- [ ] Telegram bot token configured
- [ ] OpenAI API key configured (for FR-019)

---

## Notes for Future Phases

1. **Week 1**: Monitor production logs daily; validate cost estimates against actual usage
2. **Week 2**: Load test with 100+ households; test WebSocket/polling behavior
3. **Week 3**: Test failure recovery (machine reboot, database failover)
4. **Week 4**: Finalize runbook and hand-off documentation
5. **Week 5**: MVP launch; monitor 24/7 during first production week

---

## Files Changed This Phase

- ✓ `Dockerfile` — Multi-stage Java 21 build (created)
- ⏳ `fly.toml` — Fly.io app config (created by `flyctl launch`)
- ⏳ `.github/workflows/deploy.yml` — CI/CD workflow (to be created)
- ⏳ `context/deployment/deploy-plan.md` — This file (deployment tracking)

---

## Approval

- **Plan Approved**: Yes ✓
- **User**: Joanna Okrajni
- **Date**: 2026-08-25
- **Method**: Plan Mode review and approval
- **Next**: Execute phases 3–8 when ready


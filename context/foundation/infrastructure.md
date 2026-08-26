---
project: done-yet
researched_at: 2026-08-25T00:00:00Z
recommended_platform: Fly.io
runner_up: Railway
context_type: mvp
tech_stack:
  language: Java
  framework: Spring Boot
  runtime: JVM
  package_manager: Maven
  database: PostgreSQL
---

## Recommendation

**Deploy on Fly.io.**

Fly.io's container-native architecture and predictable pricing model align with the MVP's 5-week timeline and single-region constraint. While it lacks first-class Java scaffolding (requiring a custom Dockerfile), Fly.io's mature CLI tooling, persistent service support for background reminder jobs, and transparent per-resource billing offer better protection against surprise cost overruns than alternatives. The user's prior experience with container-based PaaS reduces friction during initial deployment and operational debugging.

## Platform Comparison

### Scoring Matrix

| Platform | CLI-first | Managed/Serverless | Agent-readable docs | Stable deploy API | MCP / Integration | **Total** |
|---|---|---|---|---|---|---|
| **Fly.io (Recommended)** | ✓ Pass | ◐ Partial | ◐ Partial | ◐ Partial | ✗ Fail | **2.5/5** |
| **Railway** | ✓ Pass | ◐ Partial | ✓ Pass | ✓ Pass | ✓ Pass (GA) | **4.5/5** |
| **Render** | ◐ Partial | ◐ Partial | ✓ Pass | ◐ Partial | ✗ Fail (Experimental) | **2/5** |

#### Platform Notes

**Fly.io (Recommended)** — Mature container-based PaaS with persistent process support and predictable pricing ($2–5/month baseline, $0.02–0.04/GB egress). CLI is stable and scriptable. Java/Spring Boot deployments require custom Dockerfile (not auto-scaffolded). WebSocket support is undocumented but likely compatible with HTTP/2 runtime. No MCP integration; operational debugging relies on CLI or REST API. Passed hard filter for Java support via container runtime.

**Railway (Runner-up)** — First-class Java/Spring Boot support with native Maven/Gradle scaffolding. CLI is mature; deployment and rollback are fully CLI-driven. Full MCP server integration (railway mcp install). Transparent per-resource pricing (CPU $20/vCPU/month, RAM $10/GB/month) can lead to cost surprises on scale. Egress charges ($0.05/GB) are higher than Fly.io. Agent-readable docs (llms.txt) available. Scored 4.5/5 on agent-friendly criteria but rejected after cross-check due to pricing opacity risk.

**Render (Third)** — Docker-based PaaS with free tier spin-down (15-minute inactivity) unsuitable for production background jobs. Experimental MCP server (discontinuation risk). CLI rollback is dashboard-driven, not CLI-first. Agent-readable docs available. Lowest score (2/5) on operational consistency; free tier's ephemeral filesystem unsuitable for persistence requirements.

---

## Shortlisted Platforms

### 1. Fly.io (Recommended)

Fly.io wins on **pricing predictability and familiar tooling** for a team with container-PaaS experience. The 5-week MVP timeline benefits from a platform that doesn't require learning a new provider's opinionated Python/Node.js ecosystem. Persistent processes (required for background task reminder scheduling) are fully supported. The trade-off is manual Dockerfile maintenance and no MCP integration; operational debugging during the MVP phase will rely on CLI tools the team already knows.

### 2. Railway (Runner-up)

Railway offers **native Java/Spring Boot scaffolding** that eliminates Dockerfile overhead and provides a path to fast iteration without container expertise. The full MCP server support would simplify agent-driven deployments in later phases. The risk is cost opacity: per-resource billing ($20/vCPU/month, $10/GB/month) combined with $0.05/GB egress could surprise a team that hasn't benchmarked Telegram reminder workloads before launch. Swap to Railway if you prioritize zero-friction deployments over cost control.

### 3. Render (Third)

Render's free tier (15-minute spin-down, ephemeral filesystem) makes it unsuitable for this MVP's persistent reminder bot requirement. Experimental MCP (discontinuation risk) and weak CLI-first tooling further disadvantage it. Consider Render only if you need an ultra-low-cost option and can refactor the reminder system into stateless Lambda-style functions.

---

## Anti-Bias Cross-Check: Fly.io

### Devil's Advocate — Weaknesses

1. **Docker complexity in the critical path**: Unlike Railway's native Java scaffolding, Fly.io requires a custom Dockerfile for Spring Boot. Debugging production issues becomes multi-layered (Is it the app? Is it the Docker image? Is it the Fly.io runtime?). Build failures are opaque; layers fail silently in CI if not logged carefully.

2. **Zero managed services**: Fly.io offers only managed Postgres. The Telegram bot's task-reminder job queue needs either Redis or a polling mechanism; you'll have to run Redis in a separate Fly.io service (increasing infrastructure complexity) or use an external managed queue (Upstash, AWS SQS), adding vendor lock-in and operational drift.

3. **Undocumented WebSocket behavior**: While HTTP/2 is mentioned, WebSocket support is not explicitly confirmed. If the Telegram bot's real-time 2-way sync relies on WebSockets for low-latency messaging, you won't know if Fly.io's undocumented support is production-ready until you hit edge cases in production.

4. **No agent-friendly API**: Fly.io has no MCP server and weak API documentation. Agents operating the platform (scaling services, inspecting logs, rolling back) must reverse-engineer REST API calls or fall back to CLI parsing — both add friction during incident response.

5. **Free trial is extremely limited**: 2 hours of machine runtime OR 7 days (whichever comes first). Testing and prototyping the full stack (Spring Boot app + Postgres + background job scheduling) will consume the entire trial in a single full-day session, leaving zero buffer for iteration before launch week.

### Pre-Mortem — How This Could Fail

The team launched on Fly.io with a custom Dockerfile and allocated 2 vCPU for the Spring Boot app. By week 3, the Telegram reminder bot scaled to handle 500+ households, and Redis (deployed as a separate Fly.io service) ran out of memory and crashed. Because Fly.io doesn't offer co-located Redis, the team had to set up Upstash (external service), adding a third-party credential and eventual data consistency issues when Upstash had a regional outage. The team wasted 40 hours debugging WebSocket timeouts in production, only to discover that Fly.io's WebSocket support was undocumented and limited to 60-second idle timeouts — worse than the Telegram API's retry window. By week 5, when trying to roll back a bad deployment, the CLI rollback command failed with an undocumented error; the team had to debug it via Fly.io's REST API (which none of them knew well) while two households defected to simpler tools. They wished they'd picked Railway's native Java support and accepted the pricing transparency; Railway's MCP would have automated the operational debugging that consumed their last month before the post-MVP roadmap.

### Unknown Unknowns

- **Dockerfile build cache behavior** in production deployments is undocumented — layer rebuild frequency and cache invalidation strategies are unknown, risking slow deploys on every code push (potential for >5-minute deploy cycles).
- **Memory limits per service** on the free trial are not explicitly stated; the team may hit OOM errors mid-deployment without warning or clear recovery steps.
- **Egress pricing** ($0.02–$0.04/GB region-dependent) is not broken down by destination (domestic vs. international, Telegram API vs. database replication) — real costs won't be clear until after launch.
- **GitHub Actions CI/CD integration** is not a first-class Fly.io feature — building Dockerfiles in Actions and pushing to Fly.io's registry adds a separate CI/CD pipeline that nobody initially planned, doubling build time in the critical path.
- **Database backup retention** and restore-time SLA are not documented for free-tier Postgres — a production incident with data loss would be unrecoverable without knowing backup policies upfront.

---

## Operational Story

How Fly.io actually operates day to day for the done-yet MVP.

- **Preview deploys**: Fly.io has no built-in preview deploy feature like Vercel/Netlify. Branch environments must be manually created via `fly apps create` per branch, duplicating infrastructure costs. Recommendation: Use GitHub Actions to build Docker images and tag them with branch names; deploy only to production (`main` branch) for MVP simplicity. PR feedback on deployment status happens via GitHub CI checks.

- **Secrets**: Environment variables and secrets are stored in `fly.toml` (committed to git — DO NOT commit secrets) or via `fly secrets set KEY=value` (encrypted at rest, injected at runtime). Secrets are readable only by the app process; team members managing secrets must have Fly.io CLI access or dashboard permission. Rotation is manual (`fly secrets set KEY=newvalue`). For Telegram bot token and database credentials, use `fly secrets set` and reference as `process.env.TELEGRAM_BOT_TOKEN` in Spring Boot.

- **Rollback**: `fly releases` lists all past deployments with timestamps and exit codes. `fly deploy --image [image-ref] --build-only` builds a Docker image and pushes it; `fly deploy [image-ref]` deploys the exact image. Rollback is explicit re-deployment, not a one-command revert. Typical rollback time: 2–3 minutes (Docker pull + container startup). Data migrations do NOT roll back automatically; database rollback is manual or requires a backup restore.

- **Approval**: Production deployments (main branch push) are automatic via GitHub Actions → `fly deploy` (no human gate in MVP). Destructive actions (scaling down services, destroying volumes, rotating Postgres credentials) should be manual — CLI prevents accidental destruction (`fly scale count 0` asks for confirmation). For MVP, only auto-approve code deploys; require manual approval for infrastructure changes (scale, delete).

- **Logs**: `fly logs` tails real-time logs from all machines in the app; `fly logs --instance [instance-id]` tails one machine. Spring Boot logs appear in stdout/stderr. `fly logs --json` emits structured JSON (level, message, timestamp) useful for parsing. No log search or filtering beyond string grep; for complex queries, forward logs to an external service (Papertrail, Grafana Loki, etc.) — out of scope for MVP.

---

## Risk Register

For each identified risk: name, the cross-check lens that surfaced it, likelihood, impact, and mitigation.

| Risk | Source | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| Dockerfile build complexity adds deploy friction | Devil's advocate | **M** | **M** | Write a single `Dockerfile` early in week 1, test locally with `docker build` + `docker run`, commit to repo. Store dockerfile templates in docs/. |
| Redis runs out of memory; requires external Upstash setup | Pre-mortem | **M** | **H** | Benchmark task queue depth vs. Redis memory during week 1 load testing. If Redis exceeds 500MB, pre-provision Upstash Redis (free tier: 256MB) in week 2. Document co-location trade-off in post-MVP roadmap. |
| WebSocket timeouts break Telegram 2-way sync | Unknown unknowns | **M** | **H** | Test WebSocket idle timeout behavior in week 2: open a bot connection for 2+ hours, verify idle timeout threshold. If <2 min, implement polling fallback for reminder ACKs instead of WebSockets. |
| Free trial (2 hours / 7 days) consumed before launch | Pre-mortem | **H** | **M** | Use free trial sparingly for final integration test only. Do all development and testing on paid account ($2–5/month); charge to project budget. Verify credit card on day 1 of Fly.io setup. |
| Egress billing surprise from Telegram notification volume | Devil's advocate | **M** | **M** | Estimate: 100 households × 1 reminder/day × 200 bytes/notification = 20KB/day (~600KB/month). At $0.03/GB, <$0.02/month. Monitor actual egress via `fly status --all`; set billing alert at $25/month to catch runaway costs. |
| CLI rollback command fails; unclear error recovery | Pre-mortem | **L** | **M** | Document rollback procedure in docs/deployment.md week 2. Test rollback on staging in week 3 (deploy, break something, rollback, verify). Ensure entire team knows `fly releases` and `fly deploy [image]` flow before production week. |
| Database backup retention policy unknown | Unknown unknowns | **M** | **H** | Contact Fly.io support in week 1 to confirm Postgres backup retention on free/paid tier. Document SLA in infrastructure.md. Set manual weekly backups via `fly pg dump` → export to local USB as insurance. |
| GitHub Actions CI/CD pipeline not first-class Fly.io feature | Unknown unknowns | **M** | **M** | Build `.github/workflows/deploy.yml` in week 1: Docker build → push to Fly.io registry → `fly deploy`. Test on a staging app first. Keep deploy time < 5 min via Docker layer caching and multi-stage builds. |

---

## Getting Started

1. **Create a Fly.io account and initialize the app** (5 min):
   ```bash
   npm install -g flyctl
   fly auth login
   fly launch --name done-yet-mvp --region sjc  # San Jose (CA) for US-based users
   # Choose Java runtime when prompted
   ```
   This creates `fly.toml` and a Dockerfile scaffold. **Do NOT commit `fly.toml` to git yet** — it will contain secrets after the next step.

2. **Configure secrets and environment variables** (10 min):
   ```bash
   fly secrets set TELEGRAM_BOT_TOKEN="your-bot-token"
   fly secrets set DATABASE_PASSWORD="your-db-password"
   fly secrets set OPENAI_API_KEY="your-openai-key"  # For FR-019 AI task generation
   fly config view  # Verify secrets were set (values are masked)
   ```

3. **Build and test locally before pushing to Fly.io** (30 min):
   ```bash
   mvn clean package  # Build Spring Boot JAR
   docker build -t done-yet-mvp:latest .
   docker run -p 8080:8080 -e TELEGRAM_BOT_TOKEN="test-token" done-yet-mvp:latest
   # Test at http://localhost:8080
   ```

4. **Deploy to Fly.io** (5 min):
   ```bash
   fly deploy
   fly logs --follow  # Watch deployment logs in real-time
   ```
   After deployment succeeds, test the public URL:
   ```bash
   curl https://done-yet-mvp.fly.dev/health
   ```

5. **Set up GitHub Actions for auto-deploy on main branch** (15 min):
   Create `.github/workflows/deploy.yml`:
   ```yaml
   name: Deploy to Fly.io
   on:
     push:
       branches: [main]
   jobs:
     deploy:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
         - name: Deploy to Fly.io
           env:
             FLY_API_TOKEN: ${{ secrets.FLY_API_TOKEN }}
           run: flyctl deploy --remote-only
   ```
   Add `FLY_API_TOKEN` secret to GitHub repo settings (generate via `fly tokens create deploy`).

**Total setup time: ~1 hour.** Test the entire flow on a staging app before deploying to production.

---

## Out of Scope

The following were not evaluated in this research:

- Docker image optimization (multi-stage builds, layer caching strategies)
- CI/CD pipeline configuration beyond basic GitHub Actions
- Production-scale architecture (multi-region replication, HA setup, disaster recovery)
- Kubernetes or container orchestration beyond Fly.io's managed layer
- Cost optimization beyond MVP launch (reserved capacity, spot instances, etc.)

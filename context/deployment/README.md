# Deployment Context

This directory tracks deployment planning and execution for the `10x-cli` MVP.

## Files

- **[deploy-plan.md](./deploy-plan.md)** — Approved deployment plan for Fly.io
  - Status tracking with checkboxes for each phase
  - Exact commands for each step
  - Risk register tied to infrastructure research
  - Rollback procedures
  - External integration checklist

## Phases

The deployment follows 8 phases:

1. ✓ **Pre-Deployment Verification** — Environment checks, codebase readiness
2. ✓ **Dockerfile Creation** — Multi-stage Java 21 build (committed)
3. ⏳ **Fly.io Account Setup** — Initialize app, configure secrets
4. ⏳ **Database Setup** — Postgres provisioning (optional)
5. ⏳ **Deploy to Fly.io** — Build, push, and verify
6. ⏳ **GitHub Actions CI/CD** — Automated deploy on main push
7. ⏳ **Verification & Rollback** — Health checks, cost monitoring
8. ⏳ **Documentation** — Runbook and hand-off

## Prerequisites

- [x] Dockerfile created and committed
- [ ] Fly.io account with payment method on file
- [ ] flyctl CLI installed and authenticated
- [ ] GitHub repository with Actions enabled
- [ ] Telegram bot token (if using Telegram integration)
- [ ] OpenAI API key (if using FR-019 AI features)

## Quick Start

```bash
# Install flyctl
npm install -g flyctl

# Authenticate with Fly.io
flyctl auth login

# Initialize Fly.io app
flyctl launch --name 10x-cli-mvp --region sjc

# Configure secrets
flyctl secrets set TELEGRAM_BOT_TOKEN="<token>"
flyctl secrets set DATABASE_PASSWORD="<password>"
flyctl secrets set OPENAI_API_KEY="<key>"

# Deploy
flyctl deploy

# Verify
curl https://10x-cli-mvp.fly.dev/health
```

## Timeline

Total execution time: ~55 minutes (excluding troubleshooting)

- Pre-flight checks: 10 min
- Fly.io setup: 10 min
- Database setup: 10 min (optional)
- Deploy & verify: 15 min
- GitHub Actions: 10 min

## Reference

- **Infrastructure Research**: `../foundation/infrastructure.md`
- **Tech Stack**: `../foundation/tech-stack.md`
- **Project README**: `../../README.md`
- **CLAUDE.md**: `../../CLAUDE.md`

## Support

If deployment fails:
1. Check logs: `flyctl logs --follow`
2. Review troubleshooting section in `deploy-plan.md`
3. Reference infrastructure.md risk register for mitigation strategies

---

**Last Updated**: 2026-08-25  
**Status**: Ready for deployment (phases 3–8 pending)

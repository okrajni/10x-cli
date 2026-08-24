---
starter_id: vite-react
package_manager: npm
project_name: is-it-done
hints:
  language_family: js
  team_size: solo
  deployment_target: cloudflare-pages
  ci_provider: github-actions
  ci_default_flow: auto-deploy-on-merge
  bootstrapper_confidence: verified
  path_taken: custom
  quality_override: false
  self_check_answers:
    typed: true
    from_official_starter: true
    conventions: true
    docs_current: true
    can_judge_agent: false
  has_auth: true
  has_payments: false
  has_realtime: true
  has_ai: true
  has_background_jobs: false
---

## Full Stack Architecture

This is a **decoupled full-stack project** with two independent starters:

### Frontend
- **Starter:** Vite + React
- **Language:** JavaScript / TypeScript
- **Package manager:** npm
- **Deployment:** Cloudflare Pages
- **Features:** Real-time WebSocket, authentication UI, AI task suggestion interface, task management dashboard
- **CI/CD:** GitHub Actions → auto-deploy on merge to main

### Backend
- **Starter:** Spring Boot (Java)
- **Language:** Java
- **Package manager:** Maven
- **Deployment:** Fly (recommended; Railway or Render also viable)
- **Features:** RESTful API, PostgreSQL persistence, authentication logic, Telegram bot webhooks, AI task generation, WebSocket endpoint for real-time sync, background job scheduling (@Scheduled)
- **CI/CD:** GitHub Actions (separate workflow for backend; can share repo as monorepo or separate repos)

## Why this stack

Solo developer building a household task coordinator in 5 weeks (after-hours). Custom path chosen to balance frontend and backend concerns: Vite + React is lightweight, TypeScript-first, verified bootstrapper confidence, and clears all agent-friendly gates—ideal for a real-time UI with instant Telegram sync. Spring Boot is the Java default, batteries-included with auth, database, WebSocket, and scheduling—perfect for handling the backend's coordination complexity (task persistence, Telegram integration, AI suggestions). Both are popular in training data, well-documented, and agent-friendly. Deployment: frontend to Cloudflare Pages for edge speed; backend to Fly for containerized simplicity. Both share GitHub Actions CI/CD and can deploy independently.

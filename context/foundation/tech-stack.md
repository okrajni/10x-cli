---
starter_id: spring
package_manager: maven
project_name: done-yet
hints:
  language_family: java
  team_size: solo
  deployment_target: fly
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
  has_auth: true
  has_payments: false
  has_realtime: true
  has_ai: true
  has_background_jobs: true
---

## Why this stack

Spring Boot (Java backend) + React (JavaScript frontend) + PostgreSQL aligns your 5-week MVP timeline with your Java expertise. Spring Boot's built-in auth, DI container, and spring-ai module directly support the AI task-generation feature (FR-019–FR-020) via Gemini API integration. A separate React frontend keeps the UI layer independent and fast-iterating. PostgreSQL (running in Docker locally) provides a battle-tested relational database for household task schemas with strong schema control. Fly's container-native deploys and GitHub Actions CI minimize ops friction early, letting you focus on features. This Java+React split exploits your stated comfort with both ecosystems while keeping the scaffolding straightforward and agent-friendly.

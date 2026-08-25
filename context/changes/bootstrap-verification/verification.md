---
bootstrapped_at: 2026-08-25T12:06:00Z
starter_id: spring
starter_name: Spring Boot
project_name: done-yet
language_family: java
package_manager: maven
cwd_strategy: subdir-then-move
bootstrapper_confidence: verified
phase_3_status: ok
audit_command: null
post_setup_status: complete
---

## Hand-off

**Starter**: spring (Spring Boot)  
**Project name**: done-yet  
**Package manager**: maven  
**Language family**: java  
**Confidence**: verified  
**Path taken**: custom  
**Deployment target**: fly  
**Feature flags**: has_auth, has_realtime, has_ai, has_background_jobs

### Why this stack

Spring Boot (Java backend) + React (JavaScript frontend) + MySQL aligns your 5-week MVP timeline with your Java expertise. Spring Boot's built-in auth, DI container, and spring-ai module directly support the AI task-generation feature (FR-019–FR-020). A separate React frontend keeps the UI layer independent and fast-iterating. MySQL provides a battle-tested relational database for household task schemas. Fly's container-native deploys and GitHub Actions CI minimize ops friction early, letting you focus on features. This Java+React split exploits your stated comfort with both ecosystems while keeping the scaffolding straightforward and agent-friendly.

## Pre-scaffold verification

| Signal          | Value | Severity | Notes                    |
| --------------- | ----- | -------- | ------------------------ |
| Recency check   | N/A   | -        | Java ecosystem; no npm package to check |
| GitHub docs URL | N/A   | -        | docs_url points to docs.spring.io (not GitHub) |

## Scaffold log

**Resolved invocation**: `curl -s https://start.spring.io/starter.tgz -d dependencies=web,devtools -d type=maven-project -d javaVersion=21 -d groupId=com.example -d artifactId=.bootstrap-scaffold | tar -xzf -`  
**Strategy**: scaffold into a temp directory then move files up  
**Exit code**: 0  
**Files moved**: mvnw, mvnw.cmd, pom.xml, HELP.md, .gitattributes, .mvn/ (directory), src/main/, src/test/ (directories + contents)  
**Conflicts (.scaffold siblings)**: .gitignore.scaffold (later merged with Spring Boot version)  
**.gitignore handling**: append-merged with Spring Boot patterns  
**.bootstrap-scaffold cleanup**: deleted

### Post-scaffold customization

**Placeholder replacement**:
- ✓ pom.xml artifactId: `demo` → `done-yet`
- ✓ pom.xml project name: `DemoApplication` → `DoneYetApplication`
- ✓ Java package: `com.example.demo` → `com.example.doneyet`
- ✓ Main class renamed: `DemoApplication.java` → `DoneYetApplication.java`
- ✓ Test class renamed: `DemoApplicationTests.java` → `DoneYetApplicationTests.java`

**Database configuration**:
- ✓ MySQL 8.0 installed and running locally (Homebrew)
- ✓ Database `done_yet` created
- ✓ application.properties updated with MySQL connection (localhost:3306)
- ✓ Removed explicit Hibernate dialect (auto-detected by Spring Boot 4.1.1)
- ✓ JPA configured with `create-drop` DDL auto-update

**Build & verification**:
- ✓ Maven clean package: 50 MB JAR created
- ✓ Application started successfully on port 8080
- ✓ Database connection verified

## Post-scaffold audit

**Tool**: skipped — no built-in audit tool for java  
**Recommended external tool**: OWASP Dependency-Check or Snyk

Java ecosystem security audits typically require external tools. Consider configuring OWASP Dependency-Check via Maven plugin or Snyk CLI for dependency scanning in your CI/CD pipeline.

### Application verification

**Build status**: ✓ SUCCESS  
**JAR file**: `target/done-yet-0.0.1-SNAPSHOT.jar` (50 MB)  
**Startup test**: ✓ PASSED  
**Database connection**: ✓ CONNECTED  
**Application endpoint**: http://localhost:8080

## Hints recorded but not acted on

| Hint                    | Value             |
| ----------------------- | ----------------- |
| bootstrapper_confidence | verified          |
| quality_override        | false             |
| path_taken              | custom            |
| team_size               | solo              |
| deployment_target       | fly               |
| ci_provider             | github-actions    |
| ci_default_flow         | auto-deploy-on-merge |
| has_auth                | true              |
| has_payments            | false             |
| has_realtime            | true              |
| has_ai                  | true              |
| has_background_jobs     | true              |

These hints were carried forward from the tech-stack hand-off for future skills to act on (e.g., agent context setup, CI/CD scaffolding). v1 bootstrapper surfaces but does not act on them.

## Next steps

Your project is fully scaffolded, configured, and verified — ready for development!

**Completed setup**:
- ✓ Spring Boot 4.1.1 with Java 21 support
- ✓ Project identifiers customized (`done-yet`, `com.example.doneyet`)
- ✓ MySQL 8.0 installed and running
- ✓ Database `done_yet` created with JPA auto-schema management
- ✓ Application builds successfully (50 MB JAR)
- ✓ Application starts and connects to database on port 8080

**Remaining manual work**:
1. `git init` to initialize your repository history.
2. Review `.scaffold` sibling files (if any remain) from the conflict policy and decide which versions to keep.
3. Start the application with `./mvnw spring-boot:run` to begin development.
4. Define your JPA entities in `src/main/java/com/example/doneyet/` for task management:
   - Task entity with fields: id, title, description, completed, createdAt, dueDate, etc.
   - User entity for authentication (Spring Security support ready)
   - Category/Tag entities for organizing tasks
5. Create REST controllers in a new `controller/` package to expose task APIs.
6. Integrate Spring AI module for the AI task-generation feature (FR-019–FR-020).
7. Set up Spring Security for authentication/authorization.
8. Create a React frontend (separate project or module) to consume the Spring Boot APIs.

**Deployment to Fly.io** (when ready):
- Build the JAR: `./mvnw clean package`
- Configure `fly.toml` for Java 21 + MySQL connection
- Set up production MySQL on Fly.io or use an external managed database
- Deploy: `fly deploy`

A future skill will set up agent context (CLAUDE.md, AGENTS.md) and handle CI/CD scaffolding with GitHub Actions auto-deploy-on-merge flow.

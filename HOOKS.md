# Git Hooks Configuration

## Pre-commit Hooks

Projekt używa **Lefthook** do automatycznego uruchamiania kontroli jakości kodu przed commitem.

### Instalacja

Hooki są instalowane automatycznie podczas `bun install` (postinstall script).

Aby zainstalować manualnie:
```bash
bun install
lefthook install
```

### Co się uruchamia?

Przed każdym commitem (`pre-commit` stage) wykonywane są:

#### Frontend (TypeScript)
1. **Lint** — `bun run lint {staged_files}`
   - Sprawdza style i błędy w kodzie TypeScript (Oxlint)
   - Działa na staged `.ts(x)` files

2. **Typecheck** — `bun run typecheck`
   - Pełna analiza typów TypeScript
   - Zapewnia poprawność typów

3. **Tests** — `bun test --run {staged_files}`
   - Uruchamia testy związane ze zmienionymi plikami
   - Działa na staged `.ts(x)` files

#### Backend (Java/Spring Boot)
4. **Compile** — `mvn clean compile -q`
   - Kompilacja kodu Java
   - Uruchamia się dla staged `.java` files

5. **Tests** — `mvn test -q`
   - Uruchamia testy JUnit
   - Uruchamia się dla staged `.java` files

### Konfiguracja

- `lefthook.yml` — definicja pre-commit hooków
- `package.json` — `postinstall` skrypt instaluje hooki

### Lokalne override'y

Lokalne override'y hook'ów można umieścić w `.lefthook-local/` (ignorowane przez git).

Aby wyłączyć hooki tymczasowo:
```bash
lefthook uninstall
```

Aby zainstalować ponownie:
```bash
lefthook install
```

### Wymuszenie commitu pomimo błędów

```bash
git commit --no-verify
```

⚠️ Używaj tylko gdy jesteś pewny, że wiesz co robisz.

## Workflow

### Frontend changes:
```bash
git add src/services/api.ts
git commit -m "..."   # 🔍 Uruchomią się:
                      #    - lint (Oxlint)
                      #    - typecheck (TypeScript)
                      #    - bun test (testy związane z plikiem)
                      # ✅ Jeśli wszystko OK → commit
                      # ❌ Jeśli błędy → zablokowany
```

### Backend changes:
```bash
git add src/main/java/com/example/controller/*.java
git commit -m "..."   # 🔍 Uruchomią się:
                      #    - mvn compile (kompilacja)
                      #    - mvn test (testy JUnit)
                      # ✅ Jeśli wszystko OK → commit
                      # ❌ Jeśli błędy → zablokowany
```

### Mieszane zmiany:
```bash
git add src/main/java/com/example/api/*.java frontend/src/hooks.ts
git commit -m "..."   # 🔍 Uruchomią się wszystkie hooki (frontend + backend)
```

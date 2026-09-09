# Playwright E2E Tests Setup

This directory contains Playwright configuration and utilities for end-to-end testing.

## Authentication Setup

The test suite uses a **fixture-based authentication** pattern that combines:
- A **global setup** that runs once before tests, performs login, and caches auth state
- **Test fixtures** that efficiently provide authenticated pages to tests

This approach:
- ✅ Speeds up tests (login cached on disk, tests reuse fixture)
- ✅ Reduces login API calls (only once per hour by default)
- ✅ Enables parallel test execution safely
- ✅ Keeps tests focused on features, not authentication logic

### How It Works

1. **Global Setup (`global-setup.ts`)**
   - Runs once before all tests
   - Performs a single login with test credentials
   - Saves auth state (cookies, localStorage) to `playwright/.auth/user.json`
   - Caches the state for up to 1 hour (configurable)

2. **Test Fixtures (`playwright/fixtures.ts`)**
   - Provides an `authenticatedPage` fixture that tests can use
   - Efficiently handles auth by leveraging browser-level auth state when possible
   - Falls back to login if needed

3. **Auth Credentials**: 
   ```
   Email: test1@test.com
   Password: 123123123
   ```

### Usage

#### Running Tests

```bash
# Run all tests
npx playwright test

# Run specific test file
npx playwright test tests/e2e/login.spec.ts

# Run in headed mode (see browser)
npx playwright test --headed

# Run in debug mode (step through)
npx playwright test --debug
```

#### Writing Tests with Authentication

Use the `authenticatedPage` fixture to get a pre-authenticated page:

```typescript
import { test, expect } from '../../playwright/fixtures';

test('my authenticated test', async ({ authenticatedPage: page }) => {
  // page is already authenticated and logged in
  await expect(page).toHaveURL('/dashboard');

  // Write your test - you're ready to interact with protected features
  await page.getByRole('button', { name: 'Create' }).click();
  // Continue testing...
});
```

#### Standard Playwright Tests (without auth)

If your test doesn't need authentication, use standard Playwright imports:

```typescript
import { test, expect } from '@playwright/test';

test('unauthenticated navigation', async ({ page }) => {
  await page.goto('/login');
  await expect(page).toHaveURL('/login');
});
```

#### Creating a Fresh Unauthenticated Context

To test unauthenticated behavior (e.g., redirects to login):

```typescript
test('unauthenticated users are redirected', async ({ browser }) => {
  const freshContext = await browser.newContext(); // No stored auth
  const page = await freshContext.newPage();

  await page.goto('/dashboard');
  await expect(page).toHaveURL('/login');

  await freshContext.close();
});
```

## File Structure

```
playwright/
├── global-setup.ts      # Performs initial login & saves auth state
├── fixtures.ts          # Optional test fixtures for auth helpers
└── README.md            # This file
```

## Auth State File

- **Location**: `playwright/.auth/user.json` (created by global setup)
- **Contents**: Browser storage state (cookies, localStorage, sessionStorage)
- **.gitignore**: Added to prevent committing auth tokens
- **Regeneration**: Delete the file to force re-authentication on next test run

## Tips

- **Parallel Execution**: Tests run in parallel by default. Use unique timestamps or test IDs in your test data to avoid conflicts.
- **CI/CD**: Global setup runs automatically in CI. Ensure your test backend is running before tests.
- **Debugging**: Use `--headed` and `--debug` flags to see what's happening in the browser.
- **Rerunning Tests**: Cached auth state is reused. If auth expires, delete `playwright/.auth/user.json` to regenerate.

## Troubleshooting

### "Auth file not found"
- Run `npx playwright test` once to generate the auth state
- Or manually run: `npx playwright test --setup` (if Playwright 1.40+)

### "Tests stuck on login page"
- The auth credentials may be invalid
- Update `playwright/global-setup.ts` with correct credentials
- Delete `playwright/.auth/user.json` and re-run tests

### "Tests timeout during global setup"
- Ensure the dev server is running: `npm run dev` (in frontend directory)
- Check that `baseURL` in `playwright.config.ts` matches your server address

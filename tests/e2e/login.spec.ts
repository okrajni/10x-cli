import { test, expect } from '../../playwright/fixtures';

test('authenticated user can access dashboard', async ({ authenticatedPage }) => {
  const page = authenticatedPage;
  // Already authenticated via fixture
  await expect(page).toHaveURL('/dashboard');

  // Verify dashboard is visible
  const dashboardHeading = page.locator('h1').first();
  await expect(dashboardHeading).toBeVisible();
});

test('dashboard shows authenticated user content', async ({ authenticatedPage, context }) => {
  const page = authenticatedPage;

  // Verify we're on dashboard
  await expect(page).toHaveURL('/dashboard');
  const dashboardHeading = page.locator('h1').first();
  await expect(dashboardHeading).toBeVisible();

  // Verify cookies are present
  const cookies = await context.cookies();
  expect(cookies.length).toBeGreaterThan(0);
});

test('protected pages redirect unauthenticated users to login', async ({ browser }) => {
  // Create a fresh context without stored auth
  const freshContext = await browser.newContext();
  const page = await freshContext.newPage();

  // Navigate to protected dashboard without auth
  await page.goto('/dashboard');

  // Should redirect to login
  await expect(page).toHaveURL('/login');

  await freshContext.close();
});

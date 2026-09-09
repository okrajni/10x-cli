import { test as base, expect, Page } from '@playwright/test';

type AuthFixtures = {
  authenticatedPage: Page;
};

export const test = base.extend<AuthFixtures>({
  authenticatedPage: async ({ page }, use) => {
    // Perform login
    const email = 'test1@test.com';
    const password = '123123123';

    await page.goto('/login');
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password').fill(password);
    await page.getByRole('button', { name: /sign in/i }).click();

    // Wait for successful navigation and dashboard to load
    await expect(page).toHaveURL('/dashboard', { timeout: 10000 });
    await page.locator('h1').first().waitFor({ state: 'visible', timeout: 10000 });

    // Page is now authenticated and ready for tests
    await use(page);
  },
});

export { expect };

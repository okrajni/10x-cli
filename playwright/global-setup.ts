import { chromium, FullConfig } from '@playwright/test';
import fs from 'fs';
import path from 'path';

const authFile = 'playwright/.auth/user.json';

async function globalSetup(config: FullConfig) {
  // Skip if auth file already exists and is recent (less than 1 hour old)
  if (fs.existsSync(authFile)) {
    const stats = fs.statSync(authFile);
    const ageInMinutes = (Date.now() - stats.mtimeMs) / 1000 / 60;
    if (ageInMinutes < 60) {
      console.log('✓ Using cached auth state (fresh)');
      return;
    }
  }

  // Ensure auth directory exists
  const authDir = path.dirname(authFile);
  if (!fs.existsSync(authDir)) {
    fs.mkdirSync(authDir, { recursive: true });
  }

  console.log('🔐 Performing global authentication...');
  const baseURL = config.use?.baseURL || 'http://localhost:5173';
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    // Navigate to login page
    await page.goto(`${baseURL}/login`);

    // Fill in login form with test credentials
    await page.getByLabel('Email').fill('test1@test.com');
    await page.getByLabel('Password').fill('123123123');

    // Submit login form
    await page.getByRole('button', { name: /sign in/i }).click();

    // Wait for navigation to dashboard
    await page.waitForURL('**/dashboard', { timeout: 15000 });

    // Wait for dashboard to be fully loaded
    await page.locator('h1').first().waitFor({ state: 'visible', timeout: 10000 });

    // Save auth state (cookies, localStorage, sessionStorage)
    await context.storageState({ path: authFile });
    console.log('✓ Auth state saved successfully');
  } catch (error) {
    console.error('✗ Global setup failed:', error instanceof Error ? error.message : String(error));
    throw error;
  } finally {
    await browser.close();
  }
}

export default globalSetup;

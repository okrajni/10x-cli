import { test, expect } from '../../playwright/fixtures';

test('created task appears in list (proves not lost/hidden in database)', async ({ authenticatedPage: page }) => {
  // Setup: unique task name with timestamp to avoid collisions
  const taskName = `Visible Task ${Date.now()}`;
  const todayDate = new Date().toISOString().split('T')[0];

  // Already authenticated via fixture
  await expect(page).toHaveURL('/dashboard');

  // Navigate to task creation from dashboard
  await page.getByRole('button', { name: 'Create task' }).click();
  await expect(page).toHaveURL('/task/create');

  // Create task due today
  await page.getByRole('textbox', { name: 'Task Title *' }).fill(taskName);
  await page.getByRole('textbox', { name: 'Due Date' }).fill(todayDate);
  await page.getByRole('button', { name: 'Create Task' }).click();

  // Navigate to task list
  await page.getByRole('button', { name: 'Go to List' }).click();
  await expect(page).toHaveURL('/task');

  // Assert: created task appears in list immediately after save (proves it was persisted to database and returned by API)
  // This directly tests the "data visibility" risk - if the task query didn't include this task or if it was lost, this fails
  const taskCard = page.locator('div.rounded-card').filter({ hasText: taskName });
  await expect(taskCard).toBeVisible();
  await expect(taskCard.getByRole('heading', { name: taskName })).toBeVisible();

  // Verify task content is accessible (not truncated, not corrupted)
  const taskHeading = taskCard.getByRole('heading', { name: taskName });
  const headingText = await taskHeading.textContent();
  expect(headingText?.trim()).toBe(taskName);

  // Cleanup: Delete the created task
  await taskCard.getByRole('button', { name: 'Delete' }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByRole('button', { name: 'Delete' }).click();
});

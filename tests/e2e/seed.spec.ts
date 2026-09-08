import { test, expect } from '../../playwright/fixtures';

test('created task is visible on the Tasks list', async ({ authenticatedPage: page }) => {
  const taskName = `Task ${Date.now()}`;

  await page.getByRole('button', { name: 'Create task'}).click();
  await expect(page).toHaveURL('/task/create');


  await page.getByRole('textbox', { name: 'Task Title *' }).fill(taskName);
  await page.getByRole('button', { name: 'Create Task' }).click();
  await page.getByRole('button', { name: 'Go to List' }).click();

  await expect(page).toHaveURL('/task');

  const taskCard = page
  .locator('div.rounded-card')
  .filter({ hasText: taskName });

  await expect(
    taskCard.getByRole('heading', { name: taskName })
    ).toBeVisible();

  // Cleanup
  await taskCard.getByRole('button', { name: 'Delete' }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByRole('button', { name: 'Delete' }).click();
});


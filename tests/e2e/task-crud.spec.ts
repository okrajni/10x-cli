import { test, expect } from '../../playwright/fixtures';

test('user can create a task', async ({ authenticatedPage: page }) => {
  // Setup: unique task name to ensure isolation (allows parallel + re-run safety)
  const taskName = `Task Create ${Date.now()}`;
  const dueDate = new Date().toISOString().split('T').at(0) ?? '';

  // Action: navigate to dashboard and create task
  await expect(page).toHaveURL('/dashboard');
  await page.getByRole('button', { name: 'Create task' }).click();
  await expect(page).toHaveURL('/task/create');

  // Fill and submit task creation form
  await page.getByRole('textbox', { name: 'Task Title *' }).fill(taskName);
  await page.locator('select#category').selectOption('CLEANING');
  await page.locator('input#dueDate').fill(dueDate);
  await page.getByRole('button', { name: 'Create Task' }).click();
  await page.getByRole('button', { name: 'Go to List' }).click();
  await expect(page).toHaveURL('/task');

  // Assert: task appears in list (proves creation persisted)
  const taskCard = page.locator('div.rounded-card').filter({ hasText: taskName });
  await expect(taskCard).toBeVisible();
  await expect(taskCard.getByRole('heading', { name: taskName })).toBeVisible();
});

test('user can mark a task as complete', async ({ authenticatedPage: page }) => {
  // Setup: create a task first (own setup, independent state)
  const taskName = `Task Complete ${Date.now()}`;
  const dueDate = new Date().toISOString().split('T').at(0) ?? '';

  await expect(page).toHaveURL('/dashboard');
  await page.getByRole('button', { name: 'Create task' }).click();
  await expect(page).toHaveURL('/task/create');
  await page.getByRole('textbox', { name: 'Task Title *' }).fill(taskName);
  await page.locator('select#category').selectOption('CLEANING');
  await page.locator('input#dueDate').fill(dueDate);
  await page.getByRole('button', { name: 'Create Task' }).click();
  await page.getByRole('button', { name: 'Go to List' }).click();
  await expect(page).toHaveURL('/task');

  // Action: mark task complete
  const taskCard = page.locator('div.rounded-card').filter({ hasText: taskName });
  await expect(taskCard).toBeVisible();
  // Match button starting with "Mark" (for "Mark ... complete" aria-labels)
  await taskCard.getByRole('button', { name: /^Mark/ }).first().click();

  // Assert: task disappears from active list (moved to completed)
  await page.getByRole('button', { name: 'Active' }).click();
  const activeTaskCards = page.locator('div.rounded-card').filter({ hasText: taskName });
  await expect(activeTaskCards).toHaveCount(0);

  // Assert: task appears in completed section
  await page.getByRole('button', { name: 'Completed' }).click();
  const completedTaskCard = page.locator('div.rounded-card').filter({ hasText: taskName });
  await expect(completedTaskCard).toBeVisible();
});

test('user can edit a task', async ({ authenticatedPage: page }) => {
  // Setup: create a task to edit
  const originalName = `Task Edit Original ${Date.now()}`;
  const editedName = `Task Edit Updated ${Date.now()}`;
  const editedDescription = 'Updated via edit';
  const dueDate = new Date().toISOString().split('T').at(0) ?? '';

  await expect(page).toHaveURL('/dashboard');
  await page.getByRole('button', { name: 'Create task' }).click();
  await expect(page).toHaveURL('/task/create');
  await page.getByRole('textbox', { name: 'Task Title *' }).fill(originalName);
  await page.locator('select#category').selectOption('CLEANING');
  await page.locator('input#dueDate').fill(dueDate);
  await page.getByRole('button', { name: 'Create Task' }).click();
  await page.getByRole('button', { name: 'Go to List' }).click();
  await expect(page).toHaveURL('/task');

  // Action: navigate to edit page and update task
  const taskCard = page.locator('div.rounded-card').filter({ hasText: originalName });
  await expect(taskCard).toBeVisible();
  // Match button starting with "Edit" (not "Mark ... Edit ..." or "Delete ...")
  const editButton = taskCard.getByRole('button', { name: /^Edit/ }).first();
  await editButton.click();
  await page.waitForLoadState('networkidle');
  await expect(page).toHaveURL(/\/task\/.*\/edit/);

  const titleField = page.locator('input#title');
  await titleField.clear();
  await titleField.fill(editedName);
  const descriptionField = page.locator('textarea#description');
  await descriptionField.clear();
  await descriptionField.fill(editedDescription);
  await page.getByRole('button', { name: 'Save Changes' }).click();
  await expect(page).toHaveURL('/task');

  // Assert: edited task appears with new details
  const editedTaskCard = page.locator('div.rounded-card').filter({ hasText: editedName });
  await expect(editedTaskCard).toBeVisible();
  await expect(editedTaskCard.getByRole('heading', { name: editedName })).toBeVisible();
  const descriptionElement = editedTaskCard.locator('p').filter({ hasText: editedDescription });
  await expect(descriptionElement).toBeVisible();
});

test('user can delete a task', async ({ authenticatedPage: page }) => {
  // Setup: create a task to delete
  const taskName = `Task Delete ${Date.now()}`;
  const dueDate = new Date().toISOString().split('T').at(0) ?? '';

  await expect(page).toHaveURL('/dashboard');
  await page.getByRole('button', { name: 'Create task' }).click();
  await expect(page).toHaveURL('/task/create');
  await page.getByRole('textbox', { name: 'Task Title *' }).fill(taskName);
  await page.locator('select#category').selectOption('CLEANING');
  await page.locator('input#dueDate').fill(dueDate);
  await page.getByRole('button', { name: 'Create Task' }).click();
  await page.getByRole('button', { name: 'Go to List' }).click();
  await expect(page).toHaveURL('/task');

  // Action: delete the task via button + confirmation
  const taskCard = page.locator('div.rounded-card').filter({ hasText: taskName });
  await expect(taskCard).toBeVisible();
  // Match button starting with "Delete" (not other buttons with "Delete" in the name)
  const deleteButton = taskCard.getByRole('button', { name: /^Delete/ }).first();
  await expect(deleteButton).toBeVisible();
  await deleteButton.click();

  // Confirm deletion via dialog (try multiple role selectors)
  const deleteDialog = page.locator('[role="dialog"]').first();
  if (await deleteDialog.isVisible()) {
    await deleteDialog.getByRole('button', { name: 'Delete' }).click();
  }

  // Assert: task is removed from list (proves deletion persisted)
  await page.waitForURL('/task');
  const deletedTaskCard = page.locator('div.rounded-card').filter({ hasText: taskName });
  await expect(deletedTaskCard).toHaveCount(0);
});

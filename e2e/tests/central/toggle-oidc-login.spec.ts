import { test, expect } from '@playwright/test';

test('toggle oidc', async ({ page }) => {
  await page.goto('/apps/central/#/');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.getByPlaceholder('Enter username').click();
  await page.getByPlaceholder('Enter username').fill('admin');
  await page.getByPlaceholder('Password').click();
  await page.getByPlaceholder('Password').fill('admin');
  await page.getByRole('dialog').getByRole('button', { name: 'Sign in' }).click();
  await page.getByRole('link', { name: 'Admin' }).click();
  await page.getByRole('link', { name: 'Settings' }).click();
  await page.getByRole('row', { name: '  isOidcEnabled false' }).getByRole('button').first().click();
  await page.locator('textarea').click();
  await page.locator('textarea').press('ControlOrMeta+a');
  await page.locator('textarea').fill('true');
  await page.getByRole('button', { name: 'Edit Setting' }).click();
  await page.getByRole('button', { name: 'Sign out' }).click();
  await page.goto('/apps/central/#/admin/settings');
  await page.goto('/apps/central/');
  await page.goto('/apps/central/#/');
  await page.getByRole('button', { name: 'Sign in' }).click();

  // test oidc login page is shown
  await expect(page.getByRole('link', { name: 'UMCG' })).toBeVisible();

  await page.goto("/");

  await page.goto('/apps/central/#/admin/settings');
  await page.getByPlaceholder('Enter username').click();
  await page.getByPlaceholder('Enter username').fill('admin');
  await page.getByPlaceholder('Enter username').press('Tab');
  await page.getByPlaceholder('Password').fill('admin');
  await page.getByRole('dialog').getByRole('button', { name: 'Sign in' }).click();
  await page.goto('/apps/central');
  await page.goto('/apps/central/#/admin/settings');
  await page.getByRole('row', { name: '  isOidcEnabled true' }).getByRole('button').first().click();
  await page.locator('textarea').dblclick();
  await page.locator('textarea').dblclick();
  await page.locator('textarea').fill('false');
  await page.getByRole('button', { name: 'Edit Setting' }).click();

  
});
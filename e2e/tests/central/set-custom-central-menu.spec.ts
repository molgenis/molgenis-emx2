import { test, expect } from '@playwright/test';

test('test set custom database level menu', async ({ page }) => {
  await page.goto('/apps/central/');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.getByPlaceholder('Enter username').click();
  await page.getByPlaceholder('Enter username').fill('admin');
  await page.getByPlaceholder('Enter username').press('Tab');
  await page.getByPlaceholder('Password').fill('admin');
  await page.getByRole('dialog').getByRole('button', { name: 'Sign in' }).click();
  await page.getByRole('link', { name: 'Admin' }).click();
  await page.getByRole('link', { name: 'Settings' }).click();
  await page.getByRole('button', { name: '' }).click();
  await page.locator('input[type="text"]').click();
  await page.locator('input[type="text"]').fill('menu');
  await page.locator('textarea').click();
  await page.locator('textarea').press('ControlOrMeta+Tab');
  await page.locator('textarea').fill('[{"label":"Blabla","href":"https://google.com"}]');
  await page.getByRole('button', { name: 'Create Setting' }).click();
  await page.getByRole('button', { name: 'Sign out' }).click();
  await page.getByLabel('Close').click();
  await page.getByRole('link', { name: 'brand-logo' }).click();


  // test menu item is shown
  await expect(page.getByRole('listitem')).toContainText('Blabla');

  // cleanup
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.getByPlaceholder('Enter username').click();
  await page.getByPlaceholder('Enter username').fill('admin');
  await page.getByPlaceholder('Enter username').press('Tab');
  await page.getByPlaceholder('Password').fill('admin');
  await page.getByRole('dialog').getByRole('button', { name: 'Sign in' }).click();
  await page.getByRole('link', { name: 'Admin' }).click();
  await page.getByRole('link', { name: 'Settings' }).click();
  await page.getByRole('row', { name: '  menu [{"label":"Blabla","' }).getByRole('button').nth(1).click();
  await page.getByRole('button', { name: 'Delete Setting' }).click();
  await page.getByRole('button', { name: 'Sign out' }).click();
});
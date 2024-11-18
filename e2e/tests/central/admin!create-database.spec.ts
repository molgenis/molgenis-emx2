import { test, expect } from '@playwright/test';

const regexErrorMessage='name (required) Table name must start with a letter, followed by zero or more letters, numbers, spaces, dashes or underscores. A space immediately before or after an underscore is not allowed. The character limit is 31.'

test('database name regex validation', async ({ page }) => {
  await page.goto('/apps/central/');
  await page.getByRole('button', { name: '' }).click();
  await page.getByLabel('name').fill('a');
  await expect(page.locator('form')).not.toContainText(regexErrorMessage);
  await page.getByLabel('name').fill('first name');
  await expect(page.locator('form')).not.toContainText(regexErrorMessage);
  await page.getByLabel('name').fill('yet_another name');
  await expect(page.locator('form')).not.toContainText(regexErrorMessage);
  await page.getByLabel('name').fill('#first name');
  await expect(page.locator('form')).toContainText(regexErrorMessage);
  await page.getByLabel('name').fill('first_  name');
  await expect(page.locator('form')).toContainText(regexErrorMessage);
  await page.getByLabel('name').fill('first   _name');
  await expect(page.locator('form')).toContainText(regexErrorMessage);
  await page.getByLabel('name').fill('first  _  name');
  await expect(page.locator('form')).toContainText(regexErrorMessage);
  await page.getByLabel('name').fill('first  __   name');
  await expect(page.locator('form')).toContainText(regexErrorMessage);
  await page.getByLabel('name').fill('aa    ____      ');
  await expect(page.locator('form')).toContainText(regexErrorMessage);
  await page.getByLabel('name').fill('a234567890123456789012345678901');
  await expect(page.locator('form')).not.toContainText(regexErrorMessage);
  await page.getByLabel('name').fill('a2345678901234567890123456789012');
  await expect(page.locator('form')).toContainText(regexErrorMessage);
});

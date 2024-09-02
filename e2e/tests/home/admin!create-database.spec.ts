import { test, expect } from '@playwright/test';

let regexErrorMessage='Table name must start with a letter, followed by letters/underscores/spaces/numbers (though no underscore preceded/followed by a space) and with a maximum of 31 characters, i.e. ^(?!.* _|.*_ )[a-zA-Z][a-zA-Z0-9 _]{0,30}$'

test('database name regex validation', async ({ page }) => {
  await page.goto('http://localhost:8080/apps/central/#/');
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
  await page.getByLabel('name').fill('abcdefghijklmnopqrstuvwzyx78901');
  await expect(page.locator('form')).not.toContainText(regexErrorMessage);
  await page.getByLabel('name').fill('abcdefghijklmnopqrstuvwzyx789012');
  await expect(page.locator('form')).toContainText(regexErrorMessage);
});

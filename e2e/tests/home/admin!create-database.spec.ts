import { test, expect } from '@playwright/test';

let regexErrorMessage='Table name must start with a letter, followed by letters/underscores/spaces/numbers (though no underscore preceded/followed by a space), i.e. ^(?!.* _|.*_ )[a-zA-Z][a-zA-Z0-9 _]*$. Maximum length: 31 characters'

test('database name regex validation', async ({ page }) => {
  await page.goto('http://localhost:8080/apps/central/#/');
  await page.getByRole('button', { name: '' }).click();
  await page.getByLabel('name').fill('%');
  await expect(page.locator('form')).toContainText(regexErrorMessage);
  await page.getByLabel('name').fill('a');
  await expect(page.locator('form')).not.toContainText(regexErrorMessage);
  await page.getByLabel('name').fill('a_ b');
  await expect(page.locator('form')).toContainText(regexErrorMessage);
  await page.getByLabel('name').fill('a _b');
  await expect(page.locator('form')).toContainText(regexErrorMessage);
  await page.getByLabel('name').fill('a_b c');
  await expect(page.locator('form')).not.toContainText(regexErrorMessage);
});

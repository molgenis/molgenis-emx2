import { test, expect } from '@playwright/test';

test('catalogue description should be shown', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/all');
  await page.getByRole('button', { name: 'Accept' }).click();
  await expect(page.getByRole('main')).toContainText('Select one of the content categories listed below.');
});
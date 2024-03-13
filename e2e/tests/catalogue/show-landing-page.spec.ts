import { test, expect } from '@playwright/test';

test('test', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/');
  await expect(page.getByRole('heading', { name: 'European Health Research Data' })).toBeVisible();
});
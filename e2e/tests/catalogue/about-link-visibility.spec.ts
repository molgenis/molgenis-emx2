import { test, expect } from '@playwright/test';

test('should not show about menu item on non catalogue spesific page', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/all');
  await page.getByRole('button', { name: 'Accept' }).click();
  await expect(page.getByRole('link', { name: 'About' })).toHaveCount(0);
});

test('should show about menu item on catalogue spesific page', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/IPEC');
  await page.getByRole('button', { name: 'Accept' }).click();
  await expect(page.getByRole('link', { name: 'About' })).toHaveCount(1);
});
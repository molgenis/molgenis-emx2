import { test, expect } from '@playwright/test';

test('should show the alt logo', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/?logo=UMCGkort.woordbeeld');
  await expect(page).toHaveScreenshot();
});
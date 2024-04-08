import { test, expect } from '@playwright/test';

test('should show the alt logo', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/?logo=UMCGkort.woordbeeld');
  await page.getByRole('button', { name: 'Accept' }).click();
  await expect(page).toHaveScreenshot({clip: {x: 0, y: 0, width: 200, height: 100}, threshold: 0.4});
});
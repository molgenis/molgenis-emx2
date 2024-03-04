import { test, expect } from '@playwright/test';

test('navigate-to-next-page-on-cohorts-list-page', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/ATHLETE');
  await page.getByRole('button', { name: 'Accept' }).click();
  await page.getByRole('button', { name: 'Cohorts' }).click();
  await page.locator('nav').filter({ hasText: 'Page OF' }).getByRole('button').nth(1).click();
  await expect(page.getByRole('list')).toContainText('SEPAGES');
});
import { test, expect } from '@playwright/test';

test('filter should remain active after page (pagination) change ', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/all/cohorts');
  await page.getByRole('button', { name: 'Accept' }).click();
  await expect(page.getByRole('main')).toContainText('57 cohorts');
  await page.getByPlaceholder('Type to search..').click();
  await page.getByPlaceholder('Type to search..').fill('life');
  await expect(page.getByRole('main')).toContainText('19 cohorts');
  await page.locator('nav').filter({ hasText: 'Page OF' }).getByRole('button').nth(1).click();
  await expect(page.getByRole('main')).toContainText('19 cohorts');
});
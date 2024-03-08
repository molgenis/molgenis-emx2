import { test, expect } from '@playwright/test';

test('test', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/all/cohorts');
  await page.getByRole('button', { name: 'Reject' }).click();
  await expect(page.getByRole('main')).toContainText('53 cohorts');
});
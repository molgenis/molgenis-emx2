import { test, expect } from '@playwright/test';

test('filter cohorts list page by design', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/all/cohorts');
  await page.getByRole('button', { name: 'Accept' }).click();
  await page.locator('div:nth-child(16) > .inline-flex > .rotate-180 > svg').click();
  await page.getByRole('complementary').getByText('Cross-sectional').click();
  await expect(page.getByRole('main')).toContainText('Cross-sectional');
  await page.getByRole('button', { name: 'Remove all' }).click();
  await expect(page.getByRole('main')).toContainText('Longitudinal');
});
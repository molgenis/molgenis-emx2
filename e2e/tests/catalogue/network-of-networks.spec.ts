import { test, expect } from '@playwright/test';

test('show network of networks', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/testNetworkofNetworks');
  await page.getByRole('button', { name: 'Accept' }).click();
  await expect(page.getByText('9')).toBeVisible();
  await expect(page.getByText('4')).toBeVisible();
  await page.getByRole('button', { name: 'Cohorts' }).click();
  await expect(page.getByText('4 cohorts')).toBeVisible();
  await page.goto('/catalogue-demo/ssr-catalogue/testNetworkofNetworks');
  await page.getByRole('button', { name: 'Variables' }).click();
  await expect(page.getByText('9 variables')).toBeVisible();
  await page.getByRole('heading', { name: 'Cohorts' }).click();
  await expect(page.getByText('testCohort4')).toBeVisible();
  await page.getByRole('button', { name: 'Harmonisations' }).click();
  await expect(page.getByRole('cell', { name: 'testCohort4' }).locator('span')).toBeVisible();
});
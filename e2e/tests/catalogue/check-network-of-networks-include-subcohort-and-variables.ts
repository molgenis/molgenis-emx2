import { test, expect } from '@playwright/test';

test('check cohorts and variables of nested networks are shown on main network pages', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/testNetworkofNetworks');
  await page.getByText('9', { exact: true }).click();
  await page.getByText('4', { exact: true }).click();
  await page.getByRole('button', { name: 'Cohorts' }).click();
  await page.getByText('4 cohorts').click();
  await page.goto('/catalogue-demo/ssr-catalogue/testNetworkofNetworks');
  await page.getByRole('button', { name: 'Variables' }).click();
  await page.getByText('9 variables').click();
  await page.getByRole('button', { name: 'Cohorts' }).click();
  await page.getByText('4 cohorts').click();
  await page.getByRole('button', { name: 'Harmonisations' }).click();
  await page.getByText('testCohort4').click();
});
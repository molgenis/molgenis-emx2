import { test, expect } from '@playwright/test';

test('show network of networks', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/testNetworkofNetworks');
  await page.getByRole('button', { name: 'Accept' }).click();
  await expect(page.getByText('7', {exact: true})).toBeVisible();
  await expect(page.getByText('4', {exact: true})).toBeVisible();
  await page.getByRole('button', { name: 'Cohort studies' }).click();
  await expect(page.getByText('4 cohort studies')).toBeVisible();
  await page.goto('/catalogue-demo/ssr-catalogue/testNetworkofNetworks');
  await page.getByRole('button', { name: 'Variables' }).click();
  await expect(page.getByText('9 variables')).toBeVisible();
  await page.getByRole('heading', { name: 'Cohort studies' }).click();
  await expect(page.getByText('testCohort4')).toBeVisible();
  await page.getByRole('button', { name: 'Harmonisations' }).click();
  await expect(page.getByRole('cell', { name: 'testCohort4' }).locator('span')).toBeVisible();
});
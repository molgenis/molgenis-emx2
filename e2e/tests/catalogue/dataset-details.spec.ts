import { test, expect } from '@playwright/test';

test('show dataset details on cohorts page', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/');
  await page.getByRole('button', { name: 'Reject' }).click();
  await page.getByRole('link', { name: 'All resources' }).click();
  await page.getByRole('button', { name: 'Cohorts' }).click();
  await page.getByPlaceholder('Type to search..').click();
  await page.getByPlaceholder('Type to search..').fill('genr');
  await page.getByRole('link', { name: 'GenR', exact: true }).click();
  await page.getByRole('link', { name: 'Networks' }).click();
  await page.getByRole('link', { name: 'Datasets' }).click();
  await expect(page.locator('tbody')).toContainText('FETALCRL_22112016');
  await page.getByText('FETALCRL_22112016').click();
  await expect(page.getByText('DataWiki dataset FETALCRL_22112016')).toBeVisible();
});
import { test, expect } from '@playwright/test';

test('network detail resource listing resource detail should show the resoource details page', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/testNetwork1/networks/testNetwork1#cohorts');
  await page.getByRole('button', { name: 'Accept' }).click();
  await page.getByText('Name for test cohort 1').click();
  await page.getByRole('button', { name: 'Detail page' }).click();
  await expect(page.locator('h1')).toContainText('acronym for test cohort 1');
});


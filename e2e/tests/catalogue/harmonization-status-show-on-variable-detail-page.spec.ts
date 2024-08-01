import { test, expect } from '@playwright/test';

test('test hamonisation status is show in varaible on variable detail page', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/')
  await page.getByRole('button', { name: 'Accept' }).click();
  await page.getByText('ATHLETE').click();
  await page.getByRole('button', { name: 'Variables' }).click();
  await page.getByPlaceholder('Type to search..').click();
  await page.getByPlaceholder('Type to search..').fill('fetus_abd_circum_t2');
  await page.getByRole('link', { name: 'fetus_abd_circum_t2', exact: true }).click();
  await expect(page.getByRole('row', { name: 'fetus_abd_circum_t2 partial' }).getByRole('img')).toBeVisible();
});
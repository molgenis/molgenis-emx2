import { test, expect } from '@playwright/test';

test('Catalogue test number 1: Athlete network manager', async ({ page }) => {
  await page.goto('catalogue-demo/ssr-catalogue/');
  await page.getByRole('button', { name: 'Accept' }).click();
  await expect(page.locator('h1')).toContainText('European Health Research Data and Sample Catalogue');
  await expect(page.getByRole('main')).toContainText('Project catalogues');
  await expect(page.getByRole('main')).toContainText('ATHLETE');
  await expect(page.getByRole('main')).toContainText('Advancing Tools for Human Early Lifecourse Exposome Research and Translation');
  await page.getByText('ATHLETE').click();
  await expect(page.getByRole('heading', { name: 'ATHLETE' })).toBeVisible();
  await expect(page.getByRole('navigation').getByRole('link', { name: 'Cohort studies' })).toBeVisible();
  await expect(page.getByRole('navigation').getByRole('link', { name: 'Variables' })).toBeVisible();

});
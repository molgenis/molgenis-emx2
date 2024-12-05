import { test, expect } from '@playwright/test';
import { getAppRoute } from './getAppRoute';

test("should show a biobank card with a service tab", async ({ page }) => {
  await page.goto(getAppRoute());
  await expect(page.getByRole('main')).toContainText('Biobank1');
  await expect(page.getByRole('button', { name: 'Services' }).first()).toBeVisible();
});

test("selecting the service tab should show the servie details", async ({ page }) => {
    await page.goto(getAppRoute());
    await page.getByRole('button', { name: 'Services' }).first().click();
    await expect(page.getByRole('main')).toContainText('Biobank Service');
    await expect(page.getByText('Sample Storage, Microbiome')).toBeVisible();
    await expect(page.locator('small').filter({ hasText: 'Type:Sample Storage,' }).getByRole('link')).toBeVisible();
  });
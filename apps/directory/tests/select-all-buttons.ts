import { test, expect } from '@playwright/test';
import { getAppRoute } from "./getAppRoute";

test('select all collections and services', async ({ page }) => {
  await page.goto(getAppRoute());
  await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: 'Select all collections' }).click();
  await expect(page.getByRole('main')).toContainText('2');
  await page.getByRole('button', { name: 'Select all services' }).click();
  await expect(page.getByRole('main')).toContainText('4');
});
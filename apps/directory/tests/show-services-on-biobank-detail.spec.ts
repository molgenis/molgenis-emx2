import { test, expect } from '@playwright/test';
import { getAppRoute } from './getAppRoute';

test('show services on biobank detail view page', async ({ page }) => {
  await page.goto(getAppRoute());
  await page.getByRole('link', { name: 'Biobank1', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Services' })).toBeVisible();
  await page.getByRole('button', { name: 'Services' }).click();
  // should show both services
  await expect(page.getByRole('heading', { name: 'Biobank Service' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Test service' })).toBeVisible();
  // should show add button
  await expect(page.locator('div').filter({ hasText: /^Test service 3Add$/ }).getByRole('button')).toBeVisible();
  await expect(page.locator('div').filter({ hasText: /^Biobank ServiceAdd$/ }).getByRole('button')).toBeVisible();
  // should add to request
  await page.locator('div').filter({ hasText: /^Biobank ServiceAdd$/ }).getByRole('button').click();
  await expect(page.getByRole('button', { name: 'Request' })).toBeVisible();
  await expect(page.getByRole('main')).toContainText('1');
});
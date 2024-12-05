import { test, expect } from '@playwright/test';
import { getSchemaName } from './getSchemaName';

const schemaName = getSchemaName();

test('add/remove service from service detaill page', async ({ page }) => {
  await page.goto(`/${schemaName}/directory/`);
  await page.goto('http://localhost:5173/#/catalogue');
  await page.getByRole('button', { name: 'Services' }).first().click();
  await page.getByRole('link', { name: 'Biobank Service' }).click();
  await expect(page.locator('h1')).toContainText('Biobank Service');
  await expect(page.getByRole('main')).toContainText('Add');
  await page.getByRole('button', { name: 'Add' }).click();
  // 1 is the number of services added
  await expect(page.getByRole('main')).toContainText('1');
  await expect(page.getByRole('main')).toContainText('Remove');
  await page.getByRole('button', { name: 'Remove' }).click();
  // after removing the service, the text should be 'Add'
  await expect(page.getByRole('main')).toContainText('Add');
});
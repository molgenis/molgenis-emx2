import { test, expect } from "@playwright/test";
import playwrightConfig from '../../../playwright.config';

const route = playwrightConfig?.use?.baseURL?.startsWith('http://localhost') ? '' : '/apps/tailwind-components/#/';

test("it should render the form", async ({ page }) => {
  await page.goto(`${route}Form.story`);
  await expect(page.getByText('nameRequiredthe name')).toBeVisible();
  await expect(page.locator('div').filter({ hasText: /^statusRequired$/ }).nth(1)).toBeVisible();
});

test("it should update the model value when a field is filled out", async ({ page }) => {
  await page.goto(`${route}Form.story`);
  await page.getByLabel('name').click();
  await page.getByLabel('name').fill('test');
  await expect(page.getByRole('main')).toContainText('dataMap: { "name": "test", "category": "", "photoUrls": "", "details": "", "status": "", "tags": "", "weight": "", "orders": "", "mg_draft": "", "mg_insertedBy": "", "mg_insertedOn": "", "mg_updatedBy": "", "mg_updatedOn": "" } errorMap: { "name": [], "category": [], "photoUrls": [], "details": [], "status": [], "tags": [], "weight": [], "orders": [], "mg_draft": [], "mg_insertedBy": [], "mg_insertedOn": [], "mg_updatedBy": [], "mg_updatedOn": [] }');
  await page.locator('label').filter({ hasText: 'dog' }).getByRole('img').click();
  await expect(page.getByRole('button', { name: 'dog' })).toBeVisible();
});

test("should also work for refs", async ({ page }) => {
  await page.goto(`${route}Form.story`);
  await page.getByRole('combobox').selectOption('pet store user');
  await page.locator('label').filter({ hasText: 'pooky' }).locator('rect').click();
  await expect(page.getByRole('button', { name: 'pooky' })).toBeVisible();
});
import { test, expect } from "@playwright/test";
import playwrightConfig from '../../../playwright.config';

const route = playwrightConfig?.use?.baseURL?.startsWith('http://localhost') ? '' : '/apps/tailwind-components/#/';

test("it should render the form", async ({ page }) => {
  await page.goto(`${route}Form.story`);
  await expect(page.getByText('nameRequiredthe name')).toBeVisible();
  await expect(page.getByText('WeightRequired')).toBeVisible();
});

test("it should update the model value when a field is filled out", async ({ page }) => {
  await page.goto(`${route}Form.story`);
  await page.getByLabel('name').click();
  await page.getByLabel('name').fill('test');
  await expect(page.getByRole('main')).toContainText('dataMap:  { "bool": "", "boolarray": "", "date": "", "name": "test", "category": "", "photoUrls": "", "details": "", "tags": "", "weight": "", "heading2": "", "orders": "", "autoid": "", "mg_draft": "", "mg_insertedBy": "", "mg_insertedOn": "", "mg_updatedBy": "", "mg_updatedOn": "" }');
});
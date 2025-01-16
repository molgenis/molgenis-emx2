import { test, expect } from '@playwright/test';

test('test', async ({ page }) => {
  await page.goto('http://localhost:3000/Form.story');
  await page.getByLabel('name').click();
  await page.getByLabel('name').fill('test');
  await expect(page.getByRole('main')).toContainText('{ "bool": "", "boolarray": "", "date": "", "name": "test", "category": "", "photoUrls": "", "tags": "", "weight": "", "orders": "", "autoid": "", "mg_draft": "", "mg_insertedBy": "", "mg_insertedOn": "", "mg_updatedBy": "", "mg_updatedOn": "" }');
});
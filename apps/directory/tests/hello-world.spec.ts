import { test, expect } from '@playwright/test';

const baseURL = process.env.E2E_BASE_URL;
let schemaName = 'directory-demo';
if (!baseURL) {
  schemaName = 'directory';
} 

test('should run playwright', async ({ page }) => {
  await page.goto('https://molgenis.org/');
  expect(await page.title()).toBe('For scientific data');
});

test("directory page should load", async ({ page }) => {
  await page.goto(`/${schemaName}/directory/`);
  await expect(page.getByRole('paragraph')).toContainText('© 2024 BBMRI-ERIC');
});
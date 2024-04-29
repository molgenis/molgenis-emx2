import { test, expect } from '@playwright/test';

test.beforeEach(async ({ context, baseURL }) => {
  await context.addCookies([{ name: 'mg_allow_analytics', value: 'false', domain: new URL(baseURL as string).hostname, path: '/'}])
});

test('filter varaibles by cohort', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/testNetwork1/variables');
  await page.getByRole('complementary').getByRole('img').nth(2).click();
  await page.getByLabel('testCohort1').check();
  await expect(page.getByRole('main')).toContainText('1 variablein 1 cohort');
  await page.getByRole('button', { name: 'Harmonizations' }).click();
  await expect(page.getByRole('cell', { name: 'testCohort1' }).locator('span')).toBeVisible();
});


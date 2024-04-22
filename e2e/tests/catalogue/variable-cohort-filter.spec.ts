import { test, expect } from '@playwright/test';

test('filter varaibles by cohort', async ({ page }) => {
  await page.goto('https://preview-emx2-pr-3656.dev.molgenis.org/catalogue-demo/ssr-catalogue/testNetwork1/variables');
  await page.getByRole('button', { name: 'Accept' }).click();
  await page.getByRole('complementary').getByRole('img').nth(2).click();
  await expect(page.getByRole('complementary')).toContainText('testCohort1');
  await page.getByLabel('testCohort1').check();
  await page.getByText('variablein 4 cohorts').click();
});


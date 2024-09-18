import { test, expect } from '@playwright/test';

test.beforeEach(async ({ context, baseURL }) => {
  await context.addCookies([
    {
      name: "mg_allow_analytics",
      value: "false",
      domain: new URL(baseURL as string).hostname,
      path: "/"
    }
  ]);
});

test('filter cohorts list page by design', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/all/cohorts?page=1&conditions=[{%22id%22:%22cohortDesigns%22,%22conditions%22:[{%22name%22:%22Cross-sectional%22}]}]');
  await expect(page.getByRole('main')).toContainText('Cross-sectional');
  await page.getByRole('button', { name: 'Design -' }).click();
  await expect(page.getByRole('complementary')).toContainText('Longitudinal');
});



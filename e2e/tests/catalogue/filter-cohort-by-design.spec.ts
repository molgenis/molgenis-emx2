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
  await page.goto('/catalogue-demo/ssr-catalogue/all/cohorts');
  await page.getByRole('heading', { name: 'Design' }).click();
  await page.getByText('Cross-sectional').click();
  await expect(page.getByRole('main')).toContainText('Cross-sectional');
  await page.getByRole('button', { name: 'Remove all' }).click();
  await expect(page.getByRole('complementary')).toContainText('Longitudinal');
});

import { test, expect } from '@playwright/test';

test.beforeEach(async ({ context, baseURL }) => {
  await context.addCookies([{ name: 'mg_allow_analytics', value: 'false', domain: new URL(baseURL as string).hostname, path: '/'}])
});

test('go back from details, filter should stil be active', async ({ page }) => {
  
  await page.goto('/catalogue-demo/ssr-catalogue/testNetworkofNetworks/cohorts');
  await page.locator('div:nth-child(16) > .inline-flex > .rotate-180 > svg').click();
  await page.getByRole('complementary').getByText('Longitudinal').click();
  await page.getByRole('link', { name: 'acronym for test cohort 1' }).click();
  await page.goBack()
  await expect(page.getByRole('main')).toContainText('2 cohorts');
  await expect(page.getByLabel('Longitudinal')).toBeChecked();
});
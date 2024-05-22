import { test, expect } from '@playwright/test';

test.beforeEach(async ({ context, baseURL }) => {
  await context.addCookies([{ name: 'mg_allow_analytics', value: 'false', domain: new URL(baseURL as string).hostname, path: '/'}])
});


test('offset should be reset ( back to page 1) when filter changes ', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/EUChildNetwork/variables');
  await page.locator('nav').filter({ hasText: 'Page OF' }).getByRole('button').nth(1).click();
  await page.getByPlaceholder('Type to search..').click();
  await page.getByPlaceholder('Type to search..').fill('food_all_sens');
  await expect(page.getByRole('main')).toContainText('16 variables');
  await expect(page.getByRole('list')).toContainText('food_all_sens_IgE_0');
});
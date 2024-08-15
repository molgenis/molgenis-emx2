import { test, expect } from '@playwright/test';

test('test', async ({ page }) => {
  await page.goto('https://preview-emx2-pr-4114.dev.molgenis.org/catalogue-demo/ssr-catalogue/EUChildNetwork/variables?page=2');
  await page.getByRole('button', { name: 'Accept' }).click();

});
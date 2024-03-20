/**
 * Harmonization details test
 * Determine if the first status in the table has a valid status
 */

import { test, expect } from '@playwright/test';

const enableRejectCookiesClick = false;

const statusPatterns = new RegExp(/^(complete|partial|unmapped)$/);

test('harmonization cell has valid status @variables-view @harmonization @harmonization-matrix', async ({ page }) => {
  await page.goto('/catalogue-demo/ssr-catalogue/ATHLETE/variables/ath_ndvi100_mean_0-ATHLETE_CDM-urban_ath-ATHLETE_CDM?keys={"name":"ath_ndvi100_mean_0","resource":{"id":"ATHLETE_CDM"},"dataset":{"name":"urban_ath","resource":{"id":"ATHLETE_CDM"}}}');
  
  if (enableRejectCookiesClick) {
    await page.getByRole('button', { name: 'Reject' }).click();
  }
  
  const firstCellStatus = await page.locator('tr:nth-child(2) > td > .z-10 > .absolute > span.sr-only')
    .first()
    .innerText();

  expect(firstCellStatus).toMatch(statusPatterns)
});
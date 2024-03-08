/**
 * Test all instances where the component <SearchResultsCounts> is used.
 * All values must follow this format: <value-prefix> <value> <label>. 
 * Value prefixes are optional and used if you would like to add a label
 * before the value (e.g., `Found 12 results`). If testing locally, disable
 * the `enableRejectCookiesClick` as this does not exist when running the
 * preview locally and the action will timeout.
 */

import { test, expect } from '@playwright/test';

const enableRejectCookiesClick = true;
const numberOfResultsPattern = new RegExp(/^(([a-zA-Z]{1,})?(\s)?(([0-9]{1,})\s(cohort([s])?|variable([s])?|data\ssource([s])?|result([s])?|networks([s])?)))$/);

test('validate cohort search result counts @cohort-view @search-result-counts',
  async ({ page }) => {
    await page.goto('/catalogue-demo/ssr-catalogue/all/cohorts');
    
    if (enableRejectCookiesClick) {
      await page.getByRole('button', { name: 'Reject' }).click();
    }
    
    const text = await page.locator(".search-results-count").textContent();
    await expect(text).toMatch(numberOfResultsPattern);
  }
);

test('validate data sources search result counts @data-sources-view @search-result-counts',
  async ({ page }) => {
    await page.goto('/catalogue-demo/ssr-catalogue/all/datasources');
    
    if (enableRejectCookiesClick) {
      await page.getByRole('button', { name: 'Reject' }).click();
    }
    
    const text = await page.locator(".search-results-count").textContent();
    await expect(text).toMatch(numberOfResultsPattern);
  }
);

test('validate variables sources search result counts @variables-view @search-result-counts',
  async ({ page }) => {
    await page.goto('/catalogue-demo/ssr-catalogue/all/variables');
    
    if (enableRejectCookiesClick) {
      await page.getByRole('button', { name: 'Reject' }).click();
    }
    
    const text = await page.locator(".search-results-count").textContent();
    await expect(text).toMatch(numberOfResultsPattern);
  }
);

test('validate networks sources search result counts @networks-view @search-result-counts',
  async ({ page }) => {
    await page.goto('/catalogue-demo/ssr-catalogue/all/networks');
    
    if (enableRejectCookiesClick) {
      await page.getByRole('button', { name: 'Reject' }).click();
    }
    
    const text = await page.locator(".search-results-count").textContent();
    await expect(text).toMatch(numberOfResultsPattern);
  }
);


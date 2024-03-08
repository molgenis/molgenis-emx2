import { test, expect } from '@playwright/test';

const numberOfResultsPattern = new RegExp(/^(([a-zA-Z]{1,})?(\s)?(([0-9]{1,})\s(cohort([s])?|variable([s])?|data\ssource([s])?|result([s])?)))$/);

test('validate cohort search result counts @cohort-view @search-result-counts',
  async ({ page }) => {
    await page.goto('/catalogue-demo/ssr-catalogue/all/cohorts');
    await page.getByRole('button', { name: 'Reject' }).click(); 
    const text = await page.locator(".search-results-count").textContent();
    await expect(text).toMatch(numberOfResultsPattern);
  }
);

test('validate data sources search result counts @data-sources-view @search-result-counts',
  async ({ page }) => {
    await page.goto('/catalogue-demo/ssr-catalogue/all/datasources');
    await page.getByRole('button', { name: 'Reject' }).click();
    const text = await page.locator(".search-results-count").textContent();
    await expect(text).toMatch(numberOfResultsPattern);
  }
);

test('validate variables sources search result counts @variables-view @search-result-counts',
  async ({ page }) => {
    await page.goto('/catalogue-demo/ssr-catalogue/all/variables');
    await page.getByRole('button', { name: 'Reject' }).click();
    const text = await page.locator(".search-results-count").textContent();
    await expect(text).toMatch(numberOfResultsPattern);
  }
);

test('validate networks sources search result counts @networks-view @search-result-counts',
  async ({ page }) => {
    await page.goto('/catalogue-demo/ssr-catalogue/all/networks');
    await page.getByRole('button', { name: 'Reject' }).click();
    const text = await page.locator(".search-results-count").textContent();
    await expect(text).toMatch(numberOfResultsPattern);
  }
);


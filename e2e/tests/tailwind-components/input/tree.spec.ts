import { test, expect } from '@playwright/test';
import playwrightConfig from '../../../playwright.config';

const route = playwrightConfig?.use?.baseURL?.startsWith('http://localhost:') ? '' : 'apps/tailwind-components/';

test('should render the collapsed tree', async ({ page }, testInfo) => {
  await page.goto(route + 'input/Tree.story/');
  await expect(page.getByText('Node 0', { exact: true })).toBeVisible();
  await expect(page.getByText('Node 1', { exact: true })).toBeVisible();
  await expect(page.getByText('Node 0.0', { exact: true })).not.toBeVisible();
});

test('should expand the fist node when clicking in the icon', async ({ page }) => {
  await page.goto(route + 'input/Tree.story/');
  await page.getByRole('img').nth(1).click();
  await expect(page.getByText('Node 0.0', { exact: true })).toBeVisible();
  await expect(page.getByText('Node 0.1', { exact: true })).toBeVisible();
  await expect(page.getByText('Node 1.0', { exact: true })).not.toBeVisible();
});

test('should expand the selection down if expand selection is set to true', async ({ page }) => {
  await page.goto(route + 'input/Tree.story/');
  await expect(page.getByLabel('expand selected')).toBeChecked();
  await page.locator('.text-search-filter-group-toggle > svg').first().click();
  await page.locator('.mt-2\\.5 > ul > li > span > .text-search-filter-group-toggle > svg').first().click();
  await page.getByLabel('Node 0.0', { exact: true }).check();
  await expect(page.locator('section')).toContainText('Node 0.0');
  await expect(page.locator('section')).toContainText('Node 0.0.0');
  await expect(page.locator('section')).toContainText('Node 0.0.0.0');
  await expect(page.locator('section')).toContainText('Node 0.0.0.1');
  await expect(page.locator('section')).toContainText('Node 0.0.1');
  await expect(page.locator('section')).toContainText('Node 0.0.1.0');
  await expect(page.locator('section')).toContainText('Node 0.0.1.1');
});

test('should expand the the de-selection down if expand selection is set to true', async ({ page }) => {
  await page.goto(route + 'input/Tree.story/');
  await expect(page.getByLabel('expand selected')).toBeChecked();

  // select
  await page.getByLabel('Node 0', { exact: true }).check();
  await expect(page.locator('section')).toContainText('Number off selected nodes: 15');
  await page.getByLabel('Node 0', { exact: true }).check();

  //deselect
  await page.getByLabel('Node 0', { exact: true }).uncheck();
  await expect(page.locator('section')).toContainText('Number off selected nodes: 0');
});
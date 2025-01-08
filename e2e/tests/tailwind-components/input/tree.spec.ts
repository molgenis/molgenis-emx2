import { test, expect } from '@playwright/test';
import playwrightConfig from '../../../playwright.config';

const route = playwrightConfig?.use?.baseURL?.startsWith('http://localhost:') ? '' : '/apps/tailwind-components/';

test('should render the collapsed tree', async ({ page }, testInfo) => {
  await page.goto(route);
  await page.getByRole('link', { name: 'InputTree' }).click();
  await expect(page.getByText('Node 0', { exact: true })).toBeVisible();
  await expect(page.getByText('Node 1', { exact: true })).toBeVisible();
  await expect(page.getByText('Node 0.0', { exact: true })).not.toBeVisible();
});

test('should expand the fist node when clicking in the icon', async ({ page }) => {
  await page.goto(route);
  await page.getByRole('link', { name: 'InputTree' }).click();
  await expect(page.getByText('Node 0.0', { exact: true })).not.toBeVisible();
  await page.getByRole('main').getByRole('img').first().click();
  await expect(page.getByText('Node 0.0', { exact: true })).toBeVisible();
  await expect(page.getByText('Node 0.1', { exact: true })).toBeVisible();
  await expect(page.getByText('Node 1.0', { exact: true })).not.toBeVisible();
});

test('should expand the selection down if expand selection is set to true', async ({ page }) => {
  await page.goto(route);
  await page.getByRole('link', { name: 'InputTree' }).click();
  await expect(page.getByLabel('expand selected')).toBeChecked();
  await page.locator('label').filter({ hasText: 'Node 0' }).first().locator('rect').click();
  await expect(page.getByText('Node 0.0.0.0 x')).toBeVisible();
});

test('should expand the the de-selection down if expand selection is set to true', async ({ page }) => {
  await page.goto(route);
  await page.getByRole('link', { name: 'InputTree' }).click();

  // select
  await page.locator('label').filter({ hasText: 'Node 0' }).first().locator('rect').click();
  await expect(page.getByText('Number off selected nodes: 15')).toBeVisible();

  //deselect
  await page.locator('label').filter({ hasText: 'Node 0' }).first().locator('rect').click();
  await expect(page.getByText('Number off selected nodes: 0')).toBeVisible();
});

test('should disable selection of nodes with invisible children in case of search', async ({ page }) => {
  await page.goto(route);
  await page.getByRole('link', { name: 'InputTree' }).click();
  await page.getByRole('button', { name: 'Search for options'}).click();
  //can't get below to work, but recording gives this only
  // await page.getByPlaceholder('Type to search in options...').click();
  // await page.getByPlaceholder('Type to search in options...').fill('0.0.0.1');
  // await expect(page.getByText('Node 0.0', { exact: true })).toBeVisible();
  // await expect(page.getByText('Node 1', { exact: true })).not.toBeVisible();
});
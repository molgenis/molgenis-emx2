import { test, expect } from '@playwright/test';

test('InputSelect: Attribute modelvalue should never be present when empty value is selected', async ({ page }) => {
  await page.goto('/apps/molgenis-components/#/component/InputSelect');
  expect(await page.getByLabel('Animals', { exact: true }).getAttribute('modelvalue')).toBeNull();
  await page.getByLabel('Animals', { exact: true }).selectOption('lion');
  await expect(page.getByLabel('Animals', { exact: true })).toHaveAttribute('modelvalue', 'lion');
  await page.getByLabel('Animals', { exact: true }).selectOption('');
  expect(await page.getByLabel('Animals', { exact: true }).getAttribute('modelvalue')).toBeNull();
});

test('inputString: Attribute modelvalue should never be present when empty value is selected', async ({ page }) => {
  await page.goto('/apps/molgenis-components/#/component/InputString');
  await page.getByLabel('My string input label', { exact: true }).fill('test');
  await expect(page.getByLabel('My string input label', { exact: true })).toHaveAttribute('value', 'test');
  await page.getByLabel('My string input label', { exact: true }).fill('');
  expect(await page.getByLabel('My string input label', { exact: true }).getAttribute('value')).toBeNull();
});
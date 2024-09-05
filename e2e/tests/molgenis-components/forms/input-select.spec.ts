import { test, expect } from '@playwright/test';

test('InputSelect: Attribute modelvalue should never be present when empty value is selected', async ({ page }) => {
  await page.goto('/apps/molgenis-components/#/component/InputSelect');
  expect(await page.getByLabel('Animals', { exact: true }).getAttribute('modelvalue')).toBeNull();
  await page.getByLabel('Animals', { exact: true }).selectOption('lion');
  await expect(page.getByLabel('Animals', { exact: true })).toHaveAttribute('modelvalue', 'lion');
  await page.getByLabel('Animals', { exact: true }).selectOption('');
  expect(await page.getByLabel('Animals', { exact: true }).getAttribute('modelvalue')).toBeNull();
});

test('InputString: Attribute value should never be present if field is empty', async ({ page }) => {
  await page.goto('/apps/molgenis-components/#/component/InputString');
  await page.getByLabel('My string input label', { exact: true }).fill('test');
  await expect(page.getByLabel('My string input label', { exact: true })).toHaveAttribute('value', 'test');
  await page.getByLabel('My string input label', { exact: true }).fill('');
  expect(await page.getByLabel('My string input label', { exact: true }).getAttribute('value')).toBeNull();
});

test('InputText: Attribute value should never be present if field is empty', async ({ page }) => {
  await page.goto('/apps/molgenis-components/#/component/InputText');
  await page.locator('div').filter({ hasText: /^My text labelSome help needed\?$/ }).getByPlaceholder('type here your text').fill('test');
  await expect(page.locator('div').filter({ hasText: /^My text labelSome help needed\?$/ }).getByPlaceholder('type here your text')).toHaveAttribute('value', 'test');
  await page.locator('div').filter({ hasText: /^My text labelSome help needed\?$/ }).getByPlaceholder('type here your text').fill('');
  expect(await page.locator('div').filter({ hasText: /^My text labelSome help needed\?$/ }).getByPlaceholder('type here your text').getAttribute('value')).toBeNull();
});

test('InputPassword: Attribute value should never be present if field is empty', async ({ page }) => {
  await page.goto('/apps/molgenis-components/#/component/InputPassword');
  await page.getByPlaceholder('Password').fill('test');
  await expect(page.getByPlaceholder('Password')).toHaveAttribute('value', 'test');
  await page.getByPlaceholder('Password').fill('');
  expect(await page.getByPlaceholder('Password').getAttribute('value')).toBeNull();
});

test('InputEmail: Attribute value should never be present if field is empty', async ({ page }) => {
  await page.goto('/apps/molgenis-components/#/component/InputEmail');
  await page.getByLabel('My email input label').fill('test@molgenis.org');
  await expect(page.getByLabel('My email input label')).toHaveAttribute('value', 'test@molgenis.org');
  await page.getByLabel('My email input label').fill('');
  expect(await page.getByLabel('My email input label').getAttribute('value')).toBeNull();
});

test('InputHyperlink: Attribute value should never be present if field is empty', async ({ page }) => {
  await page.goto('/apps/molgenis-components/#/component/InputHyperlink');
  await page.getByLabel('My hyperlink input label').fill('https://molgenis.org');
  await expect(page.getByLabel('My hyperlink input label')).toHaveAttribute('value', 'https://molgenis.org');
  await page.getByLabel('My hyperlink input label').fill('');
  expect(await page.getByLabel('My hyperlink input label').getAttribute('value')).toBeNull();
});
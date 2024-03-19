import { test, expect } from '@playwright/test';

test('computedExpression', async ({ page }) => {
  //make new database
  await page.goto('/apps/central/#/');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.getByPlaceholder('Enter username').click();
  await page.getByPlaceholder('Enter username').fill('admin');
  await page.getByPlaceholder('Enter username').press('Tab');
  await page.getByPlaceholder('Password').fill('admin');
  await page.getByPlaceholder('Password').press('Enter');
  await page.getByRole('button', { name: '' }).click();
  await page.getByLabel('name').click();
  await page.getByLabel('name').fill('computedTest');
  await page.getByLabel('template').selectOption('PET_STORE');
  await page.getByLabel('true').check();
  await page.getByRole('button', { name: 'Create database' }).click();
  //create computedExpression
  await page.getByRole('link', { name: 'computedTest' }).click();
  await page.getByRole('link', { name: 'Schema' }).click();
  await expect(page.locator('#molgenis_tables_container')).toContainText('status');
  await page.getByText('status').nth(1).click();
  await page.getByRole('button', { name: ' ' }).click();
  await page.locator('div:nth-child(6) > div:nth-child(3) > .form-group > .input-group > .form-control').click();
  await page.locator('div:nth-child(6) > div:nth-child(3) > .form-group > .input-group > .form-control').fill('name+"_"+category?.name');
  await page.getByRole('button', { name: 'Apply' }).click();
  await page.getByRole('button', { name: 'Save' }).click();
  //check if computed works
  await page.getByRole('link', { name: 'Tables' }).click();
  await page.getByRole('link', { name: 'Pet' }).click();
  await page.getByRole('button', { name: '' }).click();
  await page.locator('span').filter({ hasText: 'name (required) name is' }).locator('#Pet-edit-modal-name').click();
  await page.locator('span').filter({ hasText: 'name (required) name is' }).locator('#Pet-edit-modal-name').fill('youp');
  await page.locator('#Pet-edit-modal-category').getByRole('textbox').click();
  await page.getByRole('row', { name: 'Select   cat', exact: true }).getByRole('button').first().click();
  await page.getByRole('button', { name: 'details' }).click();
  await page.locator('span').filter({ hasText: 'status' }).locator('#Pet-edit-modal-status').click();
  await page.getByRole('spinbutton').click();
  await page.getByRole('spinbutton').fill('1');
  await page.getByRole('button', { name: 'Save Pet' }).click();
  await page.getByRole('button', { name: 'filters ' }).click();
  await page.getByLabel('name').check();
  await page.getByRole('button', { name: '' }).click();
  await page.locator('#filter-name1').click();
  await page.locator('#filter-name1').fill('youp');
  await expect(page.getByRole('row')).toContainText('youp_cat');
  //remove database
  await page.getByRole('link', { name: 'brand-logo' }).click();
  await page.getByRole('row', { name: '  computedTest' }).getByRole('button').nth(1).click();
  await page.getByRole('button', { name: 'Delete database' }).click();
  await page.getByText('Close').click();
  
});
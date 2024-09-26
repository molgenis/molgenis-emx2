import {test, expect } from "@playwright/test";

test(
  "Radio Input: values are properly defined @tw-components @tw-forms @input-radio",
  async ({ page }) => {
    await page.goto('/apps/tailwind-components/#/input/Radio.story');
    
    await page.getByLabel('No').check();
    await expect(page.getByLabel('No')).toHaveValue('No')
    
    await page.getByLabel('Yes').check();
    await expect(page.getByLabel('Yes')).toHaveValue('Yes')
  }
);

test(
  'Radio Input: labels are properly displayed @tw-components @tw-forms @input-radio',
  async ({ page }) => {
    await page.goto('/apps/tailwind-components/#/input/Radio.story');
    await expect(page.getByText('No', { exact: true })).toHaveText('No');
    await expect(page.getByText('Yes')).toHaveText('Yes');
  }
);

test(
  'Radio Input: clear button is shown when indicated @tw-components @tw-forms @input-radio',
  async ({ page }) => {
    await page.goto('/apps/tailwind-components/#/input/Radio.story');
    await expect(page.getByRole('button', { name: 'Clear' })).toBeVisible();
  }
)

test(
  'Radio Input: inputs are properly reset @tw-components @tw-forms @input-radio',
  async ({ page }) => {
    await page.goto('/apps/tailwind-components/#/input/Radio.story');
    await page.getByLabel("Healthy controls").check();
    await page.getByRole('button', { name: 'Clear' }).click();
    await expect(page.getByLabel('Healthy controls')).toBeChecked({checked: false});
  }
)
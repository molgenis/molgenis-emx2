import {test, expect } from "@playwright/test";

test(
  "Radio Input: values are properly defined @tw-components @tw-forms @input-radio",
  async ({ page }) => {
    await page.goto('/apps/tailwind-components/#/input/Radio.story');
    await expect(page.getByText('no', { exact: true })).toHaveText('no');
    await expect(page.getByText('yes')).toHaveText('yes');
  }
);

test(
  'Radio Input: labels are properly displayed @tw-components @tw-forms @input-radio',
  async ({ page }) => {
    await page.goto('/apps/tailwind-components/#/input/Radio.story');
    await expect(page.getByText('Level 1 (A)')).toHaveValue('level-1');
    await expect(page.getByText('Level 2 (AA)')).toHaveValue('level-2');
    await expect(page.getByText('Level 3 (AAA)')).toHaveValue('level-3');
  }
);

test(
  "Radio Input: default value is set @tw-components @tw-forms @input-radio",
  async ({ page }) => {
    await page.goto('/apps/tailwind-components/#/input/Radio.story');
    await expect(page.getByLabel('Experimental cohort')).toBeChecked({checked: true});
  }
)

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
    await page.getByText('Healthy controls').click();
    await page.getByRole('button', { name: 'Clear' }).click();
    await expect(page.getByLabel('Healthy controls')).toBeChecked({ checked: false });
    await expect(page.getByLabel('Experimental cohort')).toBeChecked({ checked: false });
    await expect(page.getByLabel('Placebo cohort')).toBeChecked({ checked: false }); 
  }
)

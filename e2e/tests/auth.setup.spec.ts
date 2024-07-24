import { test as setup, expect } from '@playwright/test';

// Needs to be run manually in UI mode before tests are ran.
// See also: https://playwright.dev/docs/auth#authenticating-in-ui-mode

const authFile = 'e2e/.auth/user.json';

setup('authenticate', async ({ page }) => {
    await page.goto('/apps/central/');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await page.getByPlaceholder('Enter username').click();
    await page.getByPlaceholder('Enter username').fill('admin');
    await page.getByPlaceholder('Password').click();
    await page.getByPlaceholder('Password').fill('admin');
    await page.getByRole('dialog').getByRole('button', { name: 'Sign in' }).click();
    await expect(page.getByRole('navigation')).toContainText('Hi admin');

    await page.context().storageState({ path: authFile });
});
import { test as setup, expect } from '@playwright/test';

// Needs to be run manually in UI mode before tests are ran.
// See also: https://playwright.dev/docs/auth#authenticating-in-ui-mode

const authFile = 'e2e/.auth/user.json';

setup('authenticate', async ({ page }) => {
    await page.goto('/apps/central/');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await page.getByRole('textbox', { name: 'Username' }).fill('admin');
    await page.getByRole('textbox', { name: 'Password' }).fill('admin');
    await page.getByRole('dialog').getByRole('button', { name: 'Sign in' }).click();
    await expect(page.getByRole('navigation')).toContainText('Hi admin');

    await page.context().storageState({ path: authFile });
});
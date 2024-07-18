import { test, expect } from "@playwright/test";


test("go to admin settings webpage", async ({ page }) => {
await page.goto('http://localhost:8080/apps/central/');
await page.getByRole('link', { name: 'Admin' }).click();
await page.getByRole('link', { name: 'Settings' }).click();
});
import { test, expect } from "@playwright/test";

test("Custom app is accessible", async ({ page }) => {
  await page.goto("/apps/example-app");
  await expect(page).toHaveTitle("SPA Example App");
});

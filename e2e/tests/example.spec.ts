import { test, expect } from "@playwright/test";


test("has title", async ({ page }) => {
  await page.goto("/");
  await expect(page).toHaveTitle("emx2-central");
});

test("get started link", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("link", { name: "Components (for developers)" }).click();
  await expect(page).toHaveURL(/.*molgenis-components/);
});

test("login as admin success", async ({ page }) => {
  await page.goto("/apps/central/#/admin");
  await page.getByRole('textbox', { name: 'Username' }).fill('admin');
  await page.getByRole('textbox', { name: 'Password' }).fill('admin');
  await page.getByRole('dialog').getByRole('button', { name: 'Sign in' }).click();
  await page.goto("/");
  await page.getByRole("link", { name: "_SYSTEM_" }).click();
});
import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/ui/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}admin`);
  if (await page.getByRole("button", { name: "Signin" })) {
    await page.getByRole("button", { name: "Signin" }).click();
    await page.getByRole("textbox", { name: "Username" }).click();
    await page.getByRole("textbox", { name: "Username" }).fill("admin");
    await page.getByRole("textbox", { name: "Username" }).press("Tab");
    await page.getByRole("textbox", { name: "Password" }).fill("admin");
    await page.getByRole("button", { name: "Sign in" }).click();
  }

  await expect(page.getByRole("button", { name: "Account" })).toBeVisible();
});

test("user crud", async ({ page }) => {
  await page.goto(`${route}admin`);
  // Add user
  await page.getByRole("button", { name: "Create User" }).click();
  await page.locator('[id="New username"]').click();
  await page.locator('[id="New username"]').fill("e2e-test-user");
  await page.locator('[id="New user password"]').click();
  await page.locator('[id="New user password"]').fill("testtest");
  await page.getByRole("textbox").nth(2).click();
  await page.getByRole("textbox").nth(2).fill("testtest");
  await page.getByRole("button", { name: "Add user" }).click();
  // Edit user - add role, disable
  await page.getByRole("button", { name: "Edit user: e2e-test-user" }).click();
  await page.getByRole("button", { name: "Add role" }).click();
  await page
    .locator("#disabledUserRadio-radio-group")
    .getByText("Disable")
    .click();
  await page.getByRole("button", { name: "Save" }).click();
  // Edit user - enable user
  await page.getByRole("button", { name: "Edit user: e2e-test-user" }).click();
  await page
    .locator("#disabledUserRadio-radio-group")
    .getByText("Enabled")
    .click();
  await page.getByRole("button", { name: "Save" }).click();
  // Delete user
  await page
    .getByRole("button", { name: "Delete user: e2e-test-user" })
    .click();
  await page.getByRole("button", { name: "Delete", exact: true }).click();
  await expect(
    page.getByRole("button", { name: "Edit user: e2e-test-user" })
  ).not.toBeVisible();
});

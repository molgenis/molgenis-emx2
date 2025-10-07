import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/ui/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}pet%20store/Pet`);
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

test.afterEach(async ({ page }) => {
  const targetCell = await page.getByRole("cell", { name: "e2e", exact: true });
  await expect(targetCell).toBeVisible();
  await page
    .getByText("deletee2e", { exact: true })
    .filter({ visible: false })
    .dispatchEvent("click");
  await page.getByRole("button", { name: "Delete", exact: true }).click();
});

test("add new row", async ({ page }) => {
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Pet");

  await page.getByRole("button", { name: "Add Pet" }).click();
  await page.getByRole("textbox", { name: "name Required" }).click();
  await page.getByRole("textbox", { name: "name Required" }).fill("e2e");
  await page
    .locator("#category-form-field-input-radio-group")
    .getByText("cat", { exact: true })
    .click();

  // work around for scroll to next required field
  await page.getByRole("link", { name: "Heading2" }).click();

  await page.getByRole("textbox", { name: "weight Required" }).click();
  await page.getByRole("textbox", { name: "weight Required" }).fill("23");
  await page.getByRole("button", { name: "Save Pet", exact: true }).click();
  await page.getByRole("button", { name: "Close modal", exact: true }).click();

  await expect(
    page.getByRole("cell", { name: "e2e", exact: true })
  ).toBeVisible();
});

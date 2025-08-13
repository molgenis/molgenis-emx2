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

// TO DO: this test is unstable due to non-existent elements this should be reactivated after fixing markup in table row buttons
// test.afterEach(async ({ page }) => {
//   // await expect(page.getByRole("cell", {name: "e2e"})).toBeVisible({ timeout: 1000 });
//   // await page.getByText("e2edelete Delete PetDraftgo").hover();
//   // await page.locator(`td:text("span"): + button:text("Delete")`).hover();
//   // await page.waitForTimeout(1000);

//   await page.getByRole("button", { name: "delete" }).click();
//   await page.getByRole("button", { name: "Delete", exact: true }).click();
// });

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
  await page.getByRole("button", { name: "Save", exact: true }).click();

  await expect(page.getByRole("cell", { name: "e2e" })).toBeVisible({
    timeout: 1000,
  });
});

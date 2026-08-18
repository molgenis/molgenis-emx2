import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

test.describe("Create Configurable page (cms):", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(route + "cms/pages");
    const title = await page.getByRole("heading", { name: "Pages" });
    await title.waitFor();

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
    await page.getByRole("button", { name: "Account" }).click();
    await page.getByRole("button", { name: "Sign out" }).click();
  });

  test("Add new landing page", async ({ page }) => {
    const newPageName = "playwright-test-page";

    await page.getByRole("button", { name: "Add new page" }).click();
    await page.getByRole("button", { name: "Landing page" }).click();

    await page
      .getByRole("textbox", { name: "name Required" })
      .fill(newPageName);

    await page.getByRole("button", { name: "Save", exact: true }).click();
    await page.getByRole("button", { name: "Close modal" }).click();

    await expect(
      page.getByRole("link", { name: newPageName, exact: true })
    ).toBeVisible();
  });
});

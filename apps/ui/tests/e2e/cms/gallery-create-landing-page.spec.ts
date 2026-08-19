import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

const newPageName = "playwright-test-page";

async function timeout(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

test.describe.configure({ mode: "serial", retries: 0 });

test.describe("Create Configurable page (cms):", { tag: "@cms" }, () => {
  test("Add new landing page", async ({ page }) => {
    await page.goto(route + "cms/pages");
    const title = await page.getByRole("heading", { name: "Pages" });
    await title.waitFor();

    await page.getByRole("button", { name: "Signin" }).click();
    await page.getByRole("textbox", { name: "Username" }).fill("admin");
    await page.getByRole("textbox", { name: "Password" }).fill("admin");
    await page.getByRole("button", { name: "Sign in" }).click();
    await timeout(200);

    await expect(
      page.getByRole("button", { name: "Add new page" })
    ).toBeVisible();

    await page.getByRole("button", { name: "Add new page" }).click();
    await page.getByRole("button", { name: "Landing page" }).click();

    await page
      .getByRole("textbox", { name: "name Required" })
      .fill(newPageName);

    await page.getByRole("button", { name: "Save", exact: true }).click();
    await page
      .getByRole("button", { name: "Close modal", exact: true })
      .click();

    await expect(
      page.getByRole("link", { name: newPageName, exact: true })
    ).toBeVisible();

    await page.getByRole("button", { name: "Account" }).click();
    await page.getByRole("button", { name: "Sign out" }).click();
  });
});

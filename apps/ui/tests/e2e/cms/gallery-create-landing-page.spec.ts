import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

const newPageName = "playwright-test-page";

async function timeout(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

test.describe("Create Configurable page (cms):", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(route + "cms/pages");
    const title = await page.getByRole("heading", { name: "Pages" });
    await title.waitFor();
  });

  test("Add new landing page", async ({ page }) => {
    await page.getByRole("button", { name: "Signin" }).click();
    await page.getByRole("textbox", { name: "Username" }).click();
    await page.getByRole("textbox", { name: "Username" }).fill("admin");
    await page.getByRole("textbox", { name: "Username" }).press("Tab");
    await page.getByRole("textbox", { name: "Password" }).fill("admin");
    await page.getByRole("button", { name: "Sign in" }).click();

    await timeout(200);
    await page.getByRole("button", { name: "Add new page" }).click();
    await page.getByRole("button", { name: "Landing page" }).click();

    await page
      .getByRole("textbox", { name: "name Required" })
      .fill(newPageName);

    await page.getByRole("button", { name: "Save", exact: true }).click();
    await page.getByRole("button", { name: "Close modal" }).click();

    await timeout(200);
    await expect(
      page.getByRole("link", { name: newPageName, exact: true })
    ).toBeVisible();
  });

  test("new landing page contains initial content", async ({ page }) => {
    const newPageLink = await page.getByRole("link", {
      name: newPageName,
      exact: true,
    });
    const href = await newPageLink.getAttribute("href");
    const url = href?.slice(1, href.length - 1);
    await page.goto(route + url, { waitUntil: "load" });

    const link = await page.getByText(newPageName, { exact: true });
    await link.waitFor();

    await expect(
      page.getByRole("heading", { name: "Title", exact: true })
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "Section Heading", exact: true })
    ).toBeVisible();
  });
});

import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/ui/#/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}pet%20store/Pet`);
});

test("show the table explorer for the selected table", async ({ page }) => {
  await expect(page.getByRole("heading")).toContainText("Pet");
});

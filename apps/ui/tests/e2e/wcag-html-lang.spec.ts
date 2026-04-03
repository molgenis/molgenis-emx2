import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/ui/";

test.describe("HTML lang (WCAG SC3.1.2)", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(route, { waitUntil: "load" });
  });

  test("lang is defined on the index page", async ({ page }) => {
    await expect(page.locator("html")).toHaveAttribute("lang", "en-GB");
  });

  test("lang is defined on a non-index page", async ({ page }) => {
    await page.goto(`${route}pet store`, { waitUntil: "load" });
    await expect(page.locator("html")).toHaveAttribute("lang", "en-GB");
  });
});

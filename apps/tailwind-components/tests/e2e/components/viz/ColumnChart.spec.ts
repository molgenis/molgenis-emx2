import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.describe("ColumnChart", { tag: "@tw-components @tw-viz" }, () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${route}viz/ColumnChart.story`);
  });

  test("rendered", async ({ page }) => {
    expect(await page.locator("rect").first()).toBeTruthy();
    expect(await page.locator("rect").nth(1)).toBeTruthy();
    expect(await page.locator("rect").nth(2)).toBeTruthy();
    expect(await page.locator("rect").nth(3)).toBeTruthy();

    expect(await page.locator("text").first()).toBeTruthy();
    expect(await page.locator("text").nth(1)).toBeTruthy();
    expect(await page.locator("text").nth(2)).toBeTruthy();
    expect(await page.locator("text").nth(3)).toBeTruthy();
  });
});

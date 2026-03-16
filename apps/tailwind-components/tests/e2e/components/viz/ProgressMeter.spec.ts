import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.describe("ProgressMeter", { tag: "@tw-components @tw-viz" }, () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${route}viz/ProgressMeter.story`);
    await page
      .getByRole("heading", { name: "VizProgressMeter" })
      .click({ delay: 500 });
  });

  test("rects are rendered", async ({ page }) => {
    const rects = await page.locator("g rect").all();
    expect(rects.length).toEqual(8);
  });

  test("labels are rendered", async ({ page }) => {
    const labels = await page.locator("g text").all();
    expect(labels.length).toEqual(4);
  });
});

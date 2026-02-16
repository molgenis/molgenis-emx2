import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.describe("PieChart", { tag: "@tw-components @tw-viz" }, () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${route}viz/PieChart.story`);
  });

  test("rendered", async ({ page }) => {
    // pie segments are drawn
    const segment0 = await page.locator("g.pie-slices path").nth(0);
    const segment1 = await page.locator("g.pie-slices path").nth(1);
    const segment2 = await page.locator("g.pie-slices path").nth(2);
    const segment3 = await page.locator("g.pie-slices path").nth(3);
    expect(await segment0.getAttribute("data-group")).toBe("Group A");
    expect(await segment1.getAttribute("data-group")).toBe("Group B");
    expect(await segment2.getAttribute("data-group")).toBe("Group C");
    expect(await segment3.getAttribute("data-group")).toBe("Other");

    // labels rendered as two tspan elements
    const text0 = await page.locator("g.pie-slice-labels text").nth(0);
    const text1 = await page.locator("g.pie-slice-labels text").nth(1);
    const text2 = await page.locator("g.pie-slice-labels text").nth(2);
    const text3 = await page.locator("g.pie-slice-labels text").nth(3);

    expect(await text0.locator("tspan").nth(0).innerHTML()).toBe("Group A");
    expect(await text0.locator("tspan").nth(1).innerHTML()).toBe("48%");
    expect(await text1.locator("tspan").nth(0).innerHTML()).toBe("Group B");
    expect(await text1.locator("tspan").nth(1).innerHTML()).toBe("32%");
    expect(await text2.locator("tspan").nth(0).innerHTML()).toBe("Group C");
    expect(await text2.locator("tspan").nth(1).innerHTML()).toBe("11%");
    expect(await text3.locator("tspan").nth(0).innerHTML()).toBe("Other");
    expect(await text3.locator("tspan").nth(1).innerHTML()).toBe("9%");

    // lines are displayed
    const line0 = await page.locator("g.pie-slice-labels polyline").nth(0);
    const line1 = await page.locator("g.pie-slice-labels polyline").nth(1);
    const line2 = await page.locator("g.pie-slice-labels polyline").nth(2);
    const line3 = await page.locator("g.pie-slice-labels polyline").nth(3);

    expect(await line0.getAttribute("data-group")).toBe("Group A");
    expect(await line1.getAttribute("data-group")).toBe("Group B");
    expect(await line2.getAttribute("data-group")).toBe("Group C");
    expect(await line3.getAttribute("data-group")).toBe("Other");
  });
});

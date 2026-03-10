import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.describe("PieChart", { tag: "@tw-components @tw-viz" }, () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${route}viz/PieChart.story`);
    await page
      .getByRole("heading", { name: "VizPieChart" })
      .click({ delay: 500 });
  });

  test("segements and labels are rendered", async ({ page }) => {
    const segments = await page.locator("g.pie-group path").all();
    expect(segments.length).toEqual(4);
    expect(await segments[0].getAttribute("class")).toContain(
      "stroke-chart-paths"
    );

    const labels = await page.locator("g.pie-group text").all();
    expect(labels.length).toEqual(4);
    expect(await labels[0].getAttribute("class")).toContain("fill-chart-text");

    const ticks = await page.locator("g.pie-group polyline").all();
    expect(ticks.length).toEqual(4);
    expect(await ticks[0].getAttribute("class")).toContain(
      "stroke-chart-paths"
    );
  });

  test("legend is rendered", async ({ page }) => {
    const itemMarkers = await page.locator("ul.list-style-none li svg").all();
    const itemText = await page.locator("ul.list-style-none li span").all();
    expect(itemMarkers.length).toEqual(4);
    expect(itemText.length).toEqual(4);
  });

  test("hovering emphasizes corresponding segment", async ({ page }) => {
    const firstLegendItem = await page.locator(
      "ul.list-style-none li div[data-value='Group A']"
    );
    await firstLegendItem.hover();

    const segment = await page.locator(".pie-group[data-group='Group A'] path");
    expect(await segment.getAttribute("class")).toContain("scale-125");
  });
});

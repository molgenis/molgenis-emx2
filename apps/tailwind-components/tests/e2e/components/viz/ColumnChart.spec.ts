import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.describe("ColumnChart", { tag: "@tw-components @tw-viz" }, () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${route}viz/ColumnChart.story`);
    await page
      .getByRole("heading", { name: "VizColumnChart" })
      .click({ delay: 500 });
  });

  test("columns and labels are rendered:", async ({ page }) => {
    const columns = await page.locator("g.columns rect").all();
    const columnLabels = await page.locator("g.columns text").all();
    expect(columns.length).toEqual(4);
    expect(columnLabels.length).toEqual(4);
    expect(await columnLabels[0].getAttribute("class")).toContain(
      "fill-chart-text"
    );
  });

  test("axis titles are rendered", async ({ page }) => {
    const axisTitles = await page.locator("g.titles text").all();
    expect(axisTitles.length).toEqual(2);
    expect(await axisTitles[0].getAttribute("class")).toContain(
      "fill-chart-text"
    );
  });

  test("axes are rendered", async ({ page }) => {
    const axisLines = await page.locator("g.axes g path").all();
    expect(axisLines.length).toEqual(2);

    const yAxisTicks = await page.locator("g.axes g.y-axis g.tick").all();
    const yAxisTickLines = await page
      .locator("g.axes g.y-axis g.tick line")
      .all();
    const yAxisTickLabels = await page
      .locator("g.axes g.y-axis g.tick text")
      .all();
    expect(yAxisTicks.length).toEqual(5);
    expect(yAxisTickLines.length).toEqual(5);
    expect(yAxisTickLabels.length).toEqual(5);
    expect(await yAxisTickLabels[0].getAttribute("class")).toContain(
      "fill-chart-text"
    );

    const xAxisTicks = await page.locator("g.axes g.x-axis g.tick").all();
    const xAxisTickLines = await page
      .locator("g.axes g.x-axis g.tick line")
      .all();
    const xAxisTickLabels = await page
      .locator("g.axes g.x-axis g.tick text")
      .all();
    expect(xAxisTicks.length).toEqual(4);
    expect(xAxisTickLines.length).toEqual(4);
    expect(xAxisTickLabels.length).toEqual(4);
    expect(await xAxisTickLabels[0].getAttribute("class")).toContain(
      "fill-chart-text"
    );
  });

  test("hovering on column reveals data point", async ({ page }) => {
    await page.locator("g.columns rect").first().hover();
    const rectTextElem = await page.locator("g.columns g:first-child text");
    expect(await rectTextElem.getAttribute("style")).toBe("opacity: 1;");
  });

  test("clicking on a columns emits data point", async ({ page }) => {
    await page.locator("g.columns rect").first().click();
    const selection = await page.locator("output");
    expect(await selection.innerText()).toBe(
      'Clicked element: { "name": "Group A", "value": 42 }'
    );
  });
});

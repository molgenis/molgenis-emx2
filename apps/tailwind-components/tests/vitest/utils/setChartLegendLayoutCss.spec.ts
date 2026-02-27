import { expect, test, describe } from "vitest";
import { setChartLegendLayoutCss } from "../../../app/utils/viz";

describe("setChartLegendCss (viz):", () => {
  test("default classnames are returned", () => {
    const result: string = setChartLegendLayoutCss(false);
    expect(result).toBe("chart_layout_default");
  });

  test("classname is either top or bottom", () => {
    expect(setChartLegendLayoutCss(true, "top")).toBe(
      "chart_layout_with_legend_top"
    );
    expect(setChartLegendLayoutCss(true, "bottom")).toBe(
      "chart_layout_with_legend_bottom"
    );
  });
});

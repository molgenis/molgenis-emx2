import { describe, expect, test } from "vitest";
import { parseChartTitle } from "../src/utils/parseChartTitle";

describe("parseChartTitle", () => {
  test("text is correctly replaces", () => {
    const currentTitle = "The current selected value is ${value}.";
    const dataValue = 123;
    const updatedTitle = parseChartTitle(currentTitle, dataValue, "${value}");
    expect(updatedTitle).toBe("The current selected value is 123.");
  });
});

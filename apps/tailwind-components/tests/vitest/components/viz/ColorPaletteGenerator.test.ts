import { describe, expect, test } from "vitest";

import { ColorPaletteGenerator } from "../../../../app/utils/viz/ColorPaletteGenerator";
import {
  recruitmentDataExample,
  recruitmentTargetExample,
  recruitmentByGender,
} from "../../../../app/utils/viz/data";

describe("Viz: Color palette generator:", () => {
  test("default color is used when no palette or fill color is defined", () => {
    const palette = ColorPaletteGenerator({
      data: recruitmentDataExample,
      axisDomains: recruitmentDataExample.map((row) => row.xValue),
    });

    const generator = palette.generator;
    expect(generator("Feb")).toEqual("#014f9e");
  });

  test("row-level fill color is used if no palette is defined", () => {
    const palette = ColorPaletteGenerator({
      data: recruitmentTargetExample,
      axisDomains: recruitmentTargetExample.map((row) => row.xValue),
    });
    const generator = palette.generator;
    expect(generator("Feb")).toEqual("#014f9e");
    expect(generator("Apr")).toEqual("#e14f62");
  });

  test("grouped charts returns default palette", () => {
    const domains = [...new Set(recruitmentByGender.map((row) => row.xValue))];
    const palette = ColorPaletteGenerator({
      data: recruitmentByGender,
      axisDomains: domains,
      isGroupedChart: true,
    });
    const generator = palette.generator;
    expect(generator("Female")).toEqual("#deebf7");
    expect(generator("Male")).toEqual("#9ecae1");
    expect(generator("Other")).toEqual("#3182bd");
  });
});

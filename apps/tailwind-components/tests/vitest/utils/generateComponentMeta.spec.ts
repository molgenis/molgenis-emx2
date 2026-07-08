import { describe, it, expect } from "vitest";
import { join } from "path";
import {
  deriveComponentName,
  extractTypeDetail,
} from "../../../app/utils/generateComponentMeta.mjs";

const componentsDir = join(__dirname, "../../../app/components");

describe("deriveComponentName", () => {
  it("collapses eponymous folder — viz/ProgressMeter/ProgressMeter.vue → VizProgressMeter", () => {
    const filePath = join(componentsDir, "viz/ProgressMeter/ProgressMeter.vue");
    expect(deriveComponentName(filePath, componentsDir)).toBe(
      "VizProgressMeter"
    );
  });

  it("collapses eponymous folder — viz/PieChart/PieChart.vue → VizPieChart", () => {
    const filePath = join(componentsDir, "viz/PieChart/PieChart.vue");
    expect(deriveComponentName(filePath, componentsDir)).toBe("VizPieChart");
  });

  it("collapses eponymous folder — viz/ColumnChart/ColumnChart.vue → VizColumnChart", () => {
    const filePath = join(componentsDir, "viz/ColumnChart/ColumnChart.vue");
    expect(deriveComponentName(filePath, componentsDir)).toBe("VizColumnChart");
  });

  it("collapses eponymous folder — viz/ChartLegend/ChartLegend.vue → VizChartLegend", () => {
    const filePath = join(componentsDir, "viz/ChartLegend/ChartLegend.vue");
    expect(deriveComponentName(filePath, componentsDir)).toBe("VizChartLegend");
  });

  it("flat file is unchanged — viz/ChartTitle.vue → VizChartTitle", () => {
    const filePath = join(componentsDir, "viz/ChartTitle.vue");
    expect(deriveComponentName(filePath, componentsDir)).toBe("VizChartTitle");
  });

  it("non-eponymous nested file is unchanged — viz/ChartLegend/ChartLegendMarker.vue → VizChartLegendChartLegendMarker", () => {
    const filePath = join(
      componentsDir,
      "viz/ChartLegend/ChartLegendMarker.vue"
    );
    expect(deriveComponentName(filePath, componentsDir)).toBe(
      "VizChartLegendChartLegendMarker"
    );
  });
});

describe("extractTypeDetail", () => {
  it("returns null for a primitive string schema", () => {
    expect(extractTypeDetail("string")).toBeNull();
  });

  it("returns null for null/undefined schema", () => {
    expect(extractTypeDetail(null)).toBeNull();
    expect(extractTypeDetail(undefined)).toBeNull();
  });

  it("extracts union options from an enum schema with multiple string options", () => {
    const schema = {
      kind: "enum",
      type: '"primary" | "secondary" | "tertiary"',
      schema: ['"primary"', '"secondary"', '"tertiary"'],
    };
    const result = extractTypeDetail(schema);
    expect(result).toEqual({
      kind: "union",
      options: ['"primary"', '"secondary"', '"tertiary"'],
    });
  });

  it("returns null when only option after filtering undefined is a single primitive", () => {
    const schema = {
      kind: "enum",
      type: "string | undefined",
      schema: ["string", "undefined"],
    };
    expect(extractTypeDetail(schema)).toBeNull();
  });

  it("keeps non-primitive options after filtering undefined from union", () => {
    const schema = {
      kind: "enum",
      type: "ButtonType | undefined",
      schema: ['"primary"', '"secondary"', "undefined"],
    };
    const result = extractTypeDetail(schema);
    expect(result).toEqual({
      kind: "union",
      options: ['"primary"', '"secondary"'],
    });
  });

  it("returns null for a single-primitive enum (no meaningful expansion)", () => {
    const schema = {
      kind: "enum",
      type: "boolean",
      schema: ["boolean"],
    };
    expect(extractTypeDetail(schema)).toBeNull();
  });

  it("extracts array element type from an array schema with object children", () => {
    const schema = {
      kind: "array",
      type: "DatasetRow[]",
      schema: [{ kind: "object", type: "DatasetRow", schema: {} }],
    };
    const result = extractTypeDetail(schema);
    expect(result).toEqual({ kind: "array", elementType: "DatasetRow" });
  });

  it("returns null for an array of primitives", () => {
    const schema = {
      kind: "array",
      type: "string[]",
      schema: ["string"],
    };
    expect(extractTypeDetail(schema)).toBeNull();
  });

  it("extracts object members (one level, types as strings)", () => {
    const schema = {
      kind: "object",
      type: "DatasetRow",
      schema: {
        id: {
          name: "id",
          type: "string",
          required: true,
          description: "",
          global: false,
          tags: [],
          declarations: [],
          schema: "string",
        },
        value: {
          name: "value",
          type: "number",
          required: false,
          description: "",
          global: false,
          tags: [],
          declarations: [],
          schema: "number",
        },
      },
    };
    const result = extractTypeDetail(schema);
    expect(result).toEqual({
      kind: "object",
      members: { id: "string", value: "number" },
    });
  });

  it("returns null for an empty object schema", () => {
    const schema = { kind: "object", type: "{}", schema: {} };
    expect(extractTypeDetail(schema)).toBeNull();
  });

  it("handles enum schema entries as objects with a type field (nested schema objects)", () => {
    const schema = {
      kind: "enum",
      type: "ColorPalette | undefined",
      schema: [
        { kind: "object", type: "ColorPalette", schema: {} },
        "undefined",
      ],
    };
    const result = extractTypeDetail(schema);
    expect(result).toEqual({ kind: "union", options: ["ColorPalette"] });
  });
});

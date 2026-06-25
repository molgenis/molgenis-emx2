import { describe, it, expect } from "vitest";
import {
  getPaletteColorRecords,
  getSemanticColorRecords,
  getTypographyRecords,
  getSpacingRecords,
  getBorderRadiusRecords,
  getBoxShadowRecords,
  getIconNames,
} from "../../../app/utils/designTokens";

describe("getPaletteColorRecords", () => {
  it("returns records for each palette color family", () => {
    const records = getPaletteColorRecords();
    const families = [...new Set(records.map((r) => r.family))];
    expect(families).toContain("blue");
    expect(families).toContain("gray");
    expect(families).toContain("yellow");
    expect(families).toContain("green");
    expect(families).toContain("orange");
    expect(families).toContain("red");
  });

  it("each record has tokenName, family, shade, and cssValue", () => {
    const records = getPaletteColorRecords();
    expect(records.length).toBeGreaterThan(0);
    for (const record of records) {
      expect(record).toHaveProperty("tokenName");
      expect(record).toHaveProperty("family");
      expect(record).toHaveProperty("shade");
      expect(record).toHaveProperty("cssValue");
      expect(typeof record.tokenName).toBe("string");
    }
  });

  it("blue-500 record has the correct token name", () => {
    const records = getPaletteColorRecords();
    const blue500 = records.find(
      (r) => r.family === "blue" && r.shade === "500"
    );
    expect(blue500).toBeDefined();
    expect(blue500!.tokenName).toBe("bg-blue-500");
  });
});

describe("getSemanticColorRecords", () => {
  it("returns records grouped by category (background, text, border)", () => {
    const records = getSemanticColorRecords();
    const categories = [...new Set(records.map((r) => r.category))];
    expect(categories).toContain("background");
    expect(categories).toContain("text");
    expect(categories).toContain("border");
  });

  it("each record has tokenName, category, and cssVar", () => {
    const records = getSemanticColorRecords();
    expect(records.length).toBeGreaterThan(0);
    for (const record of records) {
      expect(record).toHaveProperty("tokenName");
      expect(record).toHaveProperty("category");
      expect(record).toHaveProperty("cssVar");
    }
  });

  it("button-primary background token is included", () => {
    const records = getSemanticColorRecords();
    const found = records.find(
      (r) => r.category === "background" && r.tokenName === "bg-button-primary"
    );
    expect(found).toBeDefined();
  });

  it("title text token is included", () => {
    const records = getSemanticColorRecords();
    const found = records.find(
      (r) => r.category === "text" && r.tokenName === "text-title"
    );
    expect(found).toBeDefined();
  });
});

describe("getTypographyRecords", () => {
  it("returns records for all heading-* and body-* sizes", () => {
    const records = getTypographyRecords();
    const names = records.map((r) => r.tokenName);
    expect(names).toContain("text-heading-3xl");
    expect(names).toContain("text-body-base");
    expect(names).toContain("text-body-sm");
    expect(names).toContain("text-heading-7xl");
  });

  it("each record has tokenName, sizeRem, lineHeight, and category", () => {
    const records = getTypographyRecords();
    expect(records.length).toBeGreaterThan(0);
    for (const record of records) {
      expect(record).toHaveProperty("tokenName");
      expect(record).toHaveProperty("sizeRem");
      expect(record).toHaveProperty("lineHeight");
      expect(record).toHaveProperty("category");
    }
  });

  it("heading-3xl has correct size", () => {
    const records = getTypographyRecords();
    const heading3xl = records.find((r) => r.tokenName === "text-heading-3xl");
    expect(heading3xl).toBeDefined();
    expect(heading3xl!.sizeRem).toBe("1.5625rem");
    expect(heading3xl!.lineHeight).toBe("1.2");
    expect(heading3xl!.category).toBe("heading");
  });

  it("body-base has correct line height", () => {
    const records = getTypographyRecords();
    const bodyBase = records.find((r) => r.tokenName === "text-body-base");
    expect(bodyBase).toBeDefined();
    expect(bodyBase!.sizeRem).toBe("1rem");
    expect(bodyBase!.lineHeight).toBe("1.8");
    expect(bodyBase!.category).toBe("body");
  });
});

describe("getSpacingRecords", () => {
  it("returns records derived from theme spacing extension", () => {
    const records = getSpacingRecords();
    expect(records.length).toBeGreaterThan(0);
  });

  it("each record has tokenName, cssValue, and displayLabel", () => {
    const records = getSpacingRecords();
    for (const record of records) {
      expect(record).toHaveProperty("tokenName");
      expect(record).toHaveProperty("cssValue");
      expect(record).toHaveProperty("displayLabel");
    }
  });

  it("spacing-18 is included with value 4.5rem", () => {
    const records = getSpacingRecords();
    const spacing18 = records.find((r) => r.tokenName === "spacing-18");
    expect(spacing18).toBeDefined();
    expect(spacing18!.cssValue).toBe("4.5rem");
  });
});

describe("getBorderRadiusRecords", () => {
  it("returns records derived from theme borderRadius extension", () => {
    const records = getBorderRadiusRecords();
    expect(records.length).toBeGreaterThan(0);
  });

  it("each record has tokenName, cssVar, and label", () => {
    const records = getBorderRadiusRecords();
    for (const record of records) {
      expect(record).toHaveProperty("tokenName");
      expect(record).toHaveProperty("cssVar");
      expect(record).toHaveProperty("label");
    }
  });

  it("rounded-theme is included", () => {
    const records = getBorderRadiusRecords();
    const found = records.find((r) => r.tokenName === "rounded-theme");
    expect(found).toBeDefined();
  });
});

describe("getBoxShadowRecords", () => {
  it("returns records derived from theme boxShadow extension", () => {
    const records = getBoxShadowRecords();
    expect(records.length).toBeGreaterThan(0);
  });

  it("each record has tokenName, cssVar, and label", () => {
    const records = getBoxShadowRecords();
    for (const record of records) {
      expect(record).toHaveProperty("tokenName");
      expect(record).toHaveProperty("cssVar");
      expect(record).toHaveProperty("label");
    }
  });

  it("shadow-primary is included", () => {
    const records = getBoxShadowRecords();
    const found = records.find((r) => r.tokenName === "shadow-primary");
    expect(found).toBeDefined();
  });
});

describe("getIconNames", () => {
  it("returns an array of PascalCase icon component names", () => {
    const names = getIconNames();
    expect(names.length).toBeGreaterThan(0);
    expect(names).toContain("ArrowDown");
    expect(names).toContain("Check");
    expect(names).toContain("Search");
  });
});

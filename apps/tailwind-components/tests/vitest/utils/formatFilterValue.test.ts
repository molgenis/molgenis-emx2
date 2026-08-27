import { describe, it, expect } from "vitest";
import {
  formatFilterValue,
  extractDisplayValue,
} from "../../../app/utils/formatFilterValue";

const boolLabels = { true: "Yes", false: "No", _null_: "Not set" };

describe("extractDisplayValue", () => {
  it("returns name when present", () => {
    expect(extractDisplayValue({ name: "Flu", label: "Influenza" })).toBe(
      "Flu"
    );
  });

  it("returns label when name is absent", () => {
    expect(extractDisplayValue({ label: "Influenza", code: "J09" })).toBe(
      "Influenza"
    );
  });

  it("returns first value when neither name nor label is present", () => {
    expect(extractDisplayValue({ code: "J09" })).toBe("J09");
  });
});

describe("formatFilterValue", () => {
  describe("between operator", () => {
    it("formats both min and max", () => {
      const result = formatFilterValue(
        { operator: "between", value: [5, 20] },
        {}
      );
      expect(result).toEqual({ displayValue: "5 - 20", values: [] });
    });

    it("formats min only", () => {
      const result = formatFilterValue(
        { operator: "between", value: [5, null] },
        {}
      );
      expect(result).toEqual({ displayValue: "≥ 5", values: [] });
    });

    it("formats max only", () => {
      const result = formatFilterValue(
        { operator: "between", value: [null, 20] },
        {}
      );
      expect(result).toEqual({ displayValue: "≤ 20", values: [] });
    });

    it("returns empty when both are null", () => {
      const result = formatFilterValue(
        { operator: "between", value: [null, null] },
        {}
      );
      expect(result).toEqual({ displayValue: "", values: [] });
    });
  });

  describe("equals operator with ref values", () => {
    it("returns empty for empty array", () => {
      const result = formatFilterValue({ operator: "equals", value: [] }, {});
      expect(result).toEqual({ displayValue: "", values: [] });
    });

    it("displays name for single object with name", () => {
      const result = formatFilterValue(
        { operator: "equals", value: [{ name: "Flu" }] },
        {}
      );
      expect(result).toEqual({ displayValue: "Flu", values: [] });
    });

    it("displays label for single object with label but no name", () => {
      const result = formatFilterValue(
        { operator: "equals", value: [{ label: "Influenza" }] },
        {}
      );
      expect(result).toEqual({ displayValue: "Influenza", values: [] });
    });

    it("returns count and all values for multiple objects", () => {
      const result = formatFilterValue(
        {
          operator: "equals",
          value: [{ name: "Flu" }, { name: "Cold" }, { name: "RSV" }],
        },
        {}
      );
      expect(result).toEqual({
        displayValue: "3",
        values: ["Flu", "Cold", "RSV"],
      });
    });

    it("displays string for single non-object value", () => {
      const result = formatFilterValue(
        { operator: "equals", value: ["active"] },
        {}
      );
      expect(result).toEqual({ displayValue: "active", values: [] });
    });

    it("returns count and all values for multiple non-object values", () => {
      const result = formatFilterValue(
        { operator: "equals", value: ["a", "b", "c"] },
        {}
      );
      expect(result).toEqual({ displayValue: "3", values: ["a", "b", "c"] });
    });

    it("displays name for single object passed directly (not in array)", () => {
      const result = formatFilterValue(
        { operator: "equals", value: { name: "Flu" } as any },
        {}
      );
      expect(result).toEqual({ displayValue: "Flu", values: [] });
    });
  });

  describe("notNull operator", () => {
    it("returns 'has value'", () => {
      const result = formatFilterValue(
        { operator: "notNull", value: null },
        {}
      );
      expect(result).toEqual({ displayValue: "has value", values: [] });
    });
  });

  describe("isNull operator", () => {
    it("returns 'is empty'", () => {
      const result = formatFilterValue({ operator: "isNull", value: null }, {});
      expect(result).toEqual({ displayValue: "is empty", values: [] });
    });
  });

  describe("like operator", () => {
    it("formats plain string value", () => {
      const result = formatFilterValue(
        { operator: "like", value: "search term" },
        {}
      );
      expect(result).toEqual({ displayValue: "search term", values: [] });
    });

    it("formats single object value", () => {
      const result = formatFilterValue(
        { operator: "like", value: { name: "Hospital A" } as any },
        {}
      );
      expect(result).toEqual({ displayValue: "Hospital A", values: [] });
    });

    it("returns count and values for array of objects", () => {
      const result = formatFilterValue(
        { operator: "like", value: [{ name: "A" }, { name: "B" }] },
        {}
      );
      expect(result).toEqual({ displayValue: "2", values: ["A", "B"] });
    });

    it("returns count and values for array of primitives", () => {
      const result = formatFilterValue(
        { operator: "like", value: ["foo", "bar"] },
        {}
      );
      expect(result).toEqual({ displayValue: "2", values: ["foo", "bar"] });
    });
  });

  describe("equals operator", () => {
    it("formats plain string value", () => {
      const result = formatFilterValue(
        { operator: "equals", value: "exact match" },
        {}
      );
      expect(result).toEqual({ displayValue: "exact match", values: [] });
    });

    it("formats single object value", () => {
      const result = formatFilterValue(
        { operator: "equals", value: { name: "Category X" } as any },
        {}
      );
      expect(result).toEqual({ displayValue: "Category X", values: [] });
    });

    it("returns count and values for multiple objects", () => {
      const result = formatFilterValue(
        { operator: "equals", value: [{ name: "X" }, { name: "Y" }] },
        {}
      );
      expect(result).toEqual({ displayValue: "2", values: ["X", "Y"] });
    });

    it("returns count and values for multiple primitives", () => {
      const result = formatFilterValue(
        { operator: "equals", value: ["p", "q"] },
        {}
      );
      expect(result).toEqual({ displayValue: "2", values: ["p", "q"] });
    });
  });

  describe("BOOL label resolution", () => {
    it("shows 'Yes' for true value", () => {
      const result = formatFilterValue(
        { operator: "equals", value: ["true"] },
        boolLabels
      );
      expect(result).toEqual({ displayValue: "Yes", values: [] });
    });

    it("shows 'No' for false value", () => {
      const result = formatFilterValue(
        { operator: "equals", value: ["false"] },
        boolLabels
      );
      expect(result).toEqual({ displayValue: "No", values: [] });
    });

    it("shows 'Not set' for _null_ value", () => {
      const result = formatFilterValue(
        { operator: "equals", value: ["_null_"] },
        boolLabels
      );
      expect(result).toEqual({ displayValue: "Not set", values: [] });
    });

    it("shows all three BOOL labels in multi-value chip", () => {
      const result = formatFilterValue(
        { operator: "equals", value: ["true", "false", "_null_"] },
        boolLabels
      );
      expect(result).toEqual({
        displayValue: "3",
        values: ["Yes", "No", "Not set"],
      });
    });
  });

  describe("ONTOLOGY label resolution", () => {
    it("shows ontology label instead of name for single value", () => {
      const labels = { ncit_C123: "Heart Disease" };
      const result = formatFilterValue(
        { operator: "equals", value: ["ncit_C123"] },
        labels
      );
      expect(result).toEqual({ displayValue: "Heart Disease", values: [] });
    });

    it("shows multiple ontology labels in multi-value chip", () => {
      const labels = { ncit_C1: "Heart Disease", ncit_C2: "Lung Cancer" };
      const result = formatFilterValue(
        { operator: "equals", value: ["ncit_C1", "ncit_C2"] },
        labels
      );
      expect(result).toEqual({
        displayValue: "2",
        values: ["Heart Disease", "Lung Cancer"],
      });
    });

    it("falls through to raw value when not in optionLabels", () => {
      const result = formatFilterValue(
        { operator: "equals", value: ["unknown_term"] },
        {}
      );
      expect(result).toEqual({ displayValue: "unknown_term", values: [] });
    });
  });
});

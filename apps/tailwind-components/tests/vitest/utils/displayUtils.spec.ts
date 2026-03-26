import { describe, it, expect } from "vitest";
import {
  isEmptyValue,
  isTopSection,
  buildRefbackFilter,
  getRowLabel,
  filterDataColumns,
  filterNonEmptyColumns,
} from "../../../app/utils/displayUtils";
import type { IColumn } from "../../../../metadata-utils/src/types";

describe("isEmptyValue", () => {
  it("returns true for null", () => {
    expect(isEmptyValue(null)).toBe(true);
  });

  it("returns true for undefined", () => {
    expect(isEmptyValue(undefined)).toBe(true);
  });

  it("returns true for empty string", () => {
    expect(isEmptyValue("")).toBe(true);
  });

  it("returns true for empty array", () => {
    expect(isEmptyValue([])).toBe(true);
  });

  it("returns true for empty object", () => {
    expect(isEmptyValue({})).toBe(true);
  });

  it("returns false for non-empty string", () => {
    expect(isEmptyValue("hello")).toBe(false);
  });

  it("returns false for number 0", () => {
    expect(isEmptyValue(0)).toBe(false);
  });

  it("returns false for false", () => {
    expect(isEmptyValue(false)).toBe(false);
  });

  it("returns false for non-empty array", () => {
    expect(isEmptyValue([1])).toBe(false);
  });

  it("returns false for non-empty object", () => {
    expect(isEmptyValue({ a: 1 })).toBe(false);
  });
});

describe("isTopSection", () => {
  it("returns false for null", () => {
    expect(isTopSection(null)).toBe(false);
  });

  it("returns false for undefined", () => {
    expect(isTopSection(undefined)).toBe(false);
  });

  it("returns true for id _top", () => {
    expect(isTopSection({ id: "_top" })).toBe(true);
  });

  it("returns true for id mg_top_of_form", () => {
    expect(isTopSection({ id: "mg_top_of_form" })).toBe(true);
  });

  it("returns true for label _top", () => {
    expect(isTopSection({ id: "something", label: "_top" })).toBe(true);
  });

  it("returns false for regular section", () => {
    expect(isTopSection({ id: "population", label: "Population" })).toBe(false);
  });
});

describe("buildRefbackFilter", () => {
  it("builds filter for REFBACK with single key", () => {
    expect(buildRefbackFilter("REFBACK", "resource", { id: "ALSPAC" })).toEqual(
      { resource: { id: { equals: "ALSPAC" } } }
    );
  });

  it("builds filter for REFBACK with composite key", () => {
    expect(
      buildRefbackFilter("REFBACK", "dataset", {
        resource: "ALSPAC",
        name: "core",
      })
    ).toEqual({
      dataset: {
        resource: { equals: "ALSPAC" },
        name: { equals: "core" },
      },
    });
  });

  it("returns undefined for non-REFBACK column", () => {
    expect(
      buildRefbackFilter("REF_ARRAY", "resource", { id: "ALSPAC" })
    ).toBeUndefined();
  });

  it("returns undefined when refBackId is missing", () => {
    expect(
      buildRefbackFilter("REFBACK", undefined, { id: "ALSPAC" })
    ).toBeUndefined();
  });

  it("returns undefined when parentRowId is missing", () => {
    expect(buildRefbackFilter("REFBACK", "resource", undefined)).toBeUndefined();
  });
});

describe("getRowLabel", () => {
  it("uses template when provided", () => {
    expect(
      getRowLabel({ name: "ds1", label: "Dataset 1" }, "${name} - ${label}")
    ).toBe("ds1 - Dataset 1");
  });

  it("replaces missing template vars with empty string", () => {
    expect(getRowLabel({ name: "ds1" }, "${name} - ${label}")).toBe("ds1 - ");
  });

  it("falls back to label", () => {
    expect(getRowLabel({ label: "My Label", name: "myname" })).toBe("My Label");
  });

  it("falls back to name when no label", () => {
    expect(getRowLabel({ name: "myname", id: "123" })).toBe("myname");
  });

  it("falls back to id when no label or name", () => {
    expect(getRowLabel({ id: "123" })).toBe("123");
  });

  it("falls back to JSON when nothing else", () => {
    expect(getRowLabel({ foo: "bar" })).toBe('{"foo":"bar"}');
  });
});

function col(overrides: Partial<IColumn>): IColumn {
  return {
    id: "test",
    label: "Test",
    columnType: "STRING",
    ...overrides,
  } as IColumn;
}

describe("filterDataColumns", () => {
  const columns = [
    col({ id: "name", columnType: "STRING" }),
    col({ id: "mg_top_of_form", columnType: "SECTION" }),
    col({ id: "population", columnType: "HEADING" }),
    col({ id: "mg_insertedOn", columnType: "DATETIME" }),
    col({ id: "description", columnType: "TEXT" }),
    col({ id: "resource", columnType: "REF" }),
  ];

  it("filters out SECTION, HEADING, and mg_ columns", () => {
    const result = filterDataColumns(columns);
    expect(result.map((c) => c.id)).toEqual(["name", "description", "resource"]);
  });

  it("also hides columns in hideColumns list", () => {
    const result = filterDataColumns(columns, ["resource"]);
    expect(result.map((c) => c.id)).toEqual(["name", "description"]);
  });

  it("returns all data columns when hideColumns is empty", () => {
    const result = filterDataColumns(columns, []);
    expect(result.map((c) => c.id)).toEqual(["name", "description", "resource"]);
  });
});

describe("filterNonEmptyColumns", () => {
  const columns = [
    col({ id: "name" }),
    col({ id: "description" }),
    col({ id: "status" }),
  ];

  it("removes columns where all rows are empty", () => {
    const rows = [
      { name: "A", description: null, status: "" },
      { name: "B", description: undefined, status: "" },
    ];
    const result = filterNonEmptyColumns(columns, rows);
    expect(result.map((c) => c.id)).toEqual(["name"]);
  });

  it("keeps column if at least one row has a value", () => {
    const rows = [
      { name: "A", description: null, status: "active" },
      { name: "B", description: undefined, status: "" },
    ];
    const result = filterNonEmptyColumns(columns, rows);
    expect(result.map((c) => c.id)).toEqual(["name", "status"]);
  });

  it("returns all columns when rows is empty", () => {
    const result = filterNonEmptyColumns(columns, []);
    expect(result.map((c) => c.id)).toEqual(["name", "description", "status"]);
  });

  it("treats empty objects as empty", () => {
    const rows = [{ name: "A", description: {}, status: [] }];
    const result = filterNonEmptyColumns(columns, rows);
    expect(result.map((c) => c.id)).toEqual(["name"]);
  });
});

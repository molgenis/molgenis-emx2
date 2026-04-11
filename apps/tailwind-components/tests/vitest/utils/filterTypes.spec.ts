import { describe, it, expect } from "vitest";
import {
  filterValueToTreeSelection,
  treeSelectionToFilterValue,
} from "../../../app/utils/filterTypes";
import type { IFilterValue } from "../../../types/filters";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { CountedOption } from "../../../app/utils/fetchCounts";

const ontologyColumn: IColumn = {
  columnType: "ONTOLOGY",
  id: "disease",
  label: "Disease",
};
const radioColumn: IColumn = {
  columnType: "RADIO",
  id: "status",
  label: "Status",
};

describe("filterValueToTreeSelection", () => {
  it("returns empty array for undefined", () => {
    expect(filterValueToTreeSelection(undefined)).toEqual([]);
  });

  it("returns empty array for non-equals operator", () => {
    const filterValue: IFilterValue = { operator: "like", value: "foo" };
    expect(filterValueToTreeSelection(filterValue)).toEqual([]);
  });

  it("returns empty array for between operator", () => {
    const filterValue: IFilterValue = { operator: "between", value: [1, 5] };
    expect(filterValueToTreeSelection(filterValue)).toEqual([]);
  });

  it("returns single-element array for string value", () => {
    const filterValue: IFilterValue = { operator: "equals", value: "single" };
    expect(filterValueToTreeSelection(filterValue)).toEqual(["single"]);
  });

  it("returns empty array for non-string non-array value", () => {
    const filterValue = {
      operator: "equals",
      value: 42,
    } as unknown as IFilterValue;
    expect(filterValueToTreeSelection(filterValue)).toEqual([]);
  });

  it("returns array of strings for string array value", () => {
    const filterValue: IFilterValue = { operator: "equals", value: ["a", "b"] };
    expect(filterValueToTreeSelection(filterValue)).toEqual(["a", "b"]);
  });

  it("filters out nulls from array value", () => {
    const filterValue = {
      operator: "equals",
      value: ["a", null, "b"],
    } as unknown as IFilterValue;
    expect(filterValueToTreeSelection(filterValue)).toEqual(["a", "b"]);
  });

  it("converts single-key object to its string value", () => {
    const filterValue: IFilterValue = {
      operator: "equals",
      value: [{ name: "Alice" }],
    };
    expect(filterValueToTreeSelection(filterValue)).toEqual(["Alice"]);
  });

  it("joins multiple object values with comma-space for multi-key objects", () => {
    const filterValue: IFilterValue = {
      operator: "equals",
      value: [{ firstName: "A", lastName: "B" }],
    };
    expect(filterValueToTreeSelection(filterValue)).toEqual(["A, B"]);
  });
});

describe("treeSelectionToFilterValue", () => {
  it("returns undefined for empty selection", () => {
    expect(treeSelectionToFilterValue([], ontologyColumn, [])).toBeUndefined();
  });

  it("returns equals filter with selected values for non-REF column type", () => {
    const result = treeSelectionToFilterValue(["a", "b"], ontologyColumn, []);
    expect(result).toEqual({ operator: "equals", value: ["a", "b"] });
  });

  it("returns equals filter with selected values for REF column with no keyObjects", () => {
    const options: CountedOption[] = [
      { name: "active", count: 3 },
      { name: "inactive", count: 1 },
    ];
    const result = treeSelectionToFilterValue(["active"], radioColumn, options);
    expect(result).toEqual({ operator: "equals", value: ["active"] });
  });

  it("returns equals filter with selected values for REF column with single-key keyObjects", () => {
    const options: CountedOption[] = [
      { name: "Alice", count: 2, keyObject: { name: "Alice" } },
      { name: "Bob", count: 1, keyObject: { name: "Bob" } },
    ];
    const result = treeSelectionToFilterValue(["Alice"], radioColumn, options);
    expect(result).toEqual({ operator: "equals", value: ["Alice"] });
  });

  it("returns keyObject array for REF column with composite keyObjects", () => {
    const options: CountedOption[] = [
      {
        name: "Alice Smith",
        count: 2,
        keyObject: { firstName: "Alice", lastName: "Smith" },
      },
      {
        name: "Bob Jones",
        count: 1,
        keyObject: { firstName: "Bob", lastName: "Jones" },
      },
    ];
    const result = treeSelectionToFilterValue(
      ["Alice Smith", "Bob Jones"],
      radioColumn,
      options
    );
    expect(result).toEqual({
      operator: "equals",
      value: [
        { firstName: "Alice", lastName: "Smith" },
        { firstName: "Bob", lastName: "Jones" },
      ],
    });
  });

  it("falls back to name object when selected name is missing from options", () => {
    const options: CountedOption[] = [
      {
        name: "Alice Smith",
        count: 2,
        keyObject: { firstName: "Alice", lastName: "Smith" },
      },
    ];
    const result = treeSelectionToFilterValue(
      ["Alice Smith", "Unknown Person"],
      radioColumn,
      options
    );
    expect(result).toEqual({
      operator: "equals",
      value: [
        { firstName: "Alice", lastName: "Smith" },
        { name: "Unknown Person" },
      ],
    });
  });
});

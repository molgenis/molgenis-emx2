import { describe, it, expect } from "vitest";
import {
  isSelectableFilterType,
  isStringFilterType,
  isRefExpandable,
  navDepth,
  shouldExcludeSelfRef,
  isExcludedColumn,
} from "../../../app/utils/filterTreeUtils";
import type { IColumn } from "../../../../metadata-utils/src/types";

describe("isSelectableFilterType", () => {
  it("returns true for BOOL (runtime enum value)", () => {
    expect(isSelectableFilterType("BOOL")).toBe(true);
  });

  it("returns false for BOOLEAN (not a valid column type)", () => {
    expect(isSelectableFilterType("BOOLEAN")).toBe(false);
  });

  it("returns true for ONTOLOGY", () => {
    expect(isSelectableFilterType("ONTOLOGY")).toBe(true);
  });

  it("returns true for ONTOLOGY_ARRAY", () => {
    expect(isSelectableFilterType("ONTOLOGY_ARRAY")).toBe(true);
  });

  it("returns true for RADIO", () => {
    expect(isSelectableFilterType("RADIO")).toBe(true);
  });

  it("returns true for CHECKBOX", () => {
    expect(isSelectableFilterType("CHECKBOX")).toBe(true);
  });

  it("returns true for DATE", () => {
    expect(isSelectableFilterType("DATE")).toBe(true);
  });

  it("returns true for DATE_ARRAY", () => {
    expect(isSelectableFilterType("DATE_ARRAY")).toBe(true);
  });

  it("returns true for INT", () => {
    expect(isSelectableFilterType("INT")).toBe(true);
  });

  it("returns true for INT_ARRAY", () => {
    expect(isSelectableFilterType("INT_ARRAY")).toBe(true);
  });

  it("returns true for DECIMAL", () => {
    expect(isSelectableFilterType("DECIMAL")).toBe(true);
  });

  it("returns true for DECIMAL_ARRAY", () => {
    expect(isSelectableFilterType("DECIMAL_ARRAY")).toBe(true);
  });

  it("returns true for LONG", () => {
    expect(isSelectableFilterType("LONG")).toBe(true);
  });

  it("returns true for NON_NEGATIVE_INT", () => {
    expect(isSelectableFilterType("NON_NEGATIVE_INT")).toBe(true);
  });

  it("returns true for NON_NEGATIVE_INT_ARRAY", () => {
    expect(isSelectableFilterType("NON_NEGATIVE_INT_ARRAY")).toBe(true);
  });

  it("returns false for STRING (string type, not selectable)", () => {
    expect(isSelectableFilterType("STRING")).toBe(false);
  });

  it("returns false for REF (expandable type, not selectable)", () => {
    expect(isSelectableFilterType("REF")).toBe(false);
  });

  it("returns false for HEADING (excluded type)", () => {
    expect(isSelectableFilterType("HEADING")).toBe(false);
  });
});

describe("isStringFilterType", () => {
  it("returns true for STRING", () => {
    expect(isStringFilterType("STRING")).toBe(true);
  });

  it("returns true for STRING_ARRAY", () => {
    expect(isStringFilterType("STRING_ARRAY")).toBe(true);
  });

  it("returns true for TEXT", () => {
    expect(isStringFilterType("TEXT")).toBe(true);
  });

  it("returns true for EMAIL", () => {
    expect(isStringFilterType("EMAIL")).toBe(true);
  });

  it("returns true for HYPERLINK", () => {
    expect(isStringFilterType("HYPERLINK")).toBe(true);
  });

  it("returns true for UUID", () => {
    expect(isStringFilterType("UUID")).toBe(true);
  });

  it("returns true for AUTO_ID", () => {
    expect(isStringFilterType("AUTO_ID")).toBe(true);
  });

  it("returns false for ONTOLOGY (selectable, not string)", () => {
    expect(isStringFilterType("ONTOLOGY")).toBe(false);
  });

  it("returns false for INT", () => {
    expect(isStringFilterType("INT")).toBe(false);
  });

  it("returns false for REF (expandable, not string)", () => {
    expect(isStringFilterType("REF")).toBe(false);
  });

  it("returns false for BOOL", () => {
    expect(isStringFilterType("BOOL")).toBe(false);
  });
});

describe("isRefExpandable", () => {
  it("returns true for REF", () => {
    expect(isRefExpandable("REF")).toBe(true);
  });

  it("returns true for REF_ARRAY", () => {
    expect(isRefExpandable("REF_ARRAY")).toBe(true);
  });

  it("returns true for REFBACK", () => {
    expect(isRefExpandable("REFBACK")).toBe(true);
  });

  it("returns true for SELECT", () => {
    expect(isRefExpandable("SELECT")).toBe(true);
  });

  it("returns true for MULTISELECT", () => {
    expect(isRefExpandable("MULTISELECT")).toBe(true);
  });

  it("returns false for BOOL (selectable, not expandable)", () => {
    expect(isRefExpandable("BOOL")).toBe(false);
  });

  it("returns false for ONTOLOGY (selectable, not expandable)", () => {
    expect(isRefExpandable("ONTOLOGY")).toBe(false);
  });

  it("returns false for STRING (string type, not expandable)", () => {
    expect(isRefExpandable("STRING")).toBe(false);
  });

  it("returns false for INT", () => {
    expect(isRefExpandable("INT")).toBe(false);
  });
});

describe("navDepth", () => {
  it("returns 2 for REF", () => {
    expect(navDepth("REF")).toBe(2);
  });

  it("returns 2 for SELECT", () => {
    expect(navDepth("SELECT")).toBe(2);
  });

  it("returns 1 for REF_ARRAY", () => {
    expect(navDepth("REF_ARRAY")).toBe(1);
  });

  it("returns 1 for REFBACK", () => {
    expect(navDepth("REFBACK")).toBe(1);
  });

  it("returns 1 for MULTISELECT", () => {
    expect(navDepth("MULTISELECT")).toBe(1);
  });

  it("returns 1 for non-ref types (default fallback)", () => {
    expect(navDepth("ONTOLOGY")).toBe(1);
  });
});

describe("shouldExcludeSelfRef", () => {
  it("returns true when col.refTableId === parentTableId", () => {
    const col: IColumn = {
      id: "parent",
      label: "Parent",
      columnType: "REF",
      refTableId: "Resource",
      refSchemaId: "test",
    };
    expect(shouldExcludeSelfRef(col, "Resource")).toBe(true);
  });

  it("returns false when col.refTableId differs from parentTableId", () => {
    const col: IColumn = {
      id: "type",
      label: "Type",
      columnType: "REF",
      refTableId: "ResourceType",
      refSchemaId: "test",
    };
    expect(shouldExcludeSelfRef(col, "Resource")).toBe(false);
  });

  it("returns false when col has no refTableId", () => {
    const col: IColumn = {
      id: "name",
      label: "Name",
      columnType: "STRING",
    };
    expect(shouldExcludeSelfRef(col, "Resource")).toBe(false);
  });
});

describe("isExcludedColumn", () => {
  it("returns true for HEADING columns", () => {
    const col: IColumn = { id: "h1", label: "Demo", columnType: "HEADING" };
    expect(isExcludedColumn(col)).toBe(true);
  });

  it("returns true for SECTION columns", () => {
    const col: IColumn = { id: "s1", label: "Demo", columnType: "SECTION" };
    expect(isExcludedColumn(col)).toBe(true);
  });

  it("returns true for FILE columns", () => {
    const col: IColumn = { id: "f1", label: "Demo", columnType: "FILE" };
    expect(isExcludedColumn(col)).toBe(true);
  });

  it("returns true for mg_* named columns", () => {
    const col: IColumn = {
      id: "mg_insertedOn",
      label: "Inserted On",
      columnType: "DATETIME",
    };
    expect(isExcludedColumn(col)).toBe(true);
  });

  it("returns false for normal columns", () => {
    const col: IColumn = {
      id: "disease",
      label: "Disease",
      columnType: "ONTOLOGY",
    };
    expect(isExcludedColumn(col)).toBe(false);
  });

  it("returns false for STRING columns", () => {
    const col: IColumn = { id: "name", label: "Name", columnType: "STRING" };
    expect(isExcludedColumn(col)).toBe(false);
  });
});

import { describe, it, expect } from "vitest";
import { buildLabelMap } from "../../../app/composables/useFilters";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { CountedOption } from "../../../app/utils/fetchCounts";

const boolColumn: IColumn = { id: "active", columnType: "BOOL" };
const ontologyColumn: IColumn = { id: "type", columnType: "ONTOLOGY" };
const ontologyArrayColumn: IColumn = {
  id: "types",
  columnType: "ONTOLOGY_ARRAY",
};
const radioColumn: IColumn = { id: "status", columnType: "RADIO" };
const checkboxColumn: IColumn = { id: "tags", columnType: "CHECKBOX" };
const stringColumn: IColumn = { id: "name", columnType: "STRING" };

describe("buildLabelMap — BOOL", () => {
  it("returns BOOL_LABELS regardless of counted being undefined", () => {
    const map = buildLabelMap(boolColumn, undefined);
    expect(map["true"]).toBe("Yes");
    expect(map["false"]).toBe("No");
    expect(map["_null_"]).toBe("Not set");
  });

  it("returns BOOL_LABELS regardless of counted being null", () => {
    const map = buildLabelMap(boolColumn, null);
    expect(map["true"]).toBe("Yes");
    expect(map["false"]).toBe("No");
    expect(map["_null_"]).toBe("Not set");
  });

  it("returns BOOL_LABELS when counted is an empty array", () => {
    const map = buildLabelMap(boolColumn, []);
    expect(map).toEqual({ true: "Yes", false: "No", _null_: "Not set" });
  });
});

describe("buildLabelMap — ONTOLOGY flat", () => {
  it("maps name to label for flat nodes", () => {
    const counted: CountedOption[] = [
      { name: "ncit_C1", label: "Heart Disease", count: 5 },
      { name: "ncit_C2", label: "Lung Cancer", count: 3 },
    ];
    const map = buildLabelMap(ontologyColumn, counted);
    expect(map["ncit_C1"]).toBe("Heart Disease");
    expect(map["ncit_C2"]).toBe("Lung Cancer");
  });

  it("falls back to name when label is absent", () => {
    const counted: CountedOption[] = [{ name: "ncit_C1", count: 5 }];
    const map = buildLabelMap(ontologyColumn, counted);
    expect(map["ncit_C1"]).toBe("ncit_C1");
  });
});

describe("buildLabelMap — ONTOLOGY nested children", () => {
  it("flattens tree recursively collecting all descendants", () => {
    const counted: CountedOption[] = [
      {
        name: "root",
        label: "Root Term",
        count: 10,
        children: [
          {
            name: "child1",
            label: "Child One",
            count: 4,
            children: [{ name: "grandchild1", label: "Grand One", count: 2 }],
          },
          { name: "child2", label: "Child Two", count: 6 },
        ],
      },
    ];
    const map = buildLabelMap(ontologyColumn, counted);
    expect(map["root"]).toBe("Root Term");
    expect(map["child1"]).toBe("Child One");
    expect(map["child2"]).toBe("Child Two");
    expect(map["grandchild1"]).toBe("Grand One");
    expect(Object.keys(map)).toHaveLength(4);
  });
});

describe("buildLabelMap — ONTOLOGY_ARRAY", () => {
  it("behaves identically to ONTOLOGY for flat nodes", () => {
    const counted: CountedOption[] = [
      { name: "term1", label: "Term One", count: 2 },
    ];
    const map = buildLabelMap(ontologyArrayColumn, counted);
    expect(map["term1"]).toBe("Term One");
  });
});

describe("buildLabelMap — RADIO flat (single-key)", () => {
  it("maps name to label for options without keyObject", () => {
    const counted: CountedOption[] = [
      { name: "active", label: "Active", count: 5 },
      { name: "inactive", label: "Inactive", count: 3 },
    ];
    const map = buildLabelMap(radioColumn, counted);
    expect(map["active"]).toBe("Active");
    expect(map["inactive"]).toBe("Inactive");
  });

  it("falls back to name when label absent", () => {
    const counted: CountedOption[] = [{ name: "active", count: 5 }];
    const map = buildLabelMap(radioColumn, counted);
    expect(map["active"]).toBe("active");
  });
});

describe("buildLabelMap — CHECKBOX composite-key", () => {
  it("uses JSON.stringify(keyObject) as map key, name as display value", () => {
    const keyObj = { id: "A", code: "1" };
    const counted: CountedOption[] = [
      { name: "Option A-1", count: 3, keyObject: keyObj },
    ];
    const map = buildLabelMap(checkboxColumn, counted);
    expect(map[JSON.stringify(keyObj)]).toBe("Option A-1");
  });

  it("handles multiple composite-key options", () => {
    const key1 = { id: "A", code: "1" };
    const key2 = { id: "B", code: "2" };
    const counted: CountedOption[] = [
      { name: "Option A", count: 2, keyObject: key1 },
      { name: "Option B", count: 1, keyObject: key2 },
    ];
    const map = buildLabelMap(checkboxColumn, counted);
    expect(map[JSON.stringify(key1)]).toBe("Option A");
    expect(map[JSON.stringify(key2)]).toBe("Option B");
  });
});

describe("buildLabelMap — STRING and other types", () => {
  it("returns empty map for STRING", () => {
    const counted: CountedOption[] = [{ name: "foo", count: 1 }];
    expect(buildLabelMap(stringColumn, counted)).toEqual({});
  });

  it("returns empty map when counted is undefined for STRING", () => {
    expect(buildLabelMap(stringColumn, undefined)).toEqual({});
  });

  it("returns empty map when counted is null for non-BOOL types", () => {
    expect(buildLabelMap(ontologyColumn, null)).toEqual({});
  });

  it("returns empty map when counted is empty for non-BOOL types", () => {
    expect(buildLabelMap(ontologyColumn, [])).toEqual({});
  });
});

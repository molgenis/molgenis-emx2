import { describe, it, expect } from "vitest";
import {
  isEmptyValue,
  isTopSection,
  buildRefbackFilter,
  getRowLabel,
  filterDataColumns,
  filterNonEmptyColumns,
  filterColumnsByRole,
  getListColumns,
  isRefColumn,
  isRefArrayColumn,
  buildRefHref,
  getDetailColumns,
  getDescriptionColumn,
  getLogoColumn,
  getRoleText,
  getTitleText,
  getSubtitleText,
  hasOntologyHierarchy,
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
    expect(
      buildRefbackFilter("REFBACK", "resource", undefined)
    ).toBeUndefined();
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
    expect(result.map((c) => c.id)).toEqual([
      "name",
      "description",
      "resource",
    ]);
  });

  it("also hides columns in hideColumns list", () => {
    const result = filterDataColumns(columns, ["resource"]);
    expect(result.map((c) => c.id)).toEqual(["name", "description"]);
  });

  it("returns all data columns when hideColumns is empty", () => {
    const result = filterDataColumns(columns, []);
    expect(result.map((c) => c.id)).toEqual([
      "name",
      "description",
      "resource",
    ]);
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

describe("isRefColumn", () => {
  it("returns true for REF", () => {
    expect(isRefColumn("REF")).toBe(true);
  });

  it("returns true for SELECT", () => {
    expect(isRefColumn("SELECT")).toBe(true);
  });

  it("returns true for RADIO", () => {
    expect(isRefColumn("RADIO")).toBe(true);
  });

  it("returns false for REF_ARRAY", () => {
    expect(isRefColumn("REF_ARRAY")).toBe(false);
  });

  it("returns false for REFBACK", () => {
    expect(isRefColumn("REFBACK")).toBe(false);
  });

  it("returns false for ONTOLOGY", () => {
    expect(isRefColumn("ONTOLOGY")).toBe(false);
  });

  it("returns false for STRING", () => {
    expect(isRefColumn("STRING")).toBe(false);
  });
});

describe("isRefArrayColumn", () => {
  it("returns true for REF_ARRAY", () => {
    expect(isRefArrayColumn("REF_ARRAY")).toBe(true);
  });

  it("returns true for MULTISELECT", () => {
    expect(isRefArrayColumn("MULTISELECT")).toBe(true);
  });

  it("returns true for CHECKBOX", () => {
    expect(isRefArrayColumn("CHECKBOX")).toBe(true);
  });

  it("returns false for REF", () => {
    expect(isRefArrayColumn("REF")).toBe(false);
  });

  it("returns false for REFBACK", () => {
    expect(isRefArrayColumn("REFBACK")).toBe(false);
  });

  it("returns false for STRING", () => {
    expect(isRefArrayColumn("STRING")).toBe(false);
  });

  it("returns false for ONTOLOGY", () => {
    expect(isRefArrayColumn("ONTOLOGY")).toBe(false);
  });
});

describe("buildRefHref", () => {
  it("builds href with single key value", () => {
    const href = buildRefHref("myschema", "Resources", undefined, {
      id: "ALSPAC",
    });
    expect(href).toBe(
      `/myschema/Resources/${encodeURIComponent(
        "ALSPAC"
      )}?keys=${encodeURIComponent('{"id":"ALSPAC"}')}`
    );
  });

  it("builds href with composite key", () => {
    const href = buildRefHref("myschema", "Datasets", undefined, {
      resource: "ALSPAC",
      name: "core",
    });
    expect(href).toContain("/myschema/Datasets/");
    expect(href).toContain("ALSPAC-core");
    expect(href).toContain(
      encodeURIComponent('{"resource":"ALSPAC","name":"core"}')
    );
  });

  it("uses refSchemaId when provided", () => {
    const href = buildRefHref("myschema", "Resources", "otherschema", {
      id: "X",
    });
    expect(href.startsWith("/otherschema/Resources/")).toBe(true);
  });

  it("falls back to schemaId when refSchemaId is undefined", () => {
    const href = buildRefHref("myschema", "Resources", undefined, {
      id: "X",
    });
    expect(href.startsWith("/myschema/Resources/")).toBe(true);
  });

  it("handles nested key objects in slug", () => {
    const href = buildRefHref("s", "T", undefined, {
      id: "A",
      nested: { foo: "bar" },
    });
    expect(href).toContain("/s/T/A?");
  });
});

describe("getDetailColumns", () => {
  it("returns columns with role DETAIL when any exist", () => {
    const columns = [
      col({ id: "name", columnType: "STRING" }),
      col({ id: "summary", columnType: "TEXT", role: "DETAIL" }),
      col({ id: "tags", columnType: "STRING", role: "DETAIL" }),
    ];
    const data = { name: "Alice", summary: "Bio text", tags: "tag1" };
    const result = getDetailColumns(columns, data);
    expect(result.map((c) => c.id)).toEqual(["summary", "tags"]);
  });

  it("excludes DETAIL columns with null data", () => {
    const columns = [
      col({ id: "summary", columnType: "TEXT", role: "DETAIL" }),
      col({ id: "tags", columnType: "STRING", role: "DETAIL" }),
    ];
    const data = { summary: null, tags: "tag1" };
    const result = getDetailColumns(columns, data);
    expect(result.map((c) => c.id)).toEqual(["tags"]);
  });

  it("falls back to non-key, non-role, non-heading columns when no DETAIL roles", () => {
    const columns = [
      col({ id: "name", columnType: "STRING", key: 1 }),
      col({ id: "description", columnType: "TEXT" }),
      col({ id: "status", columnType: "STRING" }),
    ];
    const data = { name: "Alice", description: "A desc", status: "active" };
    const result = getDetailColumns(columns, data);
    expect(result.map((c) => c.id)).toEqual(["description", "status"]);
  });

  it("excludes TITLE, DESCRIPTION, LOGO roles from fallback", () => {
    const columns = [
      col({ id: "title", columnType: "STRING", role: "TITLE" }),
      col({ id: "desc", columnType: "TEXT", role: "DESCRIPTION" }),
      col({ id: "logo", columnType: "FILE", role: "LOGO" }),
      col({ id: "status", columnType: "STRING" }),
    ];
    const data = {
      title: "My Title",
      desc: "Some desc",
      logo: { url: "/img.png" },
      status: "active",
    };
    const result = getDetailColumns(columns, data);
    expect(result.map((c) => c.id)).toEqual(["status"]);
  });

  it("caps fallback at 5 columns", () => {
    const columns = Array.from({ length: 8 }, (_, i) =>
      col({ id: `field${i}`, columnType: "STRING" })
    );
    const data = Object.fromEntries(columns.map((c) => [c.id, `val${c.id}`]));
    const result = getDetailColumns(columns, data);
    expect(result).toHaveLength(5);
  });

  it("excludes columns with null or undefined data from fallback", () => {
    const columns = [
      col({ id: "name", columnType: "STRING" }),
      col({ id: "description", columnType: "TEXT" }),
      col({ id: "status", columnType: "STRING" }),
    ];
    const data = { name: "Alice", description: null, status: undefined };
    const result = getDetailColumns(columns, data);
    expect(result.map((c) => c.id)).toEqual(["name"]);
  });

  it("excludes HEADING and SECTION column types from fallback", () => {
    const columns = [
      col({ id: "section1", columnType: "HEADING" }),
      col({ id: "section2", columnType: "SECTION" }),
      col({ id: "name", columnType: "STRING" }),
    ];
    const data = { section1: "x", section2: "y", name: "Alice" };
    const result = getDetailColumns(columns, data);
    expect(result.map((c) => c.id)).toEqual(["name"]);
  });

  it("excludes mg_ prefixed columns from fallback", () => {
    const columns = [
      col({ id: "mg_insertedOn", columnType: "DATETIME" }),
      col({ id: "name", columnType: "STRING" }),
    ];
    const data = { mg_insertedOn: "2024-01-01", name: "Alice" };
    const result = getDetailColumns(columns, data);
    expect(result.map((c) => c.id)).toEqual(["name"]);
  });

  it("returns empty array when no columns provided", () => {
    const result = getDetailColumns([], {});
    expect(result).toEqual([]);
  });

  it("prefers explicit DETAIL over fallback even when only one DETAIL column", () => {
    const columns = [
      col({ id: "summary", columnType: "TEXT", role: "DETAIL" }),
      col({ id: "status", columnType: "STRING" }),
      col({ id: "country", columnType: "STRING" }),
    ];
    const data = { summary: "Bio", status: "active", country: "NL" };
    const result = getDetailColumns(columns, data);
    expect(result.map((c) => c.id)).toEqual(["summary"]);
  });
});

describe("getDescriptionColumn", () => {
  it("returns column with DESCRIPTION role that has data", () => {
    const columns = [
      col({ id: "name", columnType: "STRING" }),
      col({ id: "bio", columnType: "TEXT", role: "DESCRIPTION" }),
    ];
    const data = { name: "Alice", bio: "A biography" };
    const result = getDescriptionColumn(columns, data);
    expect(result?.id).toBe("bio");
  });

  it("returns undefined when DESCRIPTION column has null data", () => {
    const columns = [
      col({ id: "bio", columnType: "TEXT", role: "DESCRIPTION" }),
    ];
    const data = { bio: null };
    const result = getDescriptionColumn(columns, data);
    expect(result).toBeUndefined();
  });

  it("returns undefined when no DESCRIPTION role column exists", () => {
    const columns = [col({ id: "name", columnType: "STRING" })];
    const data = { name: "Alice" };
    const result = getDescriptionColumn(columns, data);
    expect(result).toBeUndefined();
  });
});

describe("getLogoColumn", () => {
  it("returns column with LOGO role that has data", () => {
    const columns = [
      col({ id: "name", columnType: "STRING" }),
      col({ id: "logo", columnType: "FILE", role: "LOGO" }),
    ];
    const data = { name: "Alice", logo: { url: "/img.png" } };
    const result = getLogoColumn(columns, data);
    expect(result?.id).toBe("logo");
  });

  it("returns undefined when LOGO column has null data", () => {
    const columns = [col({ id: "logo", columnType: "FILE", role: "LOGO" })];
    const data = { logo: null };
    const result = getLogoColumn(columns, data);
    expect(result).toBeUndefined();
  });

  it("returns undefined when no LOGO role column exists", () => {
    const columns = [col({ id: "name", columnType: "STRING" })];
    const data = { name: "Alice" };
    const result = getLogoColumn(columns, data);
    expect(result).toBeUndefined();
  });
});

describe("getRoleText", () => {
  it("returns empty string for null", () => {
    expect(getRoleText(null)).toBe("");
  });

  it("returns empty string for undefined", () => {
    expect(getRoleText(undefined)).toBe("");
  });

  it("returns string values directly", () => {
    expect(getRoleText("hello")).toBe("hello");
  });

  it("returns name from object with name property", () => {
    expect(getRoleText({ name: "Amsterdam" })).toBe("Amsterdam");
  });

  it("joins array items by name", () => {
    expect(getRoleText([{ name: "A" }, { name: "B" }])).toBe("A, B");
  });

  it("joins non-name object values", () => {
    expect(getRoleText({ other: "x" })).toBe("x");
  });
});

describe("getTitleText", () => {
  it("joins values from TITLE role columns with space", () => {
    const columns = [
      col({ id: "firstName", columnType: "STRING", role: "TITLE" }),
      col({ id: "lastName", columnType: "STRING", role: "TITLE" }),
    ];
    const data = { firstName: "John", lastName: "Doe" };
    expect(getTitleText(columns, data)).toBe("John Doe");
  });

  it("returns empty string when no TITLE columns", () => {
    const columns = [col({ id: "name", columnType: "STRING" })];
    const data = { name: "Alice" };
    expect(getTitleText(columns, data)).toBe("");
  });

  it("skips empty values", () => {
    const columns = [
      col({ id: "name", columnType: "STRING", role: "TITLE" }),
      col({ id: "suffix", columnType: "STRING", role: "TITLE" }),
    ];
    const data = { name: "Alice", suffix: null };
    expect(getTitleText(columns, data)).toBe("Alice");
  });

  it("extracts name from object value", () => {
    const columns = [col({ id: "org", columnType: "REF", role: "TITLE" })];
    const data = { org: { name: "UMCG" } };
    expect(getTitleText(columns, data)).toBe("UMCG");
  });
});

describe("filterColumnsByRole", () => {
  it("returns all non-INTERNAL columns when no roles are set", () => {
    const columns = [
      col({ id: "name", columnType: "STRING" }),
      col({ id: "description", columnType: "TEXT" }),
    ];
    const result = filterColumnsByRole(columns);
    expect(result.map((c) => c.id)).toEqual(["name", "description"]);
  });

  it("excludes INTERNAL columns even when no other roles are set", () => {
    const columns = [
      col({ id: "name", columnType: "STRING" }),
      col({ id: "internal", columnType: "STRING", role: "INTERNAL" }),
    ];
    const result = filterColumnsByRole(columns);
    expect(result.map((c) => c.id)).toEqual(["name"]);
  });

  it("returns only role-configured columns when roles are present", () => {
    const columns = [
      col({ id: "name", columnType: "STRING", role: "TITLE" }),
      col({ id: "description", columnType: "TEXT" }),
      col({ id: "status", columnType: "STRING", role: "DETAIL" }),
    ];
    const result = filterColumnsByRole(columns);
    expect(result.map((c) => c.id)).toEqual(["name", "status"]);
  });

  it("excludes INTERNAL columns even when other roles exist", () => {
    const columns = [
      col({ id: "name", columnType: "STRING", role: "TITLE" }),
      col({ id: "internal", columnType: "STRING", role: "INTERNAL" }),
      col({ id: "status", columnType: "STRING", role: "DETAIL" }),
    ];
    const result = filterColumnsByRole(columns);
    expect(result.map((c) => c.id)).toEqual(["name", "status"]);
  });

  it("puts TITLE columns first when roles are present", () => {
    const columns = [
      col({ id: "status", columnType: "STRING", role: "DETAIL" }),
      col({ id: "name", columnType: "STRING", role: "TITLE" }),
      col({ id: "description", columnType: "TEXT", role: "DESCRIPTION" }),
    ];
    const result = filterColumnsByRole(columns);
    expect(result[0].id).toBe("name");
    expect(result.map((c) => c.id)).toContain("status");
    expect(result.map((c) => c.id)).toContain("description");
  });

  it("returns empty array for empty input", () => {
    expect(filterColumnsByRole([])).toEqual([]);
  });
});

describe("hasOntologyHierarchy", () => {
  it("returns false for non-array values", () => {
    expect(hasOntologyHierarchy(null)).toBe(false);
    expect(hasOntologyHierarchy(undefined)).toBe(false);
    expect(hasOntologyHierarchy("string")).toBe(false);
    expect(hasOntologyHierarchy({ name: "term" })).toBe(false);
  });

  it("returns false for empty array", () => {
    expect(hasOntologyHierarchy([])).toBe(false);
  });

  it("returns false when no items have parent", () => {
    expect(hasOntologyHierarchy([{ name: "A" }, { name: "B" }])).toBe(false);
  });

  it("returns false when all parent values are null", () => {
    expect(
      hasOntologyHierarchy([
        { name: "A", parent: null },
        { name: "B", parent: null },
      ])
    ).toBe(false);
  });

  it("returns true when at least one item has a non-null parent", () => {
    expect(
      hasOntologyHierarchy([
        { name: "A", parent: null },
        { name: "B", parent: { name: "A" } },
      ])
    ).toBe(true);
  });

  it("returns true when parent is a string", () => {
    expect(hasOntologyHierarchy([{ name: "B", parent: "A" }])).toBe(true);
  });
});

describe("getSubtitleText", () => {
  it("joins values from SUBTITLE role columns with space", () => {
    const columns = [
      col({ id: "type", columnType: "STRING", role: "SUBTITLE" }),
      col({ id: "year", columnType: "STRING", role: "SUBTITLE" }),
    ];
    const data = { type: "Cohort", year: "2020" };
    expect(getSubtitleText(columns, data)).toBe("Cohort 2020");
  });

  it("returns empty string when no SUBTITLE columns", () => {
    const columns = [col({ id: "name", columnType: "STRING" })];
    const data = { name: "Alice" };
    expect(getSubtitleText(columns, data)).toBe("");
  });

  it("skips null values", () => {
    const columns = [
      col({ id: "type", columnType: "STRING", role: "SUBTITLE" }),
    ];
    const data = { type: null };
    expect(getSubtitleText(columns, data)).toBe("");
  });
});

describe("getListColumns", () => {
  const columns = [
    col({ id: "mg_top", columnType: "SECTION" }),
    col({ id: "overview", columnType: "HEADING" }),
    col({ id: "mg_insertedOn", columnType: "DATETIME" }),
    col({ id: "rdf", columnType: "STRING", role: "INTERNAL" }),
    col({ id: "id", columnType: "STRING", key: 1 }),
    col({ id: "name", columnType: "STRING", role: "TITLE" }),
    col({ id: "description", columnType: "TEXT", role: "DESCRIPTION" }),
    col({ id: "type", columnType: "STRING", role: "DETAIL" }),
    col({ id: "status", columnType: "STRING" }),
    col({ id: "country", columnType: "STRING" }),
    col({ id: "extra1", columnType: "STRING" }),
    col({ id: "extra2", columnType: "STRING" }),
    col({ id: "extra3", columnType: "STRING" }),
  ];

  it("removes structural, system, and INTERNAL columns by default", () => {
    const result = getListColumns(columns);
    const ids = result.map((c) => c.id);
    expect(ids).not.toContain("mg_top");
    expect(ids).not.toContain("overview");
    expect(ids).not.toContain("mg_insertedOn");
    expect(ids).not.toContain("rdf");
    expect(ids).toContain("id");
    expect(ids).toContain("name");
  });

  it("respects hideColumns", () => {
    const result = getListColumns(columns, { hideColumns: ["status"] });
    expect(result.map((c) => c.id)).not.toContain("status");
  });

  it("respects visibleColumns and preserves order", () => {
    const result = getListColumns(columns, {
      visibleColumns: ["description", "name"],
    });
    expect(result.map((c) => c.id)).toEqual(["description", "name"]);
  });

  it("for CARDS layout with roles, keeps only role columns with TITLE first", () => {
    const result = getListColumns(columns, { layout: "CARDS" });
    const ids = result.map((c) => c.id);
    expect(ids[0]).toBe("name");
    expect(ids).toContain("description");
    expect(ids).toContain("type");
    expect(ids).not.toContain("status");
    expect(ids).not.toContain("country");
  });

  it("for CARDS layout without roles, falls back to key + first 5 non-key columns", () => {
    const noRoleCols = [
      col({ id: "mg_top", columnType: "SECTION" }),
      col({ id: "id", columnType: "STRING", key: 1 }),
      col({ id: "a", columnType: "STRING" }),
      col({ id: "b", columnType: "STRING" }),
      col({ id: "c", columnType: "STRING" }),
      col({ id: "d", columnType: "STRING" }),
      col({ id: "e", columnType: "STRING" }),
      col({ id: "f", columnType: "STRING" }),
    ];
    const result = getListColumns(noRoleCols, { layout: "CARDS" });
    expect(result).toHaveLength(5);
    expect(result[0].id).toBe("id");
  });

  it("for TABLE layout with roles, applies role-based selection with max 5 columns", () => {
    const result = getListColumns(columns, { layout: "TABLE" });
    const ids = result.map((c) => c.id);
    expect(ids[0]).toBe("name");
    expect(ids).toContain("description");
    expect(ids).toContain("type");
    expect(result).toHaveLength(5);
    expect(ids).toContain("status");
    expect(ids).toContain("country");
  });

  it("removes all-empty columns when rows provided", () => {
    const result = getListColumns(columns, {
      rows: [
        {
          id: "1",
          name: "A",
          description: null,
          type: "",
          status: null,
          country: null,
          extra1: null,
          extra2: null,
          extra3: null,
        },
        {
          id: "2",
          name: "B",
          description: null,
          type: "",
          status: null,
          country: null,
          extra1: null,
          extra2: null,
          extra3: null,
        },
      ],
    });
    expect(result.map((c) => c.id)).toEqual(["id", "name"]);
  });

  it("returns empty array for empty input", () => {
    expect(getListColumns([])).toEqual([]);
  });

  it("for TABLE layout without roles, limits to key columns plus up to 4 others (max 5 total)", () => {
    const noRoleCols = [
      col({ id: "mg_top", columnType: "SECTION" }),
      col({ id: "id", columnType: "STRING", key: 1 }),
      col({ id: "a", columnType: "STRING" }),
      col({ id: "b", columnType: "STRING" }),
      col({ id: "c", columnType: "STRING" }),
      col({ id: "d", columnType: "STRING" }),
      col({ id: "e", columnType: "STRING" }),
      col({ id: "f", columnType: "STRING" }),
    ];
    const result = getListColumns(noRoleCols, { layout: "TABLE" });
    expect(result).toHaveLength(5);
    expect(result[0].id).toBe("id");
  });

  it("multiple TITLE columns count as 1 toward the 5-column limit", () => {
    const manyTitleCols = [
      col({ id: "firstName", columnType: "STRING", role: "TITLE" }),
      col({ id: "lastName", columnType: "STRING", role: "TITLE" }),
      col({ id: "detail1", columnType: "STRING", role: "DETAIL" }),
      col({ id: "detail2", columnType: "STRING", role: "DETAIL" }),
      col({ id: "detail3", columnType: "STRING", role: "DETAIL" }),
      col({ id: "detail4", columnType: "STRING", role: "DETAIL" }),
    ];
    const result = getListColumns(manyTitleCols, { layout: "CARDS" });
    expect(result).toHaveLength(5);
    expect(result[0].id).toBe("firstName");
    expect(result[1].id).toBe("lastName");
  });

  it("DESCRIPTION column appears after TITLE but before DETAIL columns", () => {
    const orderedCols = [
      col({ id: "detail1", columnType: "STRING", role: "DETAIL" }),
      col({ id: "bio", columnType: "TEXT", role: "DESCRIPTION" }),
      col({ id: "title1", columnType: "STRING", role: "TITLE" }),
    ];
    const result = getListColumns(orderedCols, { layout: "CARDS" });
    const ids = result.map((c) => c.id);
    expect(ids.indexOf("title1")).toBeLessThan(ids.indexOf("bio"));
    expect(ids.indexOf("bio")).toBeLessThan(ids.indexOf("detail1"));
  });

  it("without DETAIL roles, fills remaining slots with other non-title, non-description, non-key, non-system columns", () => {
    const fallbackCols = [
      col({ id: "name", columnType: "STRING", role: "TITLE" }),
      col({ id: "a", columnType: "STRING" }),
      col({ id: "b", columnType: "STRING" }),
      col({ id: "c", columnType: "STRING" }),
      col({ id: "d", columnType: "STRING" }),
      col({ id: "e", columnType: "STRING" }),
    ];
    const result = getListColumns(fallbackCols, { layout: "CARDS" });
    const ids = result.map((c) => c.id);
    expect(ids[0]).toBe("name");
    expect(result).toHaveLength(5);
    expect(ids).toContain("a");
    expect(ids).toContain("b");
    expect(ids).toContain("c");
    expect(ids).toContain("d");
    expect(ids).not.toContain("e");
  });
});

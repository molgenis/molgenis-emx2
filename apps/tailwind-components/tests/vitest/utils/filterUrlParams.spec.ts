import { describe, it, expect } from "vitest";
import {
  serializeFilterValue,
  parseFilterValue,
  serializeFiltersToUrl,
  parseFiltersFromUrl,
} from "../../../app/utils/filterUrlParams";
import { buildGraphQLFilter } from "../../../app/utils/buildFilter";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";

describe("serializeFilterValue", () => {
  it("serializes like operator for string", () => {
    const result = serializeFilterValue({ operator: "like", value: "John" });
    expect(result).toBe("John");
  });

  it("serializes between operator for int", () => {
    const result = serializeFilterValue({
      operator: "between",
      value: [10, 20],
    });
    expect(result).toBe("10..20");
  });

  it("serializes between with only min", () => {
    const result = serializeFilterValue({
      operator: "between",
      value: [10, null],
    });
    expect(result).toBe("10..");
  });

  it("serializes between with only max", () => {
    const result = serializeFilterValue({
      operator: "between",
      value: [null, 20],
    });
    expect(result).toBe("..20");
  });

  it("returns null for empty between", () => {
    const result = serializeFilterValue({
      operator: "between",
      value: [null, null],
    });
    expect(result).toBeNull();
  });

  it("serializes equals operator with simple values using pipe", () => {
    const result = serializeFilterValue({
      operator: "equals",
      value: ["a", "b", "c"],
    });
    expect(result).toBe("a|b|c");
  });

  it("serializes equals operator with ref objects as pipe-separated keys", () => {
    const result = serializeFilterValue({
      operator: "equals",
      value: [{ name: "Cat1" }, { name: "Cat2" }],
    });
    expect(result).toBe("Cat1|Cat2");
  });

  it("serializes notNull operator", () => {
    const result = serializeFilterValue({ operator: "notNull", value: true });
    expect(result).toBe("!null");
  });

  it("serializes isNull operator", () => {
    const result = serializeFilterValue({ operator: "isNull", value: true });
    expect(result).toBe("null");
  });

  it("serializes date range", () => {
    const result = serializeFilterValue({
      operator: "between",
      value: ["2024-01-01", "2024-12-31"],
    });
    expect(result).toBe("2024-01-01..2024-12-31");
  });
});

describe("parseFilterValue", () => {
  const stringColumn: IColumn = { id: "name", columnType: "STRING" };
  const intColumn: IColumn = { id: "age", columnType: "INT" };
  const dateColumn: IColumn = { id: "birth", columnType: "DATE" };
  const refColumn: IColumn = { id: "category", columnType: "REF" };
  const uuidColumn: IColumn = { id: "id", columnType: "UUID" };

  it("parses simple string as like", () => {
    const result = parseFilterValue("John", stringColumn);
    expect(result).toEqual({ operator: "like", value: "John" });
  });

  it("passes through raw string value for string types", () => {
    const result = parseFilterValue("a|b|c", stringColumn);
    expect(result).toEqual({ operator: "like", value: "a|b|c" });
  });

  it("parses range for int", () => {
    const result = parseFilterValue("10..20", intColumn);
    expect(result).toEqual({ operator: "between", value: [10, 20] });
  });

  it("parses range with only min for int", () => {
    const result = parseFilterValue("10..", intColumn);
    expect(result).toEqual({ operator: "between", value: [10, null] });
  });

  it("parses range with only max for int", () => {
    const result = parseFilterValue("..20", intColumn);
    expect(result).toEqual({ operator: "between", value: [null, 20] });
  });

  it("parses single int as equals", () => {
    const result = parseFilterValue("25", intColumn);
    expect(result).toEqual({ operator: "equals", value: 25 });
  });

  it("parses date range", () => {
    const result = parseFilterValue("2024-01-01..2024-12-31", dateColumn);
    expect(result).toEqual({
      operator: "between",
      value: ["2024-01-01", "2024-12-31"],
    });
  });

  it("parses pipe-separated ref values as objects", () => {
    const result = parseFilterValue("Cat1|Cat2", refColumn);
    expect(result).toEqual({
      operator: "equals",
      value: [{ name: "Cat1" }, { name: "Cat2" }],
    });
  });

  it("parses simple ref value as array", () => {
    const result = parseFilterValue("Cat1", refColumn);
    expect(result).toEqual({ operator: "equals", value: [{ name: "Cat1" }] });
  });

  it("parses ref value with custom field as array", () => {
    const result = parseFilterValue("123", refColumn, "id");
    expect(result).toEqual({ operator: "equals", value: [{ id: "123" }] });
  });

  it("parses pipe-separated ref values with custom field", () => {
    const result = parseFilterValue("123|456", refColumn, "id");
    expect(result).toEqual({
      operator: "equals",
      value: [{ id: "123" }, { id: "456" }],
    });
  });

  it("parses null", () => {
    const result = parseFilterValue("null", stringColumn);
    expect(result).toEqual({ operator: "isNull", value: true });
  });

  it("parses !null", () => {
    const result = parseFilterValue("!null", stringColumn);
    expect(result).toEqual({ operator: "notNull", value: true });
  });

  it("returns null for empty value", () => {
    const result = parseFilterValue("", stringColumn);
    expect(result).toBeNull();
  });

  it("parses UUID filter as equals", () => {
    const result = parseFilterValue(
      "550e8400-e29b-41d4-a716-446655440000",
      uuidColumn
    );
    expect(result).toEqual({
      operator: "equals",
      value: "550e8400-e29b-41d4-a716-446655440000",
    });
  });

  it("parses ontology value as plain string array (not object array)", () => {
    const ontologyColumn: IColumn = { id: "tags", columnType: "ONTOLOGY" };
    const result = parseFilterValue("Adult", ontologyColumn);
    expect(result).toEqual({ operator: "equals", value: ["Adult"] });
  });

  it("parses pipe-separated ontology values as plain string array", () => {
    const ontologyColumn: IColumn = { id: "tags", columnType: "ONTOLOGY" };
    const result = parseFilterValue("Adult|Child", ontologyColumn);
    expect(result).toEqual({ operator: "equals", value: ["Adult", "Child"] });
  });

  it("parses ONTOLOGY_ARRAY value as plain string array", () => {
    const ontologyArrayColumn: IColumn = {
      id: "tags",
      columnType: "ONTOLOGY_ARRAY",
    };
    const result = parseFilterValue("Adult|Child", ontologyArrayColumn);
    expect(result).toEqual({ operator: "equals", value: ["Adult", "Child"] });
  });
});

describe("serializeFiltersToUrl", () => {
  const columns: IColumn[] = [
    { id: "name", columnType: "STRING" },
    { id: "age", columnType: "INT" },
    { id: "category", columnType: "REF" },
  ];

  it("returns empty object for empty state", () => {
    const result = serializeFiltersToUrl(new Map(), "", columns);
    expect(result).toEqual({});
  });

  it("serializes search with mg_search key", () => {
    const result = serializeFiltersToUrl(new Map(), "test search", columns);
    expect(result).toEqual({ mg_search: "test search" });
  });

  it("serializes filters", () => {
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "John" }],
      ["age", { operator: "between", value: [18, 65] }],
    ]);
    const result = serializeFiltersToUrl(filters, "", columns);
    expect(result).toEqual({ name: "John", age: "18..65" });
  });

  it("serializes both search and filters", () => {
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "John" }],
    ]);
    const result = serializeFiltersToUrl(filters, "test", columns);
    expect(result).toEqual({ mg_search: "test", name: "John" });
  });

  it("skips unknown columns", () => {
    const filters = new Map<string, IFilterValue>([
      ["unknown", { operator: "like", value: "test" }],
    ]);
    const result = serializeFiltersToUrl(filters, "", columns);
    expect(result).toEqual({});
  });

  it("serializes ref filters with dotted key syntax", () => {
    const filters = new Map<string, IFilterValue>([
      [
        "category",
        { operator: "equals", value: [{ name: "Cat1" }, { name: "Cat2" }] },
      ],
    ]);
    const result = serializeFiltersToUrl(filters, "", columns);
    expect(result).toEqual({ "category.name": "Cat1|Cat2" });
  });

  it("serializes ref filters with non-name field", () => {
    const filters = new Map<string, IFilterValue>([
      ["category", { operator: "equals", value: { id: "123" } }],
    ]);
    const result = serializeFiltersToUrl(filters, "", columns);
    expect(result).toEqual({ "category.id": "123" });
  });

  it("serializes 3-level nested ref filter to URL", () => {
    const columns = [
      { id: "order", columnType: "REF", label: "Order", refTableId: "Order" },
    ];
    const filters = new Map();
    filters.set("order.pet.category", {
      operator: "equals",
      value: { name: "dogs" },
    });
    const result = serializeFiltersToUrl(filters, "", columns);
    expect(result["order.pet.category.name"]).toBe("dogs");
  });

  it("serializes nested ontology filter (plain string values) with .name suffix", () => {
    const columns = [
      {
        id: "collectionEvents",
        columnType: "REF_ARRAY",
        label: "Collection Events",
        refTableId: "CollectionEvent",
      },
    ];
    const filters = new Map();
    filters.set("collectionEvents.ageGroups", {
      operator: "equals",
      value: ["Adult", "Child"],
    });
    const result = serializeFiltersToUrl(filters, "", columns);
    expect(result["collectionEvents.ageGroups.name"]).toBe("Adult|Child");
  });
});

describe("parseFiltersFromUrl", () => {
  const columns: IColumn[] = [
    { id: "name", columnType: "STRING" },
    { id: "age", columnType: "INT" },
    { id: "category", columnType: "REF" },
  ];

  it("returns empty state for empty query", () => {
    const result = parseFiltersFromUrl({}, columns);
    expect(result.filters.size).toBe(0);
    expect(result.search).toBe("");
  });

  it("parses search from mg_search", () => {
    const result = parseFiltersFromUrl({ mg_search: "test" }, columns);
    expect(result.search).toBe("test");
    expect(result.filters.size).toBe(0);
  });

  it("parses filters", () => {
    const result = parseFiltersFromUrl(
      { name: "John", age: "18..65" },
      columns
    );
    expect(result.filters.size).toBe(2);
    expect(result.filters.get("name")).toEqual({
      operator: "like",
      value: "John",
    });
    expect(result.filters.get("age")).toEqual({
      operator: "between",
      value: [18, 65],
    });
  });

  it("skips reserved params (mg_*) except mg_search", () => {
    const result = parseFiltersFromUrl(
      { mg_search: "test", mg_page: "2", mg_limit: "10" },
      columns
    );
    expect(result.search).toBe("test");
    expect(result.filters.size).toBe(0);
  });

  it("skips unknown columns", () => {
    const result = parseFiltersFromUrl({ unknown: "value" }, columns);
    expect(result.filters.size).toBe(0);
  });

  it("parses ref filters with dotted key syntax", () => {
    const result = parseFiltersFromUrl(
      { "category.name": "Cat1|Cat2" },
      columns
    );
    expect(result.filters.get("category")).toEqual({
      operator: "equals",
      value: [{ name: "Cat1" }, { name: "Cat2" }],
    });
  });

  it("parses ref filters with non-name field", () => {
    const result = parseFiltersFromUrl({ "category.id": "123" }, columns);
    expect(result.filters.get("category")).toEqual({
      operator: "equals",
      value: [{ id: "123" }],
    });
  });

  it("parses ref filters with backward compatibility (no dot)", () => {
    const result = parseFiltersFromUrl({ category: "Cat1|Cat2" }, columns);
    expect(result.filters.get("category")).toEqual({
      operator: "equals",
      value: [{ name: "Cat1" }, { name: "Cat2" }],
    });
  });

  it("parses 3-level nested ref filter from URL", () => {
    const columns = [
      { id: "order", columnType: "REF", label: "Order", refTableId: "Order" },
    ];
    const result = parseFiltersFromUrl(
      { "order.pet.category.name": "dogs" },
      columns
    );
    expect(result.filters.get("order.pet.category")).toEqual({
      operator: "equals",
      value: [{ name: "dogs" }],
    });
  });
});

describe("extractStringKey (via serializeFilterValue)", () => {
  it("extracts nested string value", () => {
    const result = serializeFilterValue({
      operator: "equals",
      value: { data: { name: "NestedValue" } },
    });
    expect(result).toBe("NestedValue");
  });

  it("handles empty objects gracefully", () => {
    const result = serializeFilterValue({
      operator: "equals",
      value: {},
    });
    expect(result).toBe("");
  });

  it("handles deeply nested objects with recursion limit", () => {
    const deepObj = {
      a: {
        b: {
          c: {
            d: {
              e: { f: { g: { h: { i: { j: { k: { l: "tooDeep" } } } } } } },
            },
          },
        },
      },
    };
    const result = serializeFilterValue({
      operator: "equals",
      value: deepObj,
    });
    expect(result).toBe("[object Object]");
  });
});

describe("REF filter URL round-trip", () => {
  const refColumn: IColumn = { id: "type", columnType: "REF" };
  const columns: IColumn[] = [refColumn];

  it("single REF value: URL → parse → serialize → same URL", () => {
    const query = { "type.name": "Cohort study" };
    const { filters } = parseFiltersFromUrl(query, columns);
    const serialized = serializeFiltersToUrl(filters, "", columns);
    expect(serialized).toEqual({ "type.name": "Cohort study" });
  });

  it("multiple REF values: URL → parse → serialize → same URL", () => {
    const query = { "type.name": "Cohort study|Registry" };
    const { filters } = parseFiltersFromUrl(query, columns);
    const serialized = serializeFiltersToUrl(filters, "", columns);
    expect(serialized).toEqual({ "type.name": "Cohort study|Registry" });
  });

  it("REF filter produces correct GraphQL filter", () => {
    const query = { "type.name": "Cohort study" };
    const { filters } = parseFiltersFromUrl(query, columns);
    const gql = buildGraphQLFilter(filters, columns);
    expect(gql).toEqual({ type: { name: { equals: ["Cohort study"] } } });
  });

  it("multiple REF values produce correct GraphQL filter", () => {
    const query = { "type.name": "Cohort study|Registry" };
    const { filters } = parseFiltersFromUrl(query, columns);
    const gql = buildGraphQLFilter(filters, columns);
    expect(gql).toEqual({
      type: { name: { equals: ["Cohort study", "Registry"] } },
    });
  });
});

describe("BOOL/RADIO/CHECKBOX URL round-trip", () => {
  const boolColumn: IColumn = { id: "mainCatalogue", columnType: "BOOL" };
  const radioColumn: IColumn = { id: "gender", columnType: "RADIO" };
  const checkboxColumn: IColumn = { id: "options", columnType: "CHECKBOX" };

  it("BOOL 'true' parses to {operator: 'equals', value: ['true']}", () => {
    expect(parseFilterValue("true", boolColumn)).toEqual({
      operator: "equals",
      value: ["true"],
    });
  });

  it("BOOL 'false' parses to {operator: 'equals', value: ['false']}", () => {
    expect(parseFilterValue("false", boolColumn)).toEqual({
      operator: "equals",
      value: ["false"],
    });
  });

  it("BOOL '_null_' parses to {operator: 'equals', value: ['_null_']}", () => {
    expect(parseFilterValue("_null_", boolColumn)).toEqual({
      operator: "equals",
      value: ["_null_"],
    });
  });

  it("BOOL 'true|false' parses to {operator: 'equals', value: ['true', 'false']}", () => {
    expect(parseFilterValue("true|false", boolColumn)).toEqual({
      operator: "equals",
      value: ["true", "false"],
    });
  });

  it("BOOL serialize {operator: 'equals', value: ['true']} → 'true'", () => {
    expect(serializeFilterValue({ operator: "equals", value: ["true"] })).toBe(
      "true"
    );
  });

  it("BOOL round-trip: serialize then parse returns same value", () => {
    const columns: IColumn[] = [boolColumn];
    const original: IFilterValue = { operator: "equals", value: ["true"] };
    const serialized = serializeFilterValue(original);
    const { filters } = parseFiltersFromUrl(
      { mainCatalogue: serialized! },
      columns
    );
    expect(filters.get("mainCatalogue")).toEqual(original);
  });

  it("RADIO value parses correctly", () => {
    expect(parseFilterValue("male", radioColumn)).toEqual({
      operator: "equals",
      value: ["male"],
    });
  });

  it("CHECKBOX value parses correctly", () => {
    expect(parseFilterValue("a|b", checkboxColumn)).toEqual({
      operator: "equals",
      value: ["a", "b"],
    });
  });
});

describe("RADIO flat URL serialization", () => {
  it("RADIO serializes with flat key (no .name suffix)", () => {
    const columns = [
      { id: "status", columnType: "RADIO", refTableId: "Status" },
    ] as IColumn[];
    const filters = new Map<string, IFilterValue>();
    filters.set("status", {
      operator: "equals",
      value: ["active", "inactive"],
    });

    const params = serializeFiltersToUrl(filters, "", columns);
    expect(params).toHaveProperty("status", "active|inactive");
    expect(params).not.toHaveProperty("status.name");
  });

  it("RADIO roundtrips through URL with plain strings", () => {
    const columns = [
      { id: "status", columnType: "RADIO", refTableId: "Status" },
    ] as IColumn[];
    const original = new Map<string, IFilterValue>();
    original.set("status", { operator: "equals", value: ["active"] });

    const params = serializeFiltersToUrl(original, "", columns);
    const { filters } = parseFiltersFromUrl(params, columns);
    expect(filters.get("status")).toEqual(original.get("status"));
  });
});

describe("nested REF like filter URL round-trip", () => {
  const refArrayColumn: IColumn = {
    id: "collectionEvents",
    columnType: "REF_ARRAY",
    refTableId: "CollectionEvent",
  };
  const columns: IColumn[] = [refArrayColumn];

  it("like filter on nested REF text column round-trips without losing operator", () => {
    const original = new Map<string, IFilterValue>([
      ["collectionEvents.name", { operator: "like", value: "Smith" }],
    ]);
    const urlParams = serializeFiltersToUrl(original, "", columns);
    const { filters } = parseFiltersFromUrl(urlParams, columns);
    expect(filters.get("collectionEvents.name")).toEqual({
      operator: "like",
      value: "Smith",
    });
  });

  it("like filter on nested REF text column produces correct URL key", () => {
    const original = new Map<string, IFilterValue>([
      ["collectionEvents.name", { operator: "like", value: "Smith" }],
    ]);
    const urlParams = serializeFiltersToUrl(original, "", columns);
    expect(Object.keys(urlParams)).toContain("collectionEvents.name~like");
    expect(urlParams["collectionEvents.name~like"]).toBe("Smith");
  });

  it("like filter on nested REF text column does not produce equals operator on parse", () => {
    const original = new Map<string, IFilterValue>([
      ["collectionEvents.name", { operator: "like", value: "Smith" }],
    ]);
    const urlParams = serializeFiltersToUrl(original, "", columns);
    const { filters } = parseFiltersFromUrl(urlParams, columns);
    expect(filters.get("collectionEvents.name")?.operator).toBe("like");
  });
});

describe("string filter round-trip (type → URL → parse → buildFilter)", () => {
  const stringColumn: IColumn = {
    id: "name",
    label: "Name",
    columnType: "STRING",
  };
  const columns = [stringColumn];

  function roundTrip(input: string) {
    const filterValue: IFilterValue = { operator: "like", value: input };
    const serialized = serializeFilterValue(filterValue);
    const parsed = parseFilterValue(serialized!, stringColumn);
    const gql = buildGraphQLFilter(new Map([["name", parsed!]]), columns);
    return { serialized, parsed, gql };
  }

  it("single term: aap", () => {
    const { serialized, parsed, gql } = roundTrip("aap");
    expect(serialized).toBe("aap");
    expect(parsed).toEqual({ operator: "like", value: "aap" });
    expect(gql).toEqual({ name: { like: "aap" } });
  });

  it("AND terms: aap noot", () => {
    const { serialized, parsed, gql } = roundTrip("aap noot");
    expect(serialized).toBe("aap noot");
    expect(parsed).toEqual({ operator: "like", value: "aap noot" });
    expect(gql).toEqual({
      _and: [{ name: { like: "aap" } }, { name: { like: "noot" } }],
    });
  });

  it("AND terms: aap and noot", () => {
    const { serialized, parsed, gql } = roundTrip("aap and noot");
    expect(serialized).toBe("aap and noot");
    expect(parsed).toEqual({ operator: "like", value: "aap and noot" });
    expect(gql).toEqual({
      _and: [{ name: { like: "aap" } }, { name: { like: "noot" } }],
    });
  });
});

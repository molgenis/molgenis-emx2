import { describe, it, expect } from "vitest";
import {
  serializeFilterValue,
  parseFilterValue,
  serializeFiltersToUrl,
  parseFiltersFromUrl,
} from "../../../app/composables/useFilters";
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

import { describe, it, expect } from "vitest";
import { buildGraphQLFilter } from "../../../app/utils/buildFilter";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";
import { stringColumn as nameColumn, makeColumn } from "../fixtures/columns";

const orderColumn: IColumn = makeColumn({
  id: "order",
  columnType: "REF",
  label: "Order",
  refTableId: "Order",
});
const userColumn: IColumn = makeColumn({
  id: "user",
  columnType: "REF",
  label: "User",
  refTableId: "User",
});

describe("buildGraphQLFilter", () => {
  it("builds nested filter for 3-level path", () => {
    const columns: IColumn[] = [orderColumn];
    const filters = new Map<string, IFilterValue>([
      ["order.pet.category", { operator: "equals", value: { name: "dogs" } }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      order: { pet: { category: { name: { equals: ["dogs"] } } } },
    });
  });

  it("builds nested filter for 2-level path", () => {
    const columns: IColumn[] = [orderColumn];
    const filters = new Map<string, IFilterValue>([
      ["order.status", { operator: "like", value: "active" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      order: { status: { like: "active" } },
    });
  });

  it("builds nested filter for 4-level path", () => {
    const columns: IColumn[] = [userColumn];
    const filters = new Map<string, IFilterValue>([
      [
        "user.address.city.country",
        { operator: "equals", value: { name: "Netherlands" } },
      ],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      user: {
        address: {
          city: { country: { name: { equals: ["Netherlands"] } } },
        },
      },
    });
  });

  it("combines multiple nested filters at different depths", () => {
    const columns: IColumn[] = [orderColumn, userColumn];
    const filters = new Map<string, IFilterValue>([
      ["order.pet.category", { operator: "equals", value: { name: "dogs" } }],
      ["user.name", { operator: "like", value: "John" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      order: { pet: { category: { name: { equals: ["dogs"] } } } },
      user: { name: { like: "John" } },
    });
  });

  it("handles nested filters with between operator", () => {
    const columns: IColumn[] = [orderColumn];
    const filters = new Map<string, IFilterValue>([
      ["order.price", { operator: "between", value: [10, 100] }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      order: { price: { between: { min: 10, max: 100 } } },
    });
  });

  it("handles nested filters with equals operator for ref arrays", () => {
    const columns: IColumn[] = [orderColumn];
    const filters = new Map<string, IFilterValue>([
      [
        "order.pet.category",
        {
          operator: "equals",
          value: [{ name: "dogs" }, { name: "cats" }],
        },
      ],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      order: {
        pet: { category: { name: { equals: ["dogs", "cats"] } } },
      },
    });
  });

  it("ignores filters for unknown root columns", () => {
    const columns: IColumn[] = [orderColumn];
    const filters = new Map<string, IFilterValue>([
      ["unknown.pet.category", { operator: "equals", value: { name: "dogs" } }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({});
  });

  it("builds simple filter without nesting", () => {
    const columns: IColumn[] = [nameColumn];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "John" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      name: { like: "John" },
    });
  });

  it("includes search in filter", () => {
    const columns: IColumn[] = [nameColumn];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "John" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "search term");
    expect(result).toEqual({
      _search: "search term",
      name: { like: "John" },
    });
  });

  it("parses space-separated string as AND terms", () => {
    const columns: IColumn[] = [nameColumn];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "dog cat" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      _and: [{ name: { like: "dog" } }, { name: { like: "cat" } }],
    });
  });

  it("parses 'and' keyword string as AND terms with _and wrapper", () => {
    const columns: IColumn[] = [nameColumn];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "dog and cat" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      _and: [{ name: { like: "dog" } }, { name: { like: "cat" } }],
    });
  });

  it("treats plus as literal character (not AND separator)", () => {
    const columns: IColumn[] = [nameColumn];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "dog+cat" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      name: { like: "dog+cat" },
    });
  });

  it("parses nested path with 'and' keyword string", () => {
    const columns: IColumn[] = [orderColumn];
    const filters = new Map<string, IFilterValue>([
      ["order.pet.name", { operator: "like", value: "dog and cat" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      _and: [
        { order: { pet: { name: { like: "dog" } } } },
        { order: { pet: { name: { like: "cat" } } } },
      ],
    });
  });

  it("handles single term like filter without parsing", () => {
    const columns: IColumn[] = [nameColumn];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "dog" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      name: { like: "dog" },
    });
  });

  it("treats standalone 'and' keyword as AND separator", () => {
    const columns: IColumn[] = [nameColumn];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "tools and human" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      _and: [{ name: { like: "tools" } }, { name: { like: "human" } }],
    });
  });

  it("parses quoted phrase with unquoted term as AND terms", () => {
    const columns: IColumn[] = [nameColumn];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "'aap noot' mies" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      _and: [{ name: { like: "aap noot" } }, { name: { like: "mies" } }],
    });
  });

  it("parses quoted phrase with AND keyword", () => {
    const columns: IColumn[] = [nameColumn];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "'aap noot' and mies" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      _and: [{ name: { like: "aap noot" } }, { name: { like: "mies" } }],
    });
  });

  it.each([
    ["notNull", { name: { notNull: true } }],
    ["isNull", { name: { isNull: true } }],
  ] as const)("builds %s filter", (operator, expected) => {
    const columns: IColumn[] = [nameColumn];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator, value: null }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual(expected);
  });

  it("passes UUID equals filter through", () => {
    const columns: IColumn[] = [{ id: "id", columnType: "UUID", label: "ID" }];
    const filters = new Map<string, IFilterValue>([
      ["id", { operator: "equals", value: "550e8400" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({ id: { equals: "550e8400" } });
  });

  it("unwraps ref objects for 'equals' operator with array of objects", () => {
    const columns: IColumn[] = [
      { id: "type", columnType: "REF", label: "Type", refTableId: "Type" },
    ];
    const filters = new Map<string, IFilterValue>([
      [
        "type",
        { operator: "equals", value: [{ name: "org1" }, { name: "org2" }] },
      ],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({ type: { name: { equals: ["org1", "org2"] } } });
  });

  it("keeps plain string values for 'equals' operator with array of strings", () => {
    const columns: IColumn[] = [
      { id: "type", columnType: "STRING", label: "Type" },
    ];
    const filters = new Map<string, IFilterValue>([
      ["type", { operator: "equals", value: ["a", "b"] }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({ type: { equals: ["a", "b"] } });
  });

  it("unwraps ref object for 'equals' operator", () => {
    const columns: IColumn[] = [
      {
        id: "category",
        columnType: "REF",
        label: "Category",
        refTableId: "Category",
      },
    ];
    const filters = new Map<string, IFilterValue>([
      ["category", { operator: "equals", value: [{ code: "A1" }] }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({ category: { code: { equals: ["A1"] } } });
  });

  it("uses _match_any_including_children for ONTOLOGY columns", () => {
    const columns: IColumn[] = [
      {
        id: "tags",
        columnType: "ONTOLOGY_ARRAY",
        label: "Tags",
        refTableId: "Tag",
      },
    ];
    const filters = new Map<string, IFilterValue>([
      ["tags", { operator: "equals", value: [{ name: "colors" }] }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      tags: { _match_any_including_children: ["colors"] },
    });
  });

  it("uses _match_any_including_children for multiple ONTOLOGY values", () => {
    const columns: IColumn[] = [
      {
        id: "tags",
        columnType: "ONTOLOGY",
        label: "Tags",
        refTableId: "Tag",
      },
    ];
    const filters = new Map<string, IFilterValue>([
      [
        "tags",
        {
          operator: "equals",
          value: [{ name: "green" }, { name: "blue" }],
        },
      ],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      tags: { _match_any_including_children: ["green", "blue"] },
    });
  });

  it("between with [null, null] produces no filter entry", () => {
    const columns: IColumn[] = [{ id: "age", columnType: "INT", label: "Age" }];
    const filters = new Map<string, IFilterValue>([
      ["age", { operator: "between", value: [null, null] }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({});
  });

  it("equals with empty array produces { equals: [] }", () => {
    const columns: IColumn[] = [nameColumn];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "equals", value: [] }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({ name: { equals: [] } });
  });
});

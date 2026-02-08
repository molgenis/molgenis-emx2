import { describe, it, expect } from "vitest";
import { buildGraphQLFilter } from "../../../app/utils/buildFilter";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";

describe("buildGraphQLFilter", () => {
  it("builds nested filter for 3-level path", () => {
    const columns: IColumn[] = [
      { id: "order", columnType: "REF", label: "Order", refTableId: "Order" },
    ];
    const filters = new Map<string, IFilterValue>([
      ["order.pet.category", { operator: "equals", value: { name: "dogs" } }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      order: { pet: { category: { equals: [{ name: "dogs" }] } } },
    });
  });

  it("builds nested filter for 2-level path", () => {
    const columns: IColumn[] = [
      { id: "order", columnType: "REF", label: "Order", refTableId: "Order" },
    ];
    const filters = new Map<string, IFilterValue>([
      ["order.status", { operator: "like", value: "active" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      order: { status: { like: "active" } },
    });
  });

  it("builds nested filter for 4-level path", () => {
    const columns: IColumn[] = [
      { id: "user", columnType: "REF", label: "User", refTableId: "User" },
    ];
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
          city: { country: { equals: [{ name: "Netherlands" }] } },
        },
      },
    });
  });

  it("combines multiple nested filters at different depths", () => {
    const columns: IColumn[] = [
      { id: "order", columnType: "REF", label: "Order", refTableId: "Order" },
      { id: "user", columnType: "REF", label: "User", refTableId: "User" },
    ];
    const filters = new Map<string, IFilterValue>([
      ["order.pet.category", { operator: "equals", value: { name: "dogs" } }],
      ["user.name", { operator: "like", value: "John" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      order: { pet: { category: { equals: [{ name: "dogs" }] } } },
      user: { name: { like: "John" } },
    });
  });

  it("handles nested filters with between operator", () => {
    const columns: IColumn[] = [
      { id: "order", columnType: "REF", label: "Order", refTableId: "Order" },
    ];
    const filters = new Map<string, IFilterValue>([
      ["order.price", { operator: "between", value: [10, 100] }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      order: { price: { between: { min: 10, max: 100 } } },
    });
  });

  it("handles nested filters with in operator", () => {
    const columns: IColumn[] = [
      { id: "order", columnType: "REF", label: "Order", refTableId: "Order" },
    ];
    const filters = new Map<string, IFilterValue>([
      [
        "order.pet.category",
        {
          operator: "in",
          value: [{ name: "dogs" }, { name: "cats" }],
        },
      ],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      order: {
        pet: { category: { equals: [{ name: "dogs" }, { name: "cats" }] } },
      },
    });
  });

  it("ignores filters for unknown root columns", () => {
    const columns: IColumn[] = [
      { id: "order", columnType: "REF", label: "Order", refTableId: "Order" },
    ];
    const filters = new Map<string, IFilterValue>([
      ["unknown.pet.category", { operator: "equals", value: { name: "dogs" } }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({});
  });

  it("builds simple filter without nesting", () => {
    const columns: IColumn[] = [
      { id: "name", columnType: "STRING", label: "Name" },
    ];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "John" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      name: { like: "John" },
    });
  });

  it("includes search in filter", () => {
    const columns: IColumn[] = [
      { id: "name", columnType: "STRING", label: "Name" },
    ];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "John" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "search term");
    expect(result).toEqual({
      _search: "search term",
      name: { like: "John" },
    });
  });

  it("parses space-separated string as OR terms", () => {
    const columns: IColumn[] = [
      { id: "name", columnType: "STRING", label: "Name" },
    ];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "dog cat" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      name: { like: ["dog", "cat"] },
    });
  });

  it("parses 'and' keyword string as AND terms with _and wrapper", () => {
    const columns: IColumn[] = [
      { id: "name", columnType: "STRING", label: "Name" },
    ];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "dog and cat" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      _and: [{ name: { like: "dog" } }, { name: { like: "cat" } }],
    });
  });

  it("treats plus as literal character (not AND separator)", () => {
    const columns: IColumn[] = [
      { id: "name", columnType: "STRING", label: "Name" },
    ];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "dog+cat" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      name: { like: "dog+cat" },
    });
  });

  it("parses nested path with 'and' keyword string", () => {
    const columns: IColumn[] = [
      { id: "order", columnType: "REF", label: "Order", refTableId: "Order" },
    ];
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
    const columns: IColumn[] = [
      { id: "name", columnType: "STRING", label: "Name" },
    ];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "dog" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      name: { like: "dog" },
    });
  });

  it("preserves 'and' in middle of word", () => {
    const columns: IColumn[] = [
      { id: "name", columnType: "STRING", label: "Name" },
    ];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "tools and human" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      _and: [{ name: { like: "tools" } }, { name: { like: "human" } }],
    });
  });

  it("parses quoted phrase as single OR term", () => {
    const columns: IColumn[] = [
      { id: "name", columnType: "STRING", label: "Name" },
    ];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "'aap noot' mies" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      name: { like: ["aap noot", "mies"] },
    });
  });

  it("parses quoted phrase with AND keyword", () => {
    const columns: IColumn[] = [
      { id: "name", columnType: "STRING", label: "Name" },
    ];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "'aap noot' and mies" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      _and: [{ name: { like: "aap noot" } }, { name: { like: "mies" } }],
    });
  });

  it("preserves existing behavior without quotes", () => {
    const columns: IColumn[] = [
      { id: "name", columnType: "STRING", label: "Name" },
    ];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "aap noot" }],
    ]);
    const result = buildGraphQLFilter(filters, columns, "");
    expect(result).toEqual({
      name: { like: ["aap", "noot"] },
    });
  });
});

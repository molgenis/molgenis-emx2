import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { ref, nextTick } from "vue";
import { useFilters } from "../../../app/composables/useFilters";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";

describe("useFilters", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  const mockColumns = ref<IColumn[]>([
    {
      id: "name",
      label: "Name",
      columnType: "STRING",
    },
    {
      id: "age",
      label: "Age",
      columnType: "INT",
    },
    {
      id: "category",
      label: "Category",
      columnType: "REF",
      refSchemaId: "test",
      refTableId: "Category",
    },
  ]);

  it("should initialize with empty filter state", () => {
    const { filterStates, searchValue, gqlFilter } = useFilters(mockColumns);

    expect(filterStates.value.size).toBe(0);
    expect(searchValue.value).toBe("");
    expect(gqlFilter.value).toEqual({});
  });

  it("should set a filter", async () => {
    const { filterStates, setFilter } = useFilters(mockColumns);

    const filterValue: IFilterValue = { operator: "like", value: "test" };
    setFilter("name", filterValue);

    expect(filterStates.value.get("name")).toEqual(filterValue);
    expect(filterStates.value.size).toBe(1);
  });

  it("should remove a filter when value is null", async () => {
    const { filterStates, setFilter } = useFilters(mockColumns);

    setFilter("name", { operator: "like", value: "test" });
    expect(filterStates.value.size).toBe(1);

    setFilter("name", null);
    expect(filterStates.value.size).toBe(0);
    expect(filterStates.value.has("name")).toBe(false);
  });

  it("should remove a specific filter", async () => {
    const { filterStates, setFilter, removeFilter } = useFilters(mockColumns);

    setFilter("name", { operator: "like", value: "test" });
    setFilter("age", { operator: "between", value: [10, 20] });
    expect(filterStates.value.size).toBe(2);

    removeFilter("name");
    expect(filterStates.value.size).toBe(1);
    expect(filterStates.value.has("name")).toBe(false);
    expect(filterStates.value.has("age")).toBe(true);
  });

  it("should clear all filters", async () => {
    const { filterStates, searchValue, setFilter, setSearch, clearFilters } =
      useFilters(mockColumns);

    setFilter("name", { operator: "like", value: "test" });
    setFilter("age", { operator: "equals", value: 25 });
    setSearch("search term");

    expect(filterStates.value.size).toBe(2);
    expect(searchValue.value).toBe("search term");

    clearFilters();
    expect(filterStates.value.size).toBe(0);
    expect(searchValue.value).toBe("");
  });

  it("should set search value", () => {
    const { searchValue, setSearch } = useFilters(mockColumns);

    setSearch("hello");
    expect(searchValue.value).toBe("hello");
  });

  it("should debounce gqlFilter updates", async () => {
    const { gqlFilter, setFilter } = useFilters(mockColumns, {
      debounceMs: 300,
    });

    expect(gqlFilter.value).toEqual({});

    setFilter("name", { operator: "like", value: "test" });

    // immediately after, gqlFilter should still be empty (debounced)
    expect(gqlFilter.value).toEqual({});

    // advance timers by 300ms
    vi.advanceTimersByTime(300);
    await nextTick();

    // now gqlFilter should be updated
    expect(gqlFilter.value).toEqual({
      name: { like: "test" },
    });
  });

  it("should build correct GraphQL filter for equals operator", async () => {
    const { gqlFilter, setFilter } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setFilter("age", { operator: "equals", value: 25 });

    await nextTick();

    expect(gqlFilter.value).toEqual({
      age: { equals: 25 },
    });
  });

  it("should build correct GraphQL filter for like operator", async () => {
    const { gqlFilter, setFilter } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setFilter("name", { operator: "like", value: "John" });

    await nextTick();

    expect(gqlFilter.value).toEqual({
      name: { like: "John" },
    });
  });

  it("should build correct GraphQL filter for between operator", async () => {
    const { gqlFilter, setFilter } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setFilter("age", { operator: "between", value: [10, 20] });

    await nextTick();

    expect(gqlFilter.value).toEqual({
      age: { between: { min: 10, max: 20 } },
    });
  });

  it("should build correct GraphQL filter for between with only min", async () => {
    const { gqlFilter, setFilter } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setFilter("age", { operator: "between", value: [10, null] });

    await nextTick();

    expect(gqlFilter.value).toEqual({
      age: { between: { min: 10 } },
    });
  });

  it("should build correct GraphQL filter for between with only max", async () => {
    const { gqlFilter, setFilter } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setFilter("age", { operator: "between", value: [null, 20] });

    await nextTick();

    expect(gqlFilter.value).toEqual({
      age: { between: { max: 20 } },
    });
  });

  it("should build correct GraphQL filter for in operator with array", async () => {
    const { gqlFilter, setFilter } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setFilter("category", {
      operator: "in",
      value: [{ name: "Cat1" }, { name: "Cat2" }],
    });

    await nextTick();

    expect(gqlFilter.value).toEqual({
      category: { equals: [{ name: "Cat1" }, { name: "Cat2" }] },
    });
  });

  it("should build correct GraphQL filter for in operator with single value", async () => {
    const { gqlFilter, setFilter } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setFilter("category", { operator: "in", value: { name: "Cat1" } });

    await nextTick();

    expect(gqlFilter.value).toEqual({
      category: { equals: [{ name: "Cat1" }] },
    });
  });

  it("should include search in GraphQL filter", async () => {
    const { gqlFilter, setSearch } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setSearch("search term");

    await nextTick();

    expect(gqlFilter.value).toEqual({
      _search: "search term",
    });
  });

  it("should combine multiple filters with search", async () => {
    const { gqlFilter, setFilter, setSearch } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setFilter("name", { operator: "like", value: "John" });
    setFilter("age", { operator: "between", value: [18, 65] });
    setSearch("active");

    await nextTick();

    expect(gqlFilter.value).toEqual({
      _search: "active",
      name: { like: "John" },
      age: { between: { min: 18, max: 65 } },
    });
  });

  it("should handle notNull operator", async () => {
    const { gqlFilter, setFilter } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setFilter("name", { operator: "notNull", value: true });

    await nextTick();

    expect(gqlFilter.value).toEqual({
      name: { notNull: true },
    });
  });

  it("should handle isNull operator", async () => {
    const { gqlFilter, setFilter } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setFilter("name", { operator: "isNull", value: true });

    await nextTick();

    expect(gqlFilter.value).toEqual({
      name: { isNull: true },
    });
  });

  it("should ignore filters for unknown columns", async () => {
    const { gqlFilter, setFilter } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setFilter("unknownColumn", { operator: "like", value: "test" });

    await nextTick();

    expect(gqlFilter.value).toEqual({});
  });

  it("should trim search value", async () => {
    const { gqlFilter, setSearch } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setSearch("  trimmed  ");

    await nextTick();

    expect(gqlFilter.value).toEqual({
      _search: "trimmed",
    });
  });

  it("should not include empty search in filter", async () => {
    const { gqlFilter, setSearch } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setSearch("   ");

    await nextTick();

    expect(gqlFilter.value).toEqual({});
  });
});

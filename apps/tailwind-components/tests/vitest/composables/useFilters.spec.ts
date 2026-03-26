import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { ref, reactive, nextTick } from "vue";
import {
  useFilters,
  serializeFilterValue,
  parseFilterValue,
  serializeFiltersToUrl,
  parseFiltersFromUrl,
} from "../../../app/composables/useFilters";
import { buildGraphQLFilter } from "../../../app/utils/buildFilter";
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

  it("returns a UseFilters-typed object", () => {
    const result = useFilters(mockColumns);
    expect(result).toHaveProperty("filterStates");
    expect(result).toHaveProperty("searchValue");
    expect(result).toHaveProperty("gqlFilter");
    expect(result).toHaveProperty("activeFilters");
    expect(result).toHaveProperty("setFilter");
    expect(result).toHaveProperty("setSearch");
    expect(result).toHaveProperty("clearFilters");
    expect(result).toHaveProperty("removeFilter");
  });

  it("initializes with empty filter state", () => {
    const { filterStates, searchValue, gqlFilter } = useFilters(mockColumns);

    expect(filterStates.value.size).toBe(0);
    expect(searchValue.value).toBe("");
    expect(gqlFilter.value).toEqual({});
  });

  it("sets a filter", async () => {
    const { filterStates, setFilter } = useFilters(mockColumns);

    const filterValue: IFilterValue = { operator: "like", value: "test" };
    setFilter("name", filterValue);

    expect(filterStates.value.get("name")).toEqual(filterValue);
    expect(filterStates.value.size).toBe(1);
  });

  it("removes a filter when value is null", async () => {
    const { filterStates, setFilter } = useFilters(mockColumns);

    setFilter("name", { operator: "like", value: "test" });
    expect(filterStates.value.size).toBe(1);

    setFilter("name", null);
    expect(filterStates.value.size).toBe(0);
    expect(filterStates.value.has("name")).toBe(false);
  });

  it("removes a specific filter", async () => {
    const { filterStates, setFilter, removeFilter } = useFilters(mockColumns);

    setFilter("name", { operator: "like", value: "test" });
    setFilter("age", { operator: "between", value: [10, 20] });
    expect(filterStates.value.size).toBe(2);

    removeFilter("name");
    expect(filterStates.value.size).toBe(1);
    expect(filterStates.value.has("name")).toBe(false);
    expect(filterStates.value.has("age")).toBe(true);
  });

  it("clears all filters", async () => {
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

  it("sets search value", () => {
    const { searchValue, setSearch } = useFilters(mockColumns);

    setSearch("hello");
    expect(searchValue.value).toBe("hello");
  });

  it("debounces gqlFilter updates", async () => {
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

  it("builds correct GraphQL filter for equals operator", async () => {
    const { gqlFilter, setFilter } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setFilter("age", { operator: "equals", value: 25 });

    await nextTick();

    expect(gqlFilter.value).toEqual({
      age: { equals: 25 },
    });
  });

  it("builds correct GraphQL filter for between operator with both bounds", async () => {
    const { gqlFilter, setFilter } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setFilter("age", { operator: "between", value: [10, 20] });

    await nextTick();

    expect(gqlFilter.value).toEqual({
      age: { between: { min: 10, max: 20 } },
    });
  });

  it("includes search in GraphQL filter", async () => {
    const { gqlFilter, setSearch } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setSearch("search term");

    await nextTick();

    expect(gqlFilter.value).toEqual({
      _search: "search term",
    });
  });

  it("combines multiple filters with search", async () => {
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

  it("ignores filters for unknown columns", async () => {
    const { gqlFilter, setFilter } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setFilter("unknownColumn", { operator: "like", value: "test" });

    await nextTick();

    expect(gqlFilter.value).toEqual({});
  });

  it("trims search value", async () => {
    const { gqlFilter, setSearch } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setSearch("  trimmed  ");

    await nextTick();

    expect(gqlFilter.value).toEqual({
      _search: "trimmed",
    });
  });

  it("does not include empty search in filter", async () => {
    const { gqlFilter, setSearch } = useFilters(mockColumns, {
      debounceMs: 0,
    });

    setSearch("   ");

    await nextTick();

    expect(gqlFilter.value).toEqual({});
  });

  it("updates URL on filter change with injected router", async () => {
    const mockRoute = reactive({ query: {} as Record<string, string> });
    const mockRouter = {
      replace: vi.fn((opts) => {
        // Simulate router actually updating the route
        mockRoute.query = opts.query as Record<string, string>;
      }),
    };

    const { setFilter, setSearch } = useFilters(mockColumns, {
      debounceMs: 0,
      urlSync: true,
      route: mockRoute,
      router: mockRouter,
    });

    setFilter("name", { operator: "like", value: "John" });
    await nextTick();

    expect(mockRouter.replace).toHaveBeenCalledWith({
      query: { name: "John" },
    });

    setSearch("search term");
    await nextTick();

    expect(mockRouter.replace).toHaveBeenCalledWith({
      query: { mg_search: "search term", name: "John" },
    });
  });

  it("initializes from URL with injected route", () => {
    const mockRoute = {
      query: {
        name: "John",
        age: "18..65",
        mg_search: "test",
      },
    };
    const mockRouter = { replace: vi.fn() };

    const { filterStates, searchValue } = useFilters(mockColumns, {
      urlSync: true,
      route: mockRoute,
      router: mockRouter,
    });

    expect(filterStates.value.size).toBe(2);
    expect(filterStates.value.get("name")).toEqual({
      operator: "like",
      value: "John",
    });
    expect(filterStates.value.get("age")).toEqual({
      operator: "between",
      value: [18, 65],
    });
    expect(searchValue.value).toBe("test");
  });

  it("gracefully degrades when urlSync enabled but no router provided", async () => {
    const { gqlFilter, setFilter } = useFilters(mockColumns, {
      debounceMs: 0,
      urlSync: true,
    });

    // Should work without router, just no URL sync
    setFilter("name", { operator: "like", value: "test" });

    await nextTick();

    expect(gqlFilter.value).toEqual({
      name: { like: "test" },
    });
  });

  it("preserves reserved query params when updating URL", async () => {
    const mockRoute = { query: { mg_page: "2", mg_limit: "10", page: "3" } };
    const mockRouter = { replace: vi.fn() };

    const { setFilter } = useFilters(mockColumns, {
      debounceMs: 0,
      urlSync: true,
      route: mockRoute,
      router: mockRouter,
    });

    setFilter("name", { operator: "like", value: "test" });

    await nextTick();

    expect(mockRouter.replace).toHaveBeenCalledWith({
      query: { mg_page: "2", mg_limit: "10", page: "3", name: "test" },
    });
  });

  it("preserves non-filter query params (e.g. page) when updating URL", async () => {
    const mockRoute = { query: { page: "2", view: "cards" } };
    const mockRouter = { replace: vi.fn() };

    const { setFilter } = useFilters(mockColumns, {
      debounceMs: 0,
      urlSync: true,
      route: mockRoute,
      router: mockRouter,
    });

    setFilter("name", { operator: "like", value: "test" });
    await nextTick();

    expect(mockRouter.replace).toHaveBeenCalledWith({
      query: expect.objectContaining({
        page: "2",
        view: "cards",
        name: "test",
      }),
    });
  });

  it("updates URL immediately without waiting for debounce", async () => {
    const mockRoute = { query: {} };
    const mockRouter = { replace: vi.fn() };

    const { setFilter } = useFilters(mockColumns, {
      debounceMs: 300,
      urlSync: true,
      route: mockRoute,
      router: mockRouter,
    });

    setFilter("name", { operator: "like", value: "test" });

    // URL should be updated immediately
    await nextTick();
    expect(mockRouter.replace).toHaveBeenCalledWith({
      query: { name: "test" },
    });

    // But gqlFilter is still debounced (tested elsewhere)
  });

  it("handles rapid filter changes", async () => {
    const mockRoute = { query: {} };
    const mockRouter = { replace: vi.fn() };

    const { setFilter } = useFilters(mockColumns, {
      debounceMs: 0,
      urlSync: true,
      route: mockRoute,
      router: mockRouter,
    });

    // Rapidly set multiple filters
    setFilter("name", { operator: "like", value: "test1" });
    setFilter("name", { operator: "like", value: "test2" });
    setFilter("name", { operator: "like", value: "test3" });

    await nextTick();

    // Should have called replace for each change (3 times)
    expect(mockRouter.replace).toHaveBeenCalledTimes(3);
    expect(mockRouter.replace).toHaveBeenLastCalledWith({
      query: { name: "test3" },
    });
  });

  it("updates filters when URL changes (browser back/forward)", async () => {
    const mockRoute = reactive({ query: {} as Record<string, string> });
    const mockRouter = { replace: vi.fn() };

    const { filterStates } = useFilters(mockColumns, {
      debounceMs: 0,
      urlSync: true,
      route: mockRoute,
      router: mockRouter,
    });

    // Simulate browser back/forward by changing route.query
    mockRoute.query = { name: "John" };

    await nextTick();

    // Filters should be updated (computed from URL)
    expect(filterStates.value.get("name")).toEqual({
      operator: "like",
      value: "John",
    });
  });

  it("computes activeFilters from filterStates", () => {
    const { setFilter, activeFilters } = useFilters(mockColumns);
    setFilter("name", { operator: "like", value: "John" });
    expect(activeFilters.value).toEqual([
      { columnId: "name", label: "Name", displayValue: "John", values: [] },
    ]);
  });

  it("activeFilters excludes empty filter values", () => {
    const { setFilter, activeFilters } = useFilters(mockColumns);
    setFilter("name", { operator: "like", value: "" });
    expect(activeFilters.value).toEqual([]);
  });

  it("toggleFilter prepends new filter at beginning of list", () => {
    const columns = ref([
      { id: "name", columnType: "STRING" },
      { id: "age", columnType: "INT" },
      { id: "status", columnType: "STRING" },
    ]);
    const { visibleFilterIds, toggleFilter } = useFilters(columns);
    visibleFilterIds.value = ["name", "age"];
    toggleFilter("status");
    expect(visibleFilterIds.value[0]).toBe("status");
    expect(visibleFilterIds.value).toEqual(["status", "name", "age"]);
  });

  it("toggleFilter clears filter state when removing a filter", () => {
    const columns = ref([
      { id: "name", columnType: "STRING" },
      { id: "age", columnType: "INT" },
    ]);
    const { visibleFilterIds, toggleFilter, setFilter, filterStates } =
      useFilters(columns);
    visibleFilterIds.value = ["name", "age"];
    setFilter("name", { operator: "like", value: "test" });
    expect(filterStates.value.has("name")).toBe(true);
    toggleFilter("name");
    expect(visibleFilterIds.value).toEqual(["age"]);
    expect(filterStates.value.has("name")).toBe(false);
  });

  it("reactively updates when URL changes", async () => {
    const mockRoute = reactive({
      query: { name: "test" } as Record<string, string>,
    });
    const mockRouter = { replace: vi.fn() };

    const { filterStates } = useFilters(mockColumns, {
      debounceMs: 0,
      urlSync: true,
      route: mockRoute,
      router: mockRouter,
    });

    // Initial state from URL
    expect(filterStates.value.get("name")).toEqual({
      operator: "like",
      value: "test",
    });

    // Change URL
    mockRoute.query = { name: "updated" };
    await nextTick();

    // State should update (computed from URL)
    expect(filterStates.value.get("name")).toEqual({
      operator: "like",
      value: "updated",
    });
  });
});

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

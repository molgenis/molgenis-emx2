import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { ref, reactive, nextTick } from "vue";
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

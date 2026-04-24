import { describe, it, expect, vi, beforeEach } from "vitest";
import { ref, nextTick } from "vue";
import { flushPromises } from "@vue/test-utils";
import type { IColumn } from "../../../../metadata-utils/src/types";

vi.mock("../../../app/composables/fetchGraphql", () => ({
  default: vi.fn().mockResolvedValue({}),
}));

vi.mock("../../../app/composables/fetchTableMetadata", () => ({
  default: vi.fn().mockResolvedValue({ columns: [] }),
}));

import {
  useFilters,
  MG_FILTERS_PARAM,
  MG_COLLAPSED_PARAM,
} from "../../../app/composables/useFilters";

const ontologyColumn: IColumn = {
  id: "status",
  label: "Status",
  columnType: "ONTOLOGY",
};

const intColumn: IColumn = {
  id: "age",
  label: "Age",
  columnType: "INT",
};

const stringColumn: IColumn = {
  id: "name",
  label: "Name",
  columnType: "STRING",
};

const boolColumn: IColumn = {
  id: "active",
  label: "Active",
  columnType: "BOOL",
};

const allColumns: IColumn[] = [
  ontologyColumn,
  intColumn,
  stringColumn,
  boolColumn,
];

function makeUrlSync(queryInit: Record<string, string> = {}) {
  const mockQuery: Record<string, string> = { ...queryInit };
  const replaceCalls: Array<{ query: Record<string, unknown> }> = [];
  const route = { query: mockQuery };
  const router = {
    replace: (opts: any) => {
      replaceCalls.push(opts);
      Object.assign(mockQuery, opts.query ?? {});
      for (const key of Object.keys(mockQuery)) {
        if (!(key in (opts.query ?? {}))) delete mockQuery[key];
      }
    },
  };
  return { route, router, mockQuery, replaceCalls };
}

describe("useFilters — filter state management", () => {
  it("starts with empty filter states", () => {
    const columns = ref(allColumns);
    const { filterStates } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    expect(filterStates.value.size).toBe(0);
  });

  it("setFilter adds a filter", () => {
    const columns = ref(allColumns);
    const { filterStates, setFilter } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    setFilter("status", { operator: "equals", value: ["active"] });
    expect(filterStates.value.has("status")).toBe(true);
    expect(filterStates.value.get("status")?.value).toEqual(["active"]);
  });

  it("removeFilter removes a filter", () => {
    const columns = ref(allColumns);
    const { filterStates, setFilter, removeFilter } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    setFilter("status", { operator: "equals", value: ["active"] });
    removeFilter("status");
    expect(filterStates.value.has("status")).toBe(false);
  });

  it("setFilter with null removes filter", () => {
    const columns = ref(allColumns);
    const { filterStates, setFilter } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    setFilter("status", { operator: "equals", value: ["active"] });
    setFilter("status", null);
    expect(filterStates.value.has("status")).toBe(false);
  });

  it("clearFilters removes all filters and search", () => {
    const columns = ref(allColumns);
    const { filterStates, searchValue, setFilter, setSearch, clearFilters } =
      useFilters(columns, { schemaId: "test", tableId: "table1" });
    setFilter("status", { operator: "equals", value: ["active"] });
    setSearch("hello");
    clearFilters();
    expect(filterStates.value.size).toBe(0);
    expect(searchValue.value).toBe("");
  });

  it("setSearch updates searchValue", () => {
    const columns = ref(allColumns);
    const { searchValue, setSearch } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    setSearch("diabetes");
    expect(searchValue.value).toBe("diabetes");
  });
});

describe("useFilters — activeFilters computation", () => {
  it("returns empty array when no filters active", () => {
    const columns = ref(allColumns);
    const { activeFilters } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    expect(activeFilters.value).toEqual([]);
  });

  it("builds active filter with label and displayValue", () => {
    const columns = ref(allColumns);
    const { activeFilters, setFilter } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    setFilter("status", { operator: "equals", value: ["active"] });
    expect(activeFilters.value).toHaveLength(1);
    expect(activeFilters.value[0]!.columnId).toBe("status");
    expect(activeFilters.value[0]!.label).toBe("Status");
    expect(activeFilters.value[0]!.displayValue).toBe("active");
  });

  it("shows count in displayValue when multiple values", () => {
    const columns = ref(allColumns);
    const { activeFilters, setFilter } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    setFilter("status", { operator: "equals", value: ["active", "inactive"] });
    expect(activeFilters.value[0]!.displayValue).toBe("2");
    expect(activeFilters.value[0]!.values).toEqual(["active", "inactive"]);
  });

  it("falls back to columnId as label when column has no label", () => {
    const columns = ref<IColumn[]>([
      { id: "status", columnType: "ONTOLOGY" } as IColumn,
    ]);
    const { activeFilters, setFilter } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    setFilter("status", { operator: "equals", value: ["active"] });
    expect(activeFilters.value[0]!.label).toBe("status");
  });
});

describe("useFilters — gqlFilter computation", () => {
  it("returns empty filter when no filters or search", () => {
    const columns = ref(allColumns);
    const { gqlFilter } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    expect(gqlFilter.value).toEqual({});
  });

  it("includes _search when searchValue set", () => {
    const columns = ref(allColumns);
    const { gqlFilter, setSearch } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    setSearch("diabetes");
    expect(gqlFilter.value._search).toBe("diabetes");
  });

  it("builds filter from set filters", () => {
    const columns = ref(allColumns);
    const { gqlFilter, setFilter } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    setFilter("age", { operator: "between", value: [10, 50] });
    expect((gqlFilter.value as any).age).toBeDefined();
  });
});

describe("useFilters — visibility management", () => {
  it("defaults to ontology/bool columns", () => {
    const columns = ref(allColumns);
    const { visibleFilterIds } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    expect(visibleFilterIds.value).toContain("status");
    expect(visibleFilterIds.value).toContain("active");
    expect(visibleFilterIds.value).not.toContain("name");
    expect(visibleFilterIds.value).not.toContain("age");
  });

  it("toggleFilter adds a non-visible column", () => {
    const columns = ref(allColumns);
    const { visibleFilterIds, toggleFilter } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    expect(visibleFilterIds.value).not.toContain("age");
    toggleFilter("age");
    expect(visibleFilterIds.value).toContain("age");
  });

  it("toggleFilter removes a visible column", () => {
    const columns = ref(allColumns);
    const { visibleFilterIds, toggleFilter } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    expect(visibleFilterIds.value).toContain("status");
    toggleFilter("status");
    expect(visibleFilterIds.value).not.toContain("status");
  });

  it("toggleFilter also removes filter when hiding column", () => {
    const columns = ref(allColumns);
    const { visibleFilterIds, filterStates, setFilter, toggleFilter } =
      useFilters(columns, { schemaId: "test", tableId: "table1" });
    setFilter("status", { operator: "equals", value: ["active"] });
    expect(filterStates.value.has("status")).toBe(true);
    toggleFilter("status");
    expect(visibleFilterIds.value).not.toContain("status");
    expect(filterStates.value.has("status")).toBe(false);
  });

  it("resetFilters restores defaults and clears filters", () => {
    const columns = ref(allColumns);
    const {
      visibleFilterIds,
      filterStates,
      setFilter,
      toggleFilter,
      resetFilters,
    } = useFilters(columns, { schemaId: "test", tableId: "table1" });
    setFilter("status", { operator: "equals", value: ["active"] });
    toggleFilter("age");
    resetFilters();
    expect(filterStates.value.size).toBe(0);
    expect(visibleFilterIds.value).not.toContain("age");
  });
});

describe("useFilters — URL sync", () => {
  it("reads initial filters from URL", () => {
    const { route, router } = makeUrlSync({ status: "active" });
    const columns = ref<IColumn[]>([ontologyColumn]);
    const { filterStates } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      urlSync: true,
      route,
      router,
    });
    expect(filterStates.value.has("status")).toBe(true);
  });

  it("setFilter updates URL", () => {
    const { route, router, replaceCalls } = makeUrlSync();
    const columns = ref<IColumn[]>([ontologyColumn]);
    const { setFilter } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      urlSync: true,
      route,
      router,
    });
    setFilter("status", { operator: "equals", value: ["active"] });
    expect(replaceCalls.length).toBeGreaterThan(0);
  });

  it("reads mg_filters from URL for visible filter set", () => {
    const { route, router } = makeUrlSync({
      [MG_FILTERS_PARAM]: "status,active",
    });
    const columns = ref(allColumns);
    const { visibleFilterIds } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      urlSync: true,
      route,
      router,
    });
    expect(visibleFilterIds.value).toContain("status");
    expect(visibleFilterIds.value).toContain("active");
  });

  it("clearFilters removes filter params from URL", () => {
    const { route, router, replaceCalls } = makeUrlSync({ status: "active" });
    const columns = ref<IColumn[]>([ontologyColumn]);
    const { clearFilters } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      urlSync: true,
      route,
      router,
    });
    clearFilters();
    const lastCall = replaceCalls[replaceCalls.length - 1];
    expect(lastCall?.query?.["status"]).toBeUndefined();
  });

  it("preserves non-filter URL params", () => {
    const { route, router, replaceCalls } = makeUrlSync({
      page: "3",
      sort: "name",
    });
    const columns = ref<IColumn[]>([ontologyColumn]);
    const { setFilter } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      urlSync: true,
      route,
      router,
    });
    setFilter("status", { operator: "equals", value: ["active"] });
    const lastCall = replaceCalls[replaceCalls.length - 1];
    expect(lastCall?.query?.["page"]).toBe("3");
    expect(lastCall?.query?.["sort"]).toBe("name");
  });

  it("setSearch writes mg_search to URL", () => {
    const { route, router, replaceCalls } = makeUrlSync();
    const columns = ref<IColumn[]>([ontologyColumn]);
    const { setSearch } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      urlSync: true,
      route,
      router,
    });
    setSearch("diabetes");
    const lastCall = replaceCalls[replaceCalls.length - 1];
    expect(lastCall?.query?.["mg_search"]).toBe("diabetes");
  });
});

describe("useFilters — defaultFilters option", () => {
  it("uses defaultFilters as initial visible set when urlSync has no mg_filters param", () => {
    const columns = ref(allColumns);
    const { visibleFilterIds } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      defaultFilters: ["name", "age"],
    });
    expect(visibleFilterIds.value).toEqual(["name", "age"]);
  });

  it("resetFilters restores defaultFilters when provided", () => {
    const columns = ref(allColumns);
    const { visibleFilterIds, toggleFilter, resetFilters } = useFilters(
      columns,
      {
        schemaId: "test",
        tableId: "table1",
        defaultFilters: ["name", "age"],
      }
    );
    toggleFilter("status");
    resetFilters();
    expect(visibleFilterIds.value).toEqual(["name", "age"]);
  });
});

describe("useFilters — nested column meta", () => {
  it("registerNestedColumn stores meta accessible via nestedColumnMeta", () => {
    const columns = ref(allColumns);
    const { nestedColumnMeta, registerNestedColumn } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    registerNestedColumn("publisher.country", {
      label: "publisher → country",
      columnType: "ONTOLOGY",
    });
    expect(nestedColumnMeta.value.get("publisher.country")).toEqual({
      label: "publisher → country",
      columnType: "ONTOLOGY",
    });
  });

  it("setFilter with dotted path stores value correctly", () => {
    const columns = ref(allColumns);
    const { filterStates, setFilter } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    setFilter("publisher.country", { operator: "equals", value: ["NL"] });
    expect(filterStates.value.has("publisher.country")).toBe(true);
    expect(filterStates.value.get("publisher.country")?.value).toEqual(["NL"]);
  });

  it("gqlFilter for nested ontology uses columnTypeMap to produce _match_any_including_children", () => {
    const refColumn: IColumn = {
      id: "publisher",
      label: "Publisher",
      columnType: "REF",
      refTableId: "Organisation",
    } as IColumn;
    const columns = ref([...allColumns, refColumn]);
    const { gqlFilter, setFilter, registerNestedColumn } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    registerNestedColumn("publisher.country", {
      label: "publisher → country",
      columnType: "ONTOLOGY",
    });
    setFilter("publisher.country", {
      operator: "equals",
      value: ["NL"],
    });
    expect(
      (gqlFilter.value as any).publisher?.country?._match_any_including_children
    ).toEqual(["NL"]);
  });
});

describe("useFilters — hydrateNestedFilters", () => {
  it("resolves dotted filter id into nestedColumnMeta using rawColumns", async () => {
    const { default: fetchTableMetadata } = await import(
      "../../../app/composables/fetchTableMetadata"
    );
    const refColumn: IColumn = {
      id: "publisher",
      label: "Publisher",
      columnType: "REF",
      refTableId: "Organisation",
    } as IColumn;
    const orgCountryColumn: IColumn = {
      id: "country",
      label: "Country",
      columnType: "ONTOLOGY",
    } as IColumn;
    vi.mocked(fetchTableMetadata).mockResolvedValue({
      columns: [orgCountryColumn],
    } as any);

    const columns = ref([refColumn]);
    const { nestedColumnMeta, hydrateNestedFilters } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      defaultFilters: ["publisher.country"],
    });

    await hydrateNestedFilters();
    await nextTick();

    const meta = nestedColumnMeta.value.get("publisher.country");
    expect(meta?.label).toBe("Publisher → Country");
    expect(meta?.columnType).toBe("ONTOLOGY");
  });

  it("skips dotted ids already in nestedColumnMeta", async () => {
    const { default: fetchTableMetadata } = await import(
      "../../../app/composables/fetchTableMetadata"
    );
    vi.mocked(fetchTableMetadata).mockClear();

    const columns = ref<IColumn[]>([]);
    const { registerNestedColumn, hydrateNestedFilters } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      defaultFilters: ["a.b"],
    });

    registerNestedColumn("a.b", { label: "A → B", columnType: "STRING" });
    await hydrateNestedFilters();

    expect(fetchTableMetadata).not.toHaveBeenCalled();
  });

  it("resolves nested column metadata when rawColumns populates after URL-init with dotted mg_filters", async () => {
    const { default: fetchTableMetadata } = await import(
      "../../../app/composables/fetchTableMetadata"
    );
    const subpopulationsColumn: IColumn = {
      id: "subpopulations",
      label: "Subpopulations",
      columnType: "REF",
      refTableId: "Subpopulations",
    } as IColumn;
    const ageGroupsColumn: IColumn = {
      id: "ageGroups",
      label: "Age Groups",
      columnType: "ONTOLOGY",
    } as IColumn;
    vi.mocked(fetchTableMetadata).mockResolvedValue({
      columns: [ageGroupsColumn],
    } as any);

    const { route, router } = makeUrlSync({
      [MG_FILTERS_PARAM]: "subpopulations.ageGroups",
    });
    const rawColumns = ref<IColumn[]>([]);

    const { nestedColumnMeta } = useFilters(rawColumns, {
      schemaId: "catalogue-demo",
      tableId: "Collections",
      urlSync: true,
      route,
      router,
    });

    await flushPromises();
    expect(
      nestedColumnMeta.value.get("subpopulations.ageGroups")
    ).toBeUndefined();

    rawColumns.value = [subpopulationsColumn];
    await flushPromises();
    await nextTick();

    const meta = nestedColumnMeta.value.get("subpopulations.ageGroups");
    expect(meta?.columnType).toBe("ONTOLOGY");
  });

  it("resolves nested REF leaf column type when rawColumns populates after URL-init", async () => {
    const { default: fetchTableMetadata } = await import(
      "../../../app/composables/fetchTableMetadata"
    );
    const parentColumn: IColumn = {
      id: "parent",
      label: "Parent",
      columnType: "REF",
      refTableId: "ParentTable",
    } as IColumn;
    const refLeafColumn: IColumn = {
      id: "someRef",
      label: "Some Ref",
      columnType: "REF",
    } as IColumn;
    vi.mocked(fetchTableMetadata).mockResolvedValue({
      columns: [refLeafColumn],
    } as any);

    const { route, router } = makeUrlSync({
      [MG_FILTERS_PARAM]: "parent.someRef",
    });
    const rawColumns = ref<IColumn[]>([]);

    const { nestedColumnMeta } = useFilters(rawColumns, {
      schemaId: "test",
      tableId: "table1",
      urlSync: true,
      route,
      router,
    });

    await flushPromises();
    expect(nestedColumnMeta.value.get("parent.someRef")).toBeUndefined();

    rawColumns.value = [parentColumn];
    await flushPromises();
    await nextTick();

    const meta = nestedColumnMeta.value.get("parent.someRef");
    expect(meta?.columnType).toBe("REF");
  });

  it("resolves nested DATE leaf column type when rawColumns populates after URL-init", async () => {
    const { default: fetchTableMetadata } = await import(
      "../../../app/composables/fetchTableMetadata"
    );
    const parentColumn: IColumn = {
      id: "parent",
      label: "Parent",
      columnType: "REF",
      refTableId: "ParentTable",
    } as IColumn;
    const dateLeafColumn: IColumn = {
      id: "someDate",
      label: "Some Date",
      columnType: "DATE",
    } as IColumn;
    vi.mocked(fetchTableMetadata).mockResolvedValue({
      columns: [dateLeafColumn],
    } as any);

    const { route, router } = makeUrlSync({
      [MG_FILTERS_PARAM]: "parent.someDate",
    });
    const rawColumns = ref<IColumn[]>([]);

    const { nestedColumnMeta } = useFilters(rawColumns, {
      schemaId: "test",
      tableId: "table1",
      urlSync: true,
      route,
      router,
    });

    await flushPromises();
    expect(nestedColumnMeta.value.get("parent.someDate")).toBeUndefined();

    rawColumns.value = [parentColumn];
    await flushPromises();
    await nextTick();

    const meta = nestedColumnMeta.value.get("parent.someDate");
    expect(meta?.columnType).toBe("DATE");
  });
});

describe("useFilters — AbortController per column", () => {
  it("aborts prior in-flight count request when a second refetch fires for the same column", async () => {
    const { default: fetchGraphql } = await import(
      "../../../app/composables/fetchGraphql"
    );

    let resolveFirst!: (value: any) => void;
    const firstCallSignals: AbortSignal[] = [];

    vi.mocked(fetchGraphql).mockImplementation(
      (
        _schema: string,
        _query: string,
        _vars: any,
        opts?: { signal?: AbortSignal }
      ) => {
        if (opts?.signal) firstCallSignals.push(opts.signal);
        return new Promise((resolve) => {
          resolveFirst = resolve;
        });
      }
    );

    const columns = ref<IColumn[]>([ontologyColumn]);
    useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });

    await flushPromises();

    const signal = firstCallSignals[0];
    expect(signal).toBeDefined();
    expect(signal?.aborted).toBe(false);

    columns.value = [...columns.value];
    await flushPromises();

    expect(signal?.aborted).toBe(true);

    vi.mocked(fetchGraphql).mockResolvedValue({});
    resolveFirst({ data: {} });
  });
});

describe("useFilters — count integration", () => {
  it("getCountedOptions returns empty array initially", () => {
    const columns = ref<IColumn[]>([intColumn]);
    const { getCountedOptions } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    const opts = getCountedOptions("age");
    expect(opts.value).toEqual([]);
  });

  it("isCountLoading returns false for non-countable column type", () => {
    const columns = ref<IColumn[]>([intColumn]);
    const { isCountLoading } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    expect(isCountLoading("age").value).toBe(false);
  });
});

describe("useFilters — collapsed state", () => {
  it("first 5 visible filters are not collapsed by default", () => {
    const ids = ["a", "b", "c", "d", "e"];
    const columns = ref<IColumn[]>(
      ids.map((id) => ({ id, label: id, columnType: "ONTOLOGY" }))
    );
    const { isCollapsed } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
    });
    for (const id of ids) {
      expect(isCollapsed(id)).toBe(false);
    }
  });

  it("filters beyond index 5 are collapsed by default", () => {
    const ids = ["a", "b", "c", "d", "e", "f", "g"];
    const columns = ref<IColumn[]>(
      ids.map((id) => ({ id, label: id, columnType: "ONTOLOGY" }))
    );
    const { isCollapsed } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      defaultFilters: ids,
    });
    expect(isCollapsed("f")).toBe(true);
    expect(isCollapsed("g")).toBe(true);
  });

  it("filters beyond index 5 with active filter state are not collapsed", () => {
    const ids = ["a", "b", "c", "d", "e", "f"];
    const { route, router } = makeUrlSync({ f: "something" });
    const columns = ref<IColumn[]>(
      ids.map((id) => ({ id, label: id, columnType: "ONTOLOGY" }))
    );
    const { isCollapsed } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      defaultFilters: ids,
      urlSync: true,
      route,
      router,
    });
    expect(isCollapsed("f")).toBe(false);
  });

  it("toggleCollapse collapses an expanded section", () => {
    const columns = ref<IColumn[]>([
      { id: "a", label: "a", columnType: "ONTOLOGY" },
    ]);
    const { isCollapsed, toggleCollapse } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      defaultFilters: ["a"],
    });
    expect(isCollapsed("a")).toBe(false);
    toggleCollapse("a");
    expect(isCollapsed("a")).toBe(true);
  });

  it("toggleCollapse expands a collapsed section", () => {
    const ids = ["a", "b", "c", "d", "e", "f"];
    const columns = ref<IColumn[]>(
      ids.map((id) => ({ id, label: id, columnType: "ONTOLOGY" }))
    );
    const { isCollapsed, toggleCollapse } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      defaultFilters: ids,
    });
    expect(isCollapsed("f")).toBe(true);
    toggleCollapse("f");
    expect(isCollapsed("f")).toBe(false);
  });

  it("defaultCollapsed option overrides auto-collapse rule", () => {
    const ids = ["a", "b", "c", "d", "e", "f"];
    const columns = ref<IColumn[]>(
      ids.map((id) => ({ id, label: id, columnType: "ONTOLOGY" }))
    );
    const { isCollapsed } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      defaultFilters: ids,
      defaultCollapsed: ["b", "d"],
    });
    expect(isCollapsed("a")).toBe(false);
    expect(isCollapsed("b")).toBe(true);
    expect(isCollapsed("c")).toBe(false);
    expect(isCollapsed("d")).toBe(true);
    expect(isCollapsed("f")).toBe(false);
  });

  it("reads mg_collapsed from URL on init", () => {
    const ids = ["a", "b", "c"];
    const { route, router } = makeUrlSync({
      [MG_COLLAPSED_PARAM]: "a,c",
    });
    const columns = ref<IColumn[]>(
      ids.map((id) => ({ id, label: id, columnType: "ONTOLOGY" }))
    );
    const { isCollapsed } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      defaultFilters: ids,
      urlSync: true,
      route,
      router,
    });
    expect(isCollapsed("a")).toBe(true);
    expect(isCollapsed("b")).toBe(false);
    expect(isCollapsed("c")).toBe(true);
  });

  it("toggleCollapse persists to URL", () => {
    const ids = ["a", "b", "c"];
    const { route, router, replaceCalls } = makeUrlSync({});
    const columns = ref<IColumn[]>(
      ids.map((id) => ({ id, label: id, columnType: "ONTOLOGY" }))
    );
    const { toggleCollapse } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      defaultFilters: ids,
      urlSync: true,
      route,
      router,
    });
    toggleCollapse("a");
    const lastCall = replaceCalls[replaceCalls.length - 1];
    expect(lastCall?.query?.[MG_COLLAPSED_PARAM]).toContain("a");
  });

  it("removes mg_collapsed from URL when all sections expanded", () => {
    const ids = ["a"];
    const { route, router, replaceCalls } = makeUrlSync({
      [MG_COLLAPSED_PARAM]: "a",
    });
    const columns = ref<IColumn[]>([
      { id: "a", label: "a", columnType: "ONTOLOGY" },
    ]);
    const { toggleCollapse } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      defaultFilters: ids,
      urlSync: true,
      route,
      router,
    });
    toggleCollapse("a");
    const lastCall = replaceCalls[replaceCalls.length - 1];
    expect(lastCall?.query?.[MG_COLLAPSED_PARAM]).toBeUndefined();
  });

  it("preserves other URL params when updating mg_collapsed", () => {
    const ids = ["a"];
    const { route, router, replaceCalls } = makeUrlSync({
      page: "2",
      sort: "name",
    });
    const columns = ref<IColumn[]>([
      { id: "a", label: "a", columnType: "ONTOLOGY" },
    ]);
    const { toggleCollapse } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      defaultFilters: ids,
      urlSync: true,
      route,
      router,
    });
    toggleCollapse("a");
    const lastCall = replaceCalls[replaceCalls.length - 1];
    expect(lastCall?.query?.["page"]).toBe("2");
    expect(lastCall?.query?.["sort"]).toBe("name");
  });

  it("applies first-5 rule when mg_collapsed is absent from URL", () => {
    const ids = ["a", "b", "c", "d", "e", "f", "g"];
    const { route, router } = makeUrlSync({});
    const columns = ref<IColumn[]>(
      ids.map((id) => ({ id, label: id, columnType: "ONTOLOGY" }))
    );
    const { isCollapsed } = useFilters(columns, {
      schemaId: "test",
      tableId: "table1",
      defaultFilters: ids,
      urlSync: true,
      route,
      router,
    });
    expect(isCollapsed("a")).toBe(false);
    expect(isCollapsed("f")).toBe(true);
  });
});

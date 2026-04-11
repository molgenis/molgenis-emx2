import { describe, it, expect, vi, beforeEach } from "vitest";
import { ref, nextTick } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";

vi.mock("../../../app/composables/fetchGraphql", () => ({
  default: vi.fn().mockResolvedValue({}),
}));

import {
  useFilters,
  MG_FILTERS_PARAM,
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

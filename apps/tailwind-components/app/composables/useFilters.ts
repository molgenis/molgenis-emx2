import {
  ref,
  shallowRef,
  computed,
  watch,
  nextTick,
  type Ref,
  type ComputedRef,
} from "vue";
import { useDebounceFn } from "@vueuse/core";
import type { IColumn } from "../../../metadata-utils/src/types";
import type {
  ActiveFilter,
  IFilterValue,
  IGraphQLFilter,
} from "../../types/filters";
import { buildGraphQLFilter } from "../utils/buildFilter";
import { formatFilterValue } from "../utils/formatFilterValue";
import { computeDefaultFilters } from "../utils/computeDefaultFilters";
import {
  serializeFiltersToUrl,
  parseFiltersFromUrl,
} from "../utils/filterUrlCodec";
import {
  fetchCounts,
  isCountableType,
  type CountedOption,
} from "../utils/fetchCounts";
import fetchGraphql from "./fetchGraphql";

export const MG_FILTERS_PARAM = "mg_filters";
export const MG_SEARCH_PARAM = "mg_search";
export const MAX_VISIBLE_FILTERS = 25;

type RouteQuery = Record<
  string,
  string | string[] | (string | null)[] | null | undefined
>;

export interface UseFiltersOptions {
  urlSync?: boolean;
  schemaId: string;
  tableId: string;
  debounceMs?: number;
  route?: { query: RouteQuery };
  router?: { replace: (opts: Record<string, unknown>) => void };
}

function getNonFilterParams(
  query: RouteQuery,
  cols: IColumn[]
): Record<string, unknown> {
  const columnIds = new Set(cols.map((c) => c.id));
  const preserved: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(query)) {
    if (key === MG_SEARCH_PARAM) continue;
    const firstSegment = key.split(".")[0] ?? key;
    if (columnIds.has(firstSegment)) continue;
    preserved[key] = value;
  }
  return preserved;
}

function arraysEqual(a: string[], b: string[]): boolean {
  if (a.length !== b.length) return false;
  const sortedA = [...a].sort();
  const sortedB = [...b].sort();
  return sortedA.every((val, idx) => val === sortedB[idx]);
}

function jsonEqual(a: unknown, b: unknown): boolean {
  return JSON.stringify(a) === JSON.stringify(b);
}

function resolveRouteRouter(options: UseFiltersOptions): {
  route: { query: RouteQuery } | null;
  router: { replace: (opts: Record<string, unknown>) => void } | null;
} {
  if (options.route && options.router) {
    return { route: options.route, router: options.router };
  }
  try {
    const { useRoute, useRouter } = require("#app/composables/router");
    return { route: useRoute(), router: useRouter() };
  } catch {
    return { route: null, router: null };
  }
}

const EXCLUDED_COLUMN_TYPES = new Set(["HEADING", "SECTION", "FILE"]);

function filterColumns(rawColumns: IColumn[]): IColumn[] {
  return rawColumns.filter(
    (c) => !c.id.startsWith("mg_") && !EXCLUDED_COLUMN_TYPES.has(c.columnType)
  );
}

export function useFilters(
  rawColumns: Ref<IColumn[]>,
  options: UseFiltersOptions
) {
  const { schemaId, tableId, debounceMs = 300 } = options;

  const columns = computed(() => filterColumns(rawColumns.value));
  const urlSync = options.urlSync ?? false;

  const { route, router } = urlSync
    ? resolveRouteRouter(options)
    : { route: null, router: null };
  const urlSyncEnabled = urlSync && !!route && !!router;

  function getCurrentQuery(): RouteQuery {
    return route?.query ?? {};
  }

  const parsedUrlState = computed(() => {
    if (!urlSyncEnabled) {
      return { filters: new Map<string, IFilterValue>(), search: "" };
    }
    return parseFiltersFromUrl(getCurrentQuery(), columns.value);
  });

  const filterStatesRef = shallowRef<Map<string, IFilterValue>>(new Map());
  const searchValueRef = ref("");

  const filterStates: ComputedRef<Map<string, IFilterValue>> = computed(() =>
    urlSyncEnabled ? parsedUrlState.value.filters : filterStatesRef.value
  );

  const searchValue: ComputedRef<string> = computed(() =>
    urlSyncEnabled ? parsedUrlState.value.search : searchValueRef.value
  );

  function updateUrl(newFilters: Map<string, IFilterValue>, newSearch: string) {
    if (!urlSyncEnabled || !route || !router) return;
    const filterParams = serializeFiltersToUrl(
      newFilters,
      newSearch,
      columns.value
    );
    const preserved = getNonFilterParams(getCurrentQuery(), columns.value);
    const mgFiltersParam = getCurrentQuery()[MG_FILTERS_PARAM];
    if (mgFiltersParam !== undefined) {
      preserved[MG_FILTERS_PARAM] = mgFiltersParam;
    }
    router.replace({ query: { ...preserved, ...filterParams } });
  }

  function setFilter(columnId: string, value: IFilterValue | null) {
    const current = new Map(filterStates.value);
    if (value === null) {
      current.delete(columnId);
    } else {
      current.set(columnId, value);
    }
    if (urlSyncEnabled) {
      updateUrl(current, searchValue.value);
    } else {
      filterStatesRef.value = current;
    }
  }

  function removeFilter(columnId: string) {
    setFilter(columnId, null);
  }

  function setSearch(value: string) {
    if (urlSyncEnabled) {
      updateUrl(filterStates.value, value);
    } else {
      searchValueRef.value = value;
    }
  }

  function clearFilters() {
    if (urlSyncEnabled) {
      updateUrl(new Map(), "");
    } else {
      filterStatesRef.value = new Map();
      searchValueRef.value = "";
    }
  }

  const columnTypeMap = computed(() => {
    const map = new Map<string, string>();
    for (const [id, meta] of nestedColumnMeta.value) {
      map.set(id, meta.columnType);
    }
    return map;
  });

  const gqlFilterRaw = computed<IGraphQLFilter>(() =>
    buildGraphQLFilter(
      filterStates.value,
      columns.value,
      searchValue.value,
      columnTypeMap.value
    )
  );

  let lastGqlFilter: IGraphQLFilter = {};
  const gqlFilter = computed<IGraphQLFilter>(() => {
    const next = gqlFilterRaw.value;
    if (jsonEqual(next, lastGqlFilter)) return lastGqlFilter;
    lastGqlFilter = next;
    return next;
  });

  const activeFilters = computed<ActiveFilter[]>(() => {
    const result: ActiveFilter[] = [];
    for (const [columnId, filterValue] of filterStates.value) {
      const column = columns.value.find((c) => c.id === columnId);
      const label = column?.label || column?.id || columnId;
      const { displayValue, values } = formatFilterValue(filterValue);
      if (displayValue) {
        result.push({ columnId, label, displayValue, values });
      }
    }
    return result;
  });

  const defaultFilterIds = computed(() => computeDefaultFilters(columns.value));

  function getInitialVisibleFilters(): string[] {
    const urlParam = getCurrentQuery()[MG_FILTERS_PARAM];
    if (typeof urlParam === "string" && urlParam.trim()) {
      return urlParam
        .split(",")
        .map((id) => id.trim())
        .filter(Boolean)
        .slice(0, MAX_VISIBLE_FILTERS);
    }
    return [...defaultFilterIds.value];
  }

  const visibleFilterIds = ref<string[]>(getInitialVisibleFilters());
  const userHasCustomized = ref(
    typeof getCurrentQuery()[MG_FILTERS_PARAM] === "string"
  );

  watch(defaultFilterIds, (newDefaults) => {
    if (userHasCustomized.value) return;
    visibleFilterIds.value = [...newDefaults];
  });

  watch(visibleFilterIds, async (newIds) => {
    userHasCustomized.value = true;
    const isDefault = arraysEqual(newIds, defaultFilterIds.value);
    await nextTick();
    if (!urlSyncEnabled || !route || !router) return;
    const currentQuery = { ...getCurrentQuery() };
    if (isDefault) {
      delete currentQuery[MG_FILTERS_PARAM];
    } else {
      currentQuery[MG_FILTERS_PARAM] = newIds.join(",");
    }
    router.replace({ query: currentQuery });
  });

  function toggleFilter(columnId: string) {
    if (visibleFilterIds.value.includes(columnId)) {
      visibleFilterIds.value = visibleFilterIds.value.filter(
        (id) => id !== columnId
      );
      removeFilter(columnId);
    } else if (visibleFilterIds.value.length < MAX_VISIBLE_FILTERS) {
      visibleFilterIds.value = [columnId, ...visibleFilterIds.value];
    }
  }

  function resetFilters() {
    clearFilters();
    userHasCustomized.value = false;
    visibleFilterIds.value = [...defaultFilterIds.value];
  }

  const nestedColumnMeta = ref<
    Map<
      string,
      {
        label: string;
        columnType: string;
        refTableId?: string | null;
        refSchemaId?: string | null;
      }
    >
  >(new Map());

  function registerNestedColumn(
    id: string,
    meta: {
      label: string;
      columnType: string;
      refTableId?: string | null;
      refSchemaId?: string | null;
    }
  ) {
    const next = new Map(nestedColumnMeta.value);
    next.set(id, meta);
    nestedColumnMeta.value = next;
  }

  const countsMap = shallowRef<Map<string, CountedOption[]>>(new Map());
  const loadingSet = shallowRef<Set<string>>(new Set());
  const baseCounts = shallowRef<Map<string, CountedOption[]>>(new Map());

  function mergeWithBaseCounts(
    base: CountedOption[],
    updated: CountedOption[]
  ): CountedOption[] {
    const updatedMap = new Map<string, CountedOption>();
    for (const opt of updated) {
      updatedMap.set(opt.name, opt);
    }
    return base.map((baseOpt) => {
      const match = updatedMap.get(baseOpt.name);
      const children =
        baseOpt.children && baseOpt.children.length > 0
          ? mergeWithBaseCounts(baseOpt.children, match?.children ?? [])
          : undefined;
      return {
        ...baseOpt,
        count: match?.count ?? 0,
        ...(children !== undefined ? { children } : {}),
      };
    });
  }

  function buildCrossFilter(excludeColumnId: string): IGraphQLFilter {
    const crossStates = new Map(filterStates.value);
    crossStates.delete(excludeColumnId);
    return buildGraphQLFilter(crossStates, columns.value, searchValue.value);
  }

  function resolveColumnType(columnId: string): string | null {
    const direct = columns.value.find((c) => c.id === columnId);
    if (direct) return direct.columnType;
    const nested = nestedColumnMeta.value.get(columnId);
    return nested?.columnType ?? null;
  }

  function resolveColumnRefInfo(columnId: string): {
    refTableId: string | null;
    refSchemaId: string | null;
  } {
    const direct = columns.value.find((c) => c.id === columnId);
    if (direct) {
      return {
        refTableId: direct.refTableId ?? null,
        refSchemaId: direct.refSchemaId ?? null,
      };
    }
    const nested = nestedColumnMeta.value.get(columnId);
    return {
      refTableId: nested?.refTableId ?? null,
      refSchemaId: nested?.refSchemaId ?? null,
    };
  }

  async function fetchColumnCounts(columnId: string, useBase = false) {
    const columnType = resolveColumnType(columnId);
    if (!columnType || !isCountableType(columnType)) return;

    const newLoading = new Set(loadingSet.value);
    newLoading.add(columnId);
    loadingSet.value = newLoading;

    try {
      const crossFilter = useBase ? {} : buildCrossFilter(columnId);
      const { refTableId, refSchemaId } = resolveColumnRefInfo(columnId);
      const results = await fetchCounts(
        schemaId,
        tableId,
        columnId,
        columnType,
        crossFilter,
        fetchGraphql,
        refTableId,
        refSchemaId
      );

      let merged = results;
      if (!useBase) {
        const base = baseCounts.value.get(columnId);
        if (base && base.length > 0) {
          merged = mergeWithBaseCounts(base, results);
        }
      }

      const newCounts = new Map(countsMap.value);
      newCounts.set(columnId, merged);
      countsMap.value = newCounts;

      if (useBase) {
        const newBase = new Map(baseCounts.value);
        newBase.set(columnId, results);
        baseCounts.value = newBase;
      }
    } finally {
      const newLoading = new Set(loadingSet.value);
      newLoading.delete(columnId);
      loadingSet.value = newLoading;
    }
  }

  async function fetchAllBaseCounts() {
    const countableIds = visibleFilterIds.value.filter((id) => {
      const colType = resolveColumnType(id);
      return colType && isCountableType(colType);
    });

    await Promise.all(countableIds.map((id) => fetchColumnCounts(id, true)));

    if (!userHasCustomized.value) {
      visibleFilterIds.value = visibleFilterIds.value.filter((id) => {
        const colType = resolveColumnType(id);
        if (!colType || !isCountableType(colType)) return true;
        const base = baseCounts.value.get(id);
        if (!base) return true;
        const totalCount = base.reduce((sum, opt) => sum + opt.count, 0);
        return totalCount > 0;
      });
    }
  }

  const debouncedRefetchCounts = useDebounceFn(async () => {
    const countableIds = visibleFilterIds.value.filter((id) => {
      const colType = resolveColumnType(id);
      return colType && isCountableType(colType);
    });
    await Promise.all(countableIds.map((id) => fetchColumnCounts(id, false)));
  }, debounceMs);

  watch(
    columns,
    async (cols) => {
      if (cols.length > 0 && baseCounts.value.size === 0) {
        await fetchAllBaseCounts();
      }
    },
    { immediate: true }
  );

  watch(filterStates, () => {
    if (baseCounts.value.size > 0) {
      debouncedRefetchCounts();
    }
  });

  watch(searchValue, () => {
    if (baseCounts.value.size > 0) {
      debouncedRefetchCounts();
    }
  });

  watch(nestedColumnMeta, async (meta) => {
    const newCountable: string[] = [];
    for (const [id, m] of meta) {
      if (
        isCountableType(m.columnType) &&
        !baseCounts.value.has(id) &&
        visibleFilterIds.value.includes(id)
      ) {
        newCountable.push(id);
      }
    }
    if (newCountable.length > 0) {
      await Promise.all(newCountable.map((id) => fetchColumnCounts(id, true)));
    }
  });

  function getCountedOptions(columnId: string): ComputedRef<CountedOption[]> {
    return computed(() => countsMap.value.get(columnId) ?? []);
  }

  function isCountLoading(columnId: string): ComputedRef<boolean> {
    return computed(() => loadingSet.value.has(columnId));
  }

  return {
    filterStates,
    searchValue,
    gqlFilter,
    activeFilters,
    setFilter,
    setSearch,
    clearFilters,
    removeFilter,
    columns,
    visibleFilterIds,
    toggleFilter,
    resetFilters,
    getCountedOptions,
    isCountLoading,
    nestedColumnMeta,
    registerNestedColumn,
    schemaId,
    tableId,
  };
}

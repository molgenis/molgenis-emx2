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
import { computeDefaultFilters } from "../utils/filterTypes";
import {
  serializeFiltersToUrl,
  parseFiltersFromUrl,
} from "../utils/filterUrlCodec";
import { fetchCounts, type CountedOption } from "../utils/fetchCounts";
import { isCountableType, isExcludedColumn } from "../utils/filterTypes";
import { BOOL_LABELS } from "../utils/filterConstants";
import { arraysEqual, jsonEqual } from "../utils/compare";
import fetchGraphql from "./fetchGraphql";
import fetchTableMetadata from "./fetchTableMetadata";

export const MG_FILTERS_PARAM = "mg_filters";
export const MG_SEARCH_PARAM = "mg_search";
export const MG_COLLAPSED_PARAM = "mg_collapsed";
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
  defaultFilters?: string[];
  defaultCollapsed?: string[];
  route?: { query: RouteQuery };
  router?: { replace: (opts: Record<string, unknown>) => void };
}

function preserveExternalQueryParams(
  query: RouteQuery,
  cols: IColumn[]
): Record<string, unknown> {
  const columnIds = new Set(cols.map((c) => c.id));
  const preserved: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(query)) {
    if (key === MG_SEARCH_PARAM) continue;
    if (key === MG_COLLAPSED_PARAM) {
      preserved[key] = value;
      continue;
    }
    const firstSegment = key.split(".")[0] ?? key;
    if (columnIds.has(firstSegment)) continue;
    preserved[key] = value;
  }
  return preserved;
}

function filterColumns(rawColumns: IColumn[]): IColumn[] {
  return rawColumns.filter((c) => !isExcludedColumn(c));
}

export function useFilters(
  rawColumns: Ref<IColumn[]>,
  options: UseFiltersOptions
) {
  const { schemaId, tableId, debounceMs = 300 } = options;

  const columns = computed(() => filterColumns(rawColumns.value));
  const urlSync = options.urlSync ?? false;

  if (urlSync && (!options.route || !options.router)) {
    console.warn(
      "useFilters: urlSync is true but route/router were not provided. URL sync disabled."
    );
  }

  const route =
    urlSync && options.route && options.router ? options.route : null;
  const router =
    urlSync && options.route && options.router ? options.router : null;

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
    const preserved = preserveExternalQueryParams(
      getCurrentQuery(),
      columns.value
    );
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
      const column = resolveColumn(columnId);
      const label = column?.label || column?.id || columnId;
      if (!column) {
        const { displayValue, values } = formatFilterValue(filterValue, {});
        if (displayValue)
          result.push({ columnId, label, displayValue, values });
        continue;
      }
      const counted = countsMap.value.get(columnId) ?? null;
      const optionLabels = buildLabelMap(column, counted);
      const { displayValue, values } = formatFilterValue(
        filterValue,
        optionLabels
      );
      if (displayValue) {
        result.push({ columnId, label, displayValue, values });
      }
    }
    return result;
  });

  const defaultFilterIds = computed(() => {
    if (options.defaultFilters && options.defaultFilters.length > 0) {
      return options.defaultFilters;
    }
    return computeDefaultFilters(columns.value);
  });

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
    if (!userHasCustomized.value) return;
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
    userHasCustomized.value = true;
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
    if (urlSyncEnabled && route && router) {
      const currentQuery = { ...getCurrentQuery() };
      delete currentQuery[MG_FILTERS_PARAM];
      router.replace({ query: currentQuery });
    }
  }

  const collapsed = ref(new Set<string>());

  const collapsedIds = computed(() => collapsed.value);

  function applyDefaultCollapse() {
    if (options.defaultCollapsed) {
      collapsed.value = new Set(options.defaultCollapsed);
      return;
    }
    const ids = visibleFilterIds.value;
    const next = new Set<string>();
    ids.forEach((id, index) => {
      if (index >= 5 && !filterStates.value.has(id)) {
        next.add(id);
      }
    });
    collapsed.value = next;
  }

  function persistCollapsed(next: Set<string>) {
    if (!route || !router) return;
    const currentQuery = { ...(route.query as Record<string, unknown>) };
    if (next.size === 0) {
      delete currentQuery[MG_COLLAPSED_PARAM];
    } else {
      currentQuery[MG_COLLAPSED_PARAM] = [...next].join(",");
    }
    router.replace({ query: currentQuery });
  }

  function toggleCollapse(columnId: string) {
    const next = new Set(collapsed.value);
    if (next.has(columnId)) {
      next.delete(columnId);
    } else {
      next.add(columnId);
    }
    collapsed.value = next;
    persistCollapsed(next);
  }

  function isCollapsed(columnId: string): boolean {
    return collapsed.value.has(columnId);
  }

  if (urlSyncEnabled && route) {
    const urlParam = route.query[MG_COLLAPSED_PARAM];
    if (typeof urlParam === "string" && urlParam.trim()) {
      collapsed.value = new Set(
        urlParam
          .split(",")
          .map((s) => s.trim())
          .filter(Boolean)
      );
    } else {
      applyDefaultCollapse();
    }
  } else {
    applyDefaultCollapse();
  }

  const nestedColumnMeta = ref<
    Map<
      string,
      {
        label: string;
        columnType: string;
        refTableId?: string | null;
        refSchemaId?: string | null;
        refLabel?: string | null;
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
      refLabel?: string | null;
    }
  ) {
    const next = new Map(nestedColumnMeta.value);
    next.set(id, meta);
    nestedColumnMeta.value = next;
  }

  async function fetchTableColumns(
    sId: string,
    tId: string
  ): Promise<IColumn[]> {
    try {
      const meta = await fetchTableMetadata(sId, tId);
      return meta.columns ?? [];
    } catch (e) {
      console.error("Failed to fetch table columns:", e);
      return [];
    }
  }

  async function hydrateNestedFilters() {
    const visibleIds = visibleFilterIds.value;
    const alreadyKnown = nestedColumnMeta.value;

    const dottedIds = visibleIds.filter(
      (id) => id.includes(".") && !alreadyKnown.has(id)
    );
    if (dottedIds.length === 0) return;

    for (const id of dottedIds) {
      const segments = id.split(".");
      let currentCols: IColumn[] = rawColumns.value;
      let currentSchemaId = schemaId;
      const labelParts: string[] = [];
      let resolved = true;

      for (let i = 0; i < segments.length; i++) {
        const seg = segments[i]!;
        const col = currentCols.find((c) => c.id === seg);
        if (!col) {
          resolved = false;
          break;
        }
        labelParts.push(col.label || col.id);

        if (i < segments.length - 1) {
          if (!col.refTableId) {
            resolved = false;
            break;
          }
          const nextSchemaId = col.refSchemaId || currentSchemaId;
          currentCols = await fetchTableColumns(nextSchemaId, col.refTableId);
          currentSchemaId = nextSchemaId;
        } else if (resolved) {
          registerNestedColumn(id, {
            label: labelParts.join(" → "),
            columnType: col.columnType,
            refTableId: col.refTableId ?? null,
            refSchemaId: col.refSchemaId ?? null,
          });
        }
      }
    }
  }

  watch(
    visibleFilterIds,
    async (ids) => {
      if (ids.some((id) => id.includes("."))) {
        await hydrateNestedFilters();
      }
    },
    { immediate: true }
  );

  watch(rawColumns, async (cols) => {
    if (cols.length === 0) return;
    const hasUnresolvedDotted = visibleFilterIds.value.some(
      (id) => id.includes(".") && !nestedColumnMeta.value.has(id)
    );
    if (hasUnresolvedDotted) {
      await hydrateNestedFilters();
    }
  });

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

  function resolveColumn(columnId: string): IColumn | null {
    const direct = columns.value.find((c) => c.id === columnId);
    if (direct) return direct;
    const nested = nestedColumnMeta.value.get(columnId);
    if (!nested) return null;
    return {
      id: columnId,
      columnType: nested.columnType,
      label: nested.label,
      refTableId: nested.refTableId ?? undefined,
      refSchemaId: nested.refSchemaId ?? undefined,
    } as IColumn;
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

  function resolveRefLabel(columnId: string): string | null {
    const direct = columns.value.find((c) => c.id === columnId);
    if (direct) return direct.refLabel ?? direct.refLabelDefault ?? null;
    const nested = nestedColumnMeta.value.get(columnId);
    return nested?.refLabel ?? null;
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
      const refLabel = resolveRefLabel(columnId);
      const results = await fetchCounts(
        schemaId,
        tableId,
        columnId,
        columnType,
        crossFilter,
        fetchGraphql,
        refTableId,
        refSchemaId,
        refLabel
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
        const totalCount = base.reduce(
          (sum, opt) => (opt.name === "_null_" ? sum : sum + opt.count),
          0
        );
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
    collapsedIds,
    toggleCollapse,
    isCollapsed,
    hydrateNestedFilters,
  };
}

export function buildLabelMap(
  column: IColumn,
  counted: CountedOption[] | undefined | null
): Record<string, string> {
  if (column.columnType === "BOOL") {
    return { ...BOOL_LABELS };
  }

  if (!counted || counted.length === 0) {
    return {};
  }

  if (
    column.columnType === "ONTOLOGY" ||
    column.columnType === "ONTOLOGY_ARRAY"
  ) {
    return flattenOntologyTree(counted);
  }

  if (column.columnType === "RADIO" || column.columnType === "CHECKBOX") {
    return flattenRefOptions(counted);
  }

  return {};
}

function flattenOntologyTree(nodes: CountedOption[]): Record<string, string> {
  const map: Record<string, string> = {};
  collectOntologyNodes(nodes, map);
  return map;
}

function collectOntologyNodes(
  nodes: CountedOption[],
  map: Record<string, string>
): void {
  for (const node of nodes) {
    map[node.name] = node.label ?? node.name;
    if (node.children && node.children.length > 0) {
      collectOntologyNodes(node.children, map);
    }
  }
}

function flattenRefOptions(options: CountedOption[]): Record<string, string> {
  const map: Record<string, string> = {};
  for (const option of options) {
    if (option.keyObject !== undefined) {
      map[JSON.stringify(option.keyObject)] = option.name;
    } else {
      map[option.name] = option.label ?? option.name;
    }
  }
  return map;
}

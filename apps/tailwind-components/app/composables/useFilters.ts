import {
  ref,
  shallowRef,
  computed,
  watch,
  nextTick,
  type Ref,
  type ComputedRef,
} from "vue";
import { useRoute, useRouter } from "#imports";
import type { IColumn } from "../../../metadata-utils/src/types";
import type {
  ActiveFilter,
  IFilterValue,
  IGraphQLFilter,
  NestedColumnMeta,
} from "../../types/filters";
import { buildGraphQLFilter } from "../utils/buildGqlFilter";
import { formatFilterValue } from "../utils/formatFilterValue";
import {
  computeDefaultFilters,
  isCountableType,
  isExcludedColumn,
} from "../utils/filterTypes";
import { BOOL_LABELS } from "../utils/filterTypes";
import {
  serializeFiltersToUrl,
  parseFiltersFromUrl,
} from "../utils/filterUrlParams";
import { type CountedOption } from "../utils/fetchCounts";
import { arraysEqual } from "../utils/compare";
import fetchTableMetadata from "./fetchTableMetadata";
import { useFilterCounts } from "./useFilterCounts";

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

  const route = useRoute();
  const router = useRouter();

  const urlSyncEnabled = urlSync;

  function getCurrentQuery(): RouteQuery {
    return route.query as RouteQuery;
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

  const nestedColumnMeta = ref<Map<string, NestedColumnMeta>>(new Map());

  const columnTypeMap = computed(() => {
    const map = new Map<string, string>();
    for (const [id, meta] of nestedColumnMeta.value) {
      map.set(id, meta.columnType);
    }
    return map;
  });

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
      refLabel: nested.refLabel ?? undefined,
    } as IColumn;
  }

  const counts = useFilterCounts({
    schemaId,
    tableId,
    debounceMs,
    columns,
    visibleFilterIds,
    filterStates,
    searchValue,
    nestedColumnMeta,
    resolveColumn,
    columnTypeMap,
  });

  function pruneVisibleByBaseCount() {
    if (userHasCustomized.value) return;
    visibleFilterIds.value = visibleFilterIds.value.filter((id) => {
      const colType = resolveColumn(id)?.columnType ?? null;
      if (!colType || !isCountableType(colType)) return true;
      const base = counts.baseCounts.value.get(id);
      if (!base) return true;
      const totalCount = base.reduce(
        (sum, opt) => (opt.name === "_null_" ? sum : sum + opt.count),
        0
      );
      return totalCount > 0;
    });
  }

  watch(
    columns,
    async (cols) => {
      if (cols.length > 0 && counts.baseCounts.value.size === 0) {
        if (!userHasCustomized.value && visibleFilterIds.value.length === 0) {
          visibleFilterIds.value = [...defaultFilterIds.value];
        }
        await Promise.all(
          visibleFilterIds.value
            .filter((id) => {
              const colType = resolveColumn(id)?.columnType ?? null;
              return colType && isCountableType(colType);
            })
            .map((id) => counts.fetchColumnCounts(id, true))
        );
        pruneVisibleByBaseCount();
        const hasInitialFilters =
          filterStates.value.size > 0 || (searchValue.value ?? "").length > 0;
        if (hasInitialFilters) {
          counts.debouncedRefetchCounts();
        }
      }
    },
    { immediate: true }
  );

  function updateUrl(newFilters: Map<string, IFilterValue>, newSearch: string) {
    if (!urlSyncEnabled) return;
    const filterParams = serializeFiltersToUrl(
      newFilters,
      newSearch,
      columns.value
    );
    const preserved = preserveExternalQueryParams(
      getCurrentQuery(),
      columns.value
    );
    if (userHasCustomized.value) {
      preserved[MG_FILTERS_PARAM] = visibleFilterIds.value.join(",");
    } else {
      const mgFiltersParam = getCurrentQuery()[MG_FILTERS_PARAM];
      if (mgFiltersParam !== undefined) {
        preserved[MG_FILTERS_PARAM] = mgFiltersParam;
      }
    }
    router.replace({
      query: { ...preserved, ...filterParams } as Record<string, string>,
    });
  }

  function commit(newFilters: Map<string, IFilterValue>, newSearch: string) {
    if (urlSyncEnabled) {
      updateUrl(newFilters, newSearch);
    } else {
      filterStatesRef.value = newFilters;
      searchValueRef.value = newSearch;
    }
  }

  function setFilter(columnId: string, value: IFilterValue | null) {
    const current = new Map(filterStates.value);
    if (value === null) {
      current.delete(columnId);
    } else {
      current.set(columnId, value);
    }
    commit(current, searchValue.value);
  }

  function removeFilter(columnId: string) {
    setFilter(columnId, null);
  }

  function setSearch(value: string) {
    commit(filterStates.value, value);
  }

  function clearFilters() {
    commit(new Map(), "");
  }

  function toggleFilter(columnId: string) {
    userHasCustomized.value = true;
    if (visibleFilterIds.value.includes(columnId)) {
      visibleFilterIds.value = visibleFilterIds.value.filter(
        (id) => id !== columnId
      );
      removeFilter(columnId);
    } else if (visibleFilterIds.value.length < MAX_VISIBLE_FILTERS) {
      visibleFilterIds.value = [...visibleFilterIds.value, columnId];
    }
  }

  function resetFilters() {
    clearFilters();
    userHasCustomized.value = false;
    visibleFilterIds.value = [...defaultFilterIds.value];
    pruneVisibleByBaseCount();
    if (urlSyncEnabled) {
      const currentQuery = { ...getCurrentQuery() };
      delete currentQuery[MG_FILTERS_PARAM];
      router.replace({ query: currentQuery });
    }
  }

  const gqlFilter = computed<IGraphQLFilter>(() =>
    buildGraphQLFilter(
      filterStates.value,
      columns.value,
      searchValue.value,
      columnTypeMap.value
    )
  );

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
      const counted = counts.baseCounts.value.get(columnId) ?? null;
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
    if (!urlSyncEnabled) return;
    const currentQuery = { ...(route.query as Record<string, string>) };
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

  if (urlSyncEnabled) {
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

  function registerNestedColumn(id: string, meta: NestedColumnMeta) {
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

  watch(defaultFilterIds, (newDefaults) => {
    if (userHasCustomized.value) return;
    visibleFilterIds.value = [...newDefaults];
  });

  watch(visibleFilterIds, async (newIds) => {
    if (!userHasCustomized.value) return;
    const isDefault = arraysEqual(newIds, defaultFilterIds.value);
    await nextTick();
    if (!urlSyncEnabled) return;
    const currentQuery = { ...getCurrentQuery() };
    if (isDefault || newIds.length === 0) {
      delete currentQuery[MG_FILTERS_PARAM];
    } else {
      currentQuery[MG_FILTERS_PARAM] = newIds.join(",");
    }
    router.replace({ query: currentQuery });
  });

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
    getCountedOptions: counts.getCountedOptions,
    isCountLoading: counts.isCountLoading,
    isSaturated: counts.isSaturated,
    nestedColumnMeta,
    registerNestedColumn,
    schemaId,
    tableId,
    toggleCollapse,
    isCollapsed,
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

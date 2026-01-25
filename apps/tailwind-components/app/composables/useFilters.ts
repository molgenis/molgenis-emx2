import {
  ref,
  computed,
  watch,
  type Ref,
  onMounted,
  getCurrentInstance,
} from "vue";
import { useDebounceFn } from "@vueuse/core";
import type { IColumn } from "../../../metadata-utils/src/types";
import type { IFilterValue, IGraphQLFilter } from "../../types/filters";
import { buildGraphQLFilter } from "../utils/buildFilter";

export interface UseFiltersOptions {
  debounceMs?: number;
  urlSync?: boolean;
  route?: { query: Record<string, string | string[] | undefined> };
  router?: { replace: (opts: { query: Record<string, unknown> }) => void };
}

/**
 * URL Filter Format:
 *
 * Simple values:
 *   ?name=John              → like filter (strings)
 *   ?age=25                 → equals filter (numbers)
 *   ?birth=2024-01-01       → equals filter (dates)
 *
 * Multi-value (pipe separator - avoids comma conflicts in data):
 *   ?name=John|Jane         → in filter
 *   ?category=Cat1|Cat2     → in filter for refs (primary key values)
 *
 * Ranges (double-dot separator):
 *   ?age=18..65             → between filter
 *   ?age=18..               → >= 18
 *   ?age=..65               → <= 65
 *   ?birth=2024-01-01..2024-12-31  → date range
 *
 * Null checks:
 *   ?name=null              → is null
 *   ?name=!null             → is not null
 *
 * Reserved:
 *   mg_search               → global search
 *   mg_*                    → reserved for system params (pagination, etc.)
 *
 * Future (not yet implemented):
 *   ?name=eq:John           → explicit equals operator
 *   ?name=like:John         → explicit like operator
 *   ?age=gte:18             → greater than or equal
 *   ?parent.name=John       → path query for related tables
 */

const REF_TYPES = ["REF", "REF_ARRAY", "REFBACK", "ONTOLOGY", "ONTOLOGY_ARRAY"];
const RANGE_TYPES = ["INT", "LONG", "DECIMAL", "DATE", "DATETIME"];
const MULTI_VALUE_SEPARATOR = "|";
const RESERVED_PREFIX = "mg_";
const SEARCH_PARAM = "mg_search";

export function serializeFilterValue(
  value: IFilterValue,
  column: IColumn
): string | null {
  const { operator, value: val } = value;
  if (val === null || val === undefined) return null;

  switch (operator) {
    case "between":
      if (Array.isArray(val) && val.length === 2) {
        const [min, max] = val;
        const minStr = min ?? "";
        const maxStr = max ?? "";
        if (minStr === "" && maxStr === "") return null;
        return `${minStr}..${maxStr}`;
      }
      return null;

    case "in":
      if (Array.isArray(val)) {
        // For refs with objects, extract primary key (usually 'name' or first key)
        if (val.length && typeof val[0] === "object") {
          const keys = val.map((v) => {
            if (typeof v === "object" && v !== null) {
              // Use 'name' if available, otherwise first key value
              return v.name ?? Object.values(v)[0];
            }
            return String(v);
          });
          return keys.join(MULTI_VALUE_SEPARATOR);
        }
        return val.join(MULTI_VALUE_SEPARATOR);
      }
      return String(val);

    case "notNull":
      return "!null";

    case "isNull":
      return "null";

    case "like":
    case "equals":
    default:
      if (Array.isArray(val)) {
        if (val.length && typeof val[0] === "object") {
          const keys = val.map((v) => {
            if (typeof v === "object" && v !== null) {
              return v.name ?? Object.values(v)[0];
            }
            return String(v);
          });
          return keys.join(MULTI_VALUE_SEPARATOR);
        }
        return val.join(MULTI_VALUE_SEPARATOR);
      }
      return String(val);
  }
}

export function parseFilterValue(
  urlValue: string,
  column: IColumn
): IFilterValue | null {
  if (!urlValue) return null;

  // Special null handling
  if (urlValue === "null") {
    return { operator: "isNull", value: true };
  }
  if (urlValue === "!null") {
    return { operator: "notNull", value: true };
  }

  const columnType = column.columnType;

  // REF/ONTOLOGY types - parse as primary key values
  if (REF_TYPES.includes(columnType)) {
    if (urlValue.includes(MULTI_VALUE_SEPARATOR)) {
      // Multi-value: convert to array of objects with 'name' key
      const values = urlValue.split(MULTI_VALUE_SEPARATOR);
      return { operator: "in", value: values.map((v) => ({ name: v })) };
    }
    // Single value
    return { operator: "equals", value: { name: urlValue } };
  }

  // Numeric/Date types - check for range
  if (RANGE_TYPES.includes(columnType)) {
    if (urlValue.includes("..")) {
      const [minStr, maxStr] = urlValue.split("..");
      const isDate = columnType.includes("DATE");
      return {
        operator: "between",
        value: [
          minStr === "" ? null : isDate ? minStr : Number(minStr),
          maxStr === "" ? null : isDate ? maxStr : Number(maxStr),
        ],
      };
    }
    // Single value equals
    const isDate = columnType.includes("DATE");
    return {
      operator: "equals",
      value: isDate ? urlValue : Number(urlValue),
    };
  }

  // String types - pipe means multi-value
  if (urlValue.includes(MULTI_VALUE_SEPARATOR)) {
    return { operator: "in", value: urlValue.split(MULTI_VALUE_SEPARATOR) };
  }

  // Default: like for strings
  return { operator: "like", value: urlValue };
}

export function serializeFiltersToUrl(
  filterStates: Map<string, IFilterValue>,
  searchValue: string,
  columns: IColumn[]
): Record<string, string> {
  const params: Record<string, string> = {};

  if (searchValue.trim()) {
    params[SEARCH_PARAM] = searchValue.trim();
  }

  for (const [columnId, value] of filterStates) {
    const column = columns.find((c) => c.id === columnId);
    if (!column) continue;

    const serialized = serializeFilterValue(value, column);
    if (serialized !== null) {
      params[columnId] = serialized;
    }
  }

  return params;
}

export function parseFiltersFromUrl(
  query: Record<string, string | string[] | undefined>,
  columns: IColumn[]
): { filters: Map<string, IFilterValue>; search: string } {
  const filters = new Map<string, IFilterValue>();
  let search = "";

  for (const [key, value] of Object.entries(query)) {
    if (key === SEARCH_PARAM && typeof value === "string") {
      search = value;
      continue;
    }

    // Skip reserved params (mg_*)
    if (key.startsWith(RESERVED_PREFIX)) continue;

    const column = columns.find((c) => c.id === key);
    if (!column || typeof value !== "string") continue;

    const filterValue = parseFilterValue(value, column);
    if (filterValue) {
      filters.set(key, filterValue);
    }
  }

  return { filters, search };
}

export function useFilters(
  columns: Ref<IColumn[]>,
  options?: UseFiltersOptions
) {
  const filterStates = ref<Map<string, IFilterValue>>(new Map());
  const searchValue = ref("");

  // _gqlFilter is always current, gqlFilter is debounced for API calls
  const _gqlFilter = computed(() =>
    buildGraphQLFilter(filterStates.value, columns.value, searchValue.value)
  );
  const gqlFilter = ref(_gqlFilter.value);
  const updateGqlFilter = useDebounceFn(() => {
    gqlFilter.value = _gqlFilter.value;
  }, options?.debounceMs ?? 300);

  // URL sync setup
  const urlSyncEnabled = !!options?.urlSync;
  const updatingFromUrl = ref(false);

  let route: { query: Record<string, string | string[] | undefined> } | null =
    null;
  let router: {
    replace: (opts: { query: Record<string, unknown> }) => void;
  } | null = null;

  if (urlSyncEnabled) {
    route = options?.route ?? null;
    router = options?.router ?? null;
    if (!route || !router) {
      try {
        const nuxt = require("#app/composables/router");
        route = route ?? nuxt.useRoute();
        router = router ?? nuxt.useRouter();
      } catch {
        // Nuxt not available, URL sync disabled silently
      }
    }
  }

  function syncToUrl() {
    if (!router || !route || updatingFromUrl.value) return;
    const params = serializeFiltersToUrl(
      filterStates.value,
      searchValue.value,
      columns.value
    );

    // Preserve reserved query params (mg_*) except mg_search
    const newQuery: Record<string, unknown> = {};
    for (const [key, value] of Object.entries(route.query)) {
      if (key.startsWith(RESERVED_PREFIX) && key !== SEARCH_PARAM) {
        newQuery[key] = value;
      }
    }

    // Add filter params
    for (const [key, value] of Object.entries(params)) {
      newQuery[key] = value;
    }

    router.replace({ query: newQuery });
  }

  function initFromUrl() {
    if (!route?.query) return;

    updatingFromUrl.value = true;
    try {
      const { filters, search } = parseFiltersFromUrl(
        route.query,
        columns.value
      );
      filterStates.value = filters;
      searchValue.value = search;
      gqlFilter.value = _gqlFilter.value;
    } finally {
      updatingFromUrl.value = false;
    }
  }

  // Watch filterStates for external changes (e.g., v-model binding from Sidebar)
  watch(
    filterStates,
    () => {
      updateGqlFilter();
    },
    { deep: true }
  );

  if (urlSyncEnabled && route) {
    // Watch for external URL changes (browser back/forward)
    watch(
      () => JSON.stringify(route!.query),
      () => {
        if (updatingFromUrl.value) return;
        updatingFromUrl.value = true;
        try {
          const { filters, search } = parseFiltersFromUrl(
            route!.query,
            columns.value
          );
          filterStates.value = filters;
          searchValue.value = search;
          gqlFilter.value = _gqlFilter.value;
        } finally {
          updatingFromUrl.value = false;
        }
      }
    );

    // Sync to URL after gqlFilter updates (debounced)
    watch(gqlFilter, () => syncToUrl());

    // Init from URL on mount (or immediately if no component instance)
    if (getCurrentInstance()) {
      onMounted(() => initFromUrl());
    } else {
      // No component instance, init immediately (e.g., in tests)
      initFromUrl();
    }
  }

  function setFilter(columnId: string, value: IFilterValue | null) {
    if (value === null) {
      filterStates.value.delete(columnId);
    } else {
      filterStates.value.set(columnId, value);
    }
    filterStates.value = new Map(filterStates.value);
    updateGqlFilter();
  }

  function setSearch(value: string) {
    searchValue.value = value;
    updateGqlFilter();
  }

  function clearFilters() {
    filterStates.value = new Map();
    searchValue.value = "";
    updateGqlFilter();
  }

  function removeFilter(columnId: string) {
    filterStates.value.delete(columnId);
    filterStates.value = new Map(filterStates.value);
    updateGqlFilter();
  }

  return {
    filterStates,
    searchValue,
    gqlFilter,
    setFilter,
    setSearch,
    clearFilters,
    removeFilter,
  };
}

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
import type { IFilterValue } from "../../types/filters";
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

const REF_TYPES = [
  "REF",
  "REF_ARRAY",
  "REFBACK",
  "ONTOLOGY",
  "ONTOLOGY_ARRAY",
  "SELECT",
  "MULTISELECT",
  "RADIO",
  "CHECKBOX",
];

function extractStringKey(v: unknown, depth = 0): string {
  if (depth > 10) return String(v);
  if (typeof v === "string") return v;
  if (typeof v !== "object" || v === null) return String(v);
  const obj = v as Record<string, unknown>;
  if (typeof obj.name === "string") return obj.name;
  const firstValue = Object.values(obj)[0];
  return extractStringKey(firstValue, depth + 1);
}
const RANGE_TYPES = ["INT", "LONG", "DECIMAL", "DATE", "DATETIME"];
const MULTI_VALUE_SEPARATOR = "|";
const RESERVED_PREFIX = "mg_";
const SEARCH_PARAM = "mg_search";

export function serializeFilterValue(value: IFilterValue): string | null {
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
        if (val.length && typeof val[0] === "object") {
          const primaryKeys = val.map((v) => extractStringKey(v));
          return primaryKeys.join(MULTI_VALUE_SEPARATOR);
        }
        return val.join(MULTI_VALUE_SEPARATOR);
      }
      if (typeof val === "object" && val !== null) {
        return extractStringKey(val);
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
          const keys = val.map((v) => extractStringKey(v));
          return keys.join(MULTI_VALUE_SEPARATOR);
        }
        return val.join(MULTI_VALUE_SEPARATOR);
      }
      if (typeof val === "object" && val !== null) {
        return extractStringKey(val);
      }
      return String(val);
  }
}

function extractRefField(value: IFilterValue): string {
  const val = value.value;
  if (Array.isArray(val) && val.length && typeof val[0] === "object") {
    return Object.keys(val[0])[0] ?? "name";
  }
  if (typeof val === "object" && val !== null) {
    return Object.keys(val)[0] ?? "name";
  }
  return "name";
}

export function parseFilterValue(
  urlValue: string,
  column: IColumn,
  refField: string | null = null
): IFilterValue | null {
  if (!urlValue) return null;

  if (urlValue === "null") {
    return { operator: "isNull", value: true };
  }
  if (urlValue === "!null") {
    return { operator: "notNull", value: true };
  }

  const columnType = column.columnType;

  if (REF_TYPES.includes(columnType)) {
    const field = refField ?? "name";
    const isArrayType = columnType.endsWith("_ARRAY");
    if (urlValue.includes(MULTI_VALUE_SEPARATOR)) {
      const values = urlValue.split(MULTI_VALUE_SEPARATOR);
      return { operator: "in", value: values.map((v) => ({ [field]: v })) };
    }
    const refValue = { [field]: urlValue };
    return {
      operator: isArrayType ? "in" : "equals",
      value: isArrayType ? [refValue] : refValue,
    };
  }

  if (RANGE_TYPES.includes(columnType)) {
    if (urlValue.includes("..")) {
      const [minStr, maxStr] = urlValue.split("..");
      const isDateType = columnType.includes("DATE");
      return {
        operator: "between",
        value: [
          minStr === "" ? null : isDateType ? minStr : Number(minStr),
          maxStr === "" ? null : isDateType ? maxStr : Number(maxStr),
        ],
      };
    }
    const isDateType = columnType.includes("DATE");
    return {
      operator: "equals",
      value: isDateType ? urlValue : Number(urlValue),
    };
  }

  if (urlValue.includes(MULTI_VALUE_SEPARATOR)) {
    return { operator: "in", value: urlValue.split(MULTI_VALUE_SEPARATOR) };
  }

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

  for (const [key, value] of filterStates) {
    const firstSegment = key.split(".")[0];
    const column = columns.find((c) => c.id === firstSegment);
    if (!column) continue;

    const serialized = serializeFilterValue(value);
    if (serialized !== null) {
      if (key.includes(".")) {
        const val = value.value;
        const isRefLikeValue =
          (val !== null && typeof val === "object" && !Array.isArray(val)) ||
          (Array.isArray(val) && val.length > 0 && typeof val[0] === "object");
        if (isRefLikeValue) {
          const refField = extractRefField(value);
          params[`${key}.${refField}`] = serialized;
        } else {
          params[key] = serialized;
        }
      } else if (REF_TYPES.includes(column.columnType)) {
        const refField = extractRefField(value);
        params[`${key}.${refField}`] = serialized;
      } else {
        params[key] = serialized;
      }
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

    if (key.startsWith(RESERVED_PREFIX)) continue;

    const segments = key.split(".");
    const firstSegment = segments[0]!;

    const column = columns.find((c) => c.id === firstSegment);
    if (!column || typeof value !== "string") continue;

    let filterKey: string;
    let refField: string | null;

    if (!REF_TYPES.includes(column.columnType) || segments.length === 1) {
      filterKey = key;
      refField = null;
    } else if (segments.length === 2) {
      filterKey = firstSegment;
      refField = segments[1]!;
    } else {
      filterKey = segments.slice(0, -1).join(".");
      refField = segments[segments.length - 1]!;
    }

    const filterValue = parseFilterValue(value, column, refField);
    if (filterValue) {
      filters.set(filterKey, filterValue);
    }
  }

  return { filters, search };
}

export function useFilters(
  columns: Ref<IColumn[]>,
  options?: UseFiltersOptions
) {
  let urlSyncEnabled = !!options?.urlSync;
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
        /* Nuxt not available */
      }
    }
    if (!route || !router) {
      urlSyncEnabled = false;
    }
  }

  const parsedUrlState = computed(() => {
    if (!urlSyncEnabled || !route?.query) {
      return { filters: new Map<string, IFilterValue>(), search: "" };
    }
    return parseFiltersFromUrl(route.query, columns.value);
  });

  const filterStatesFromUrl = computed(() => parsedUrlState.value.filters);
  const searchValueFromUrl = computed(() => parsedUrlState.value.search);

  const filterStatesRef = ref<Map<string, IFilterValue>>(new Map());
  const searchValueRef = ref("");

  const actualSearchValue = urlSyncEnabled
    ? searchValueFromUrl
    : searchValueRef;

  const actualFilterStates = computed({
    get: () =>
      urlSyncEnabled ? filterStatesFromUrl.value : filterStatesRef.value,
    set: (newFilters: Map<string, IFilterValue>) => {
      if (urlSyncEnabled) {
        if (!router || !route) return;
        const params = serializeFiltersToUrl(
          newFilters,
          actualSearchValue.value,
          columns.value
        );
        const preservedParams: Record<string, unknown> = {};
        for (const [key, value] of Object.entries(route.query)) {
          if (key.startsWith(RESERVED_PREFIX) && key !== SEARCH_PARAM) {
            preservedParams[key] = value;
          }
        }
        router.replace({ query: { ...preservedParams, ...params } });
      } else {
        filterStatesRef.value = newFilters;
      }
    },
  });

  const _gqlFilter = computed(() =>
    buildGraphQLFilter(
      actualFilterStates.value,
      columns.value,
      actualSearchValue.value
    )
  );
  const gqlFilter = ref(_gqlFilter.value);
  const updateGqlFilter = useDebounceFn(() => {
    gqlFilter.value = _gqlFilter.value;
  }, options?.debounceMs ?? 300);

  watch(
    () => columns.value.length,
    (newLen, oldLen) => {
      if (oldLen === 0 && newLen > 0) {
        updateGqlFilter();
      }
    }
  );

  if (urlSyncEnabled) {
    watch(
      () => route?.query,
      () => {
        updateGqlFilter();
      },
      { deep: true }
    );

    if (getCurrentInstance()) {
      onMounted(() => {
        gqlFilter.value = _gqlFilter.value;
      });
    } else {
      gqlFilter.value = _gqlFilter.value;
    }
  } else {
    watch(
      filterStatesRef,
      () => {
        updateGqlFilter();
      },
      { deep: true, flush: "sync" }
    );
    watch(searchValueRef, () => {
      updateGqlFilter();
    });
  }

  function updateUrl(filters: Map<string, IFilterValue>, search: string) {
    if (!router || !route) return;
    const params = serializeFiltersToUrl(filters, search, columns.value);
    const preservedParams: Record<string, unknown> = {};
    for (const [key, value] of Object.entries(route.query)) {
      if (key.startsWith(RESERVED_PREFIX) && key !== SEARCH_PARAM) {
        preservedParams[key] = value;
      }
    }
    router.replace({ query: { ...preservedParams, ...params } });
  }

  function setFilter(columnId: string, value: IFilterValue | null) {
    const newFilters = new Map(actualFilterStates.value);
    if (value === null) {
      newFilters.delete(columnId);
    } else {
      newFilters.set(columnId, value);
    }
    actualFilterStates.value = newFilters;
  }

  function setSearch(value: string) {
    if (urlSyncEnabled) {
      updateUrl(actualFilterStates.value, value);
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

  function removeFilter(columnId: string) {
    setFilter(columnId, null);
  }

  return {
    filterStates: actualFilterStates,
    searchValue: actualSearchValue,
    gqlFilter,
    setFilter,
    setSearch,
    clearFilters,
    removeFilter,
  };
}

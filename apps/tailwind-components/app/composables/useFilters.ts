import {
  ref,
  computed,
  watch,
  type Ref,
  onMounted,
  getCurrentInstance,
  nextTick,
} from "vue";
import { useDebounceFn } from "@vueuse/core";
import type { IColumn, IRow } from "../../../metadata-utils/src/types";
import type {
  ActiveFilter,
  IFilterValue,
  UseFilters,
} from "../../types/filters";
import { buildGraphQLFilter } from "../utils/buildFilter";
import { formatFilterValue } from "../utils/formatFilterValue";
import { computeDefaultFilters } from "../utils/computeDefaultFilters";
import { MAX_NESTING_DEPTH } from "../utils/filterConstants";
import {
  createCountFetcher,
  type ICountFetcher,
} from "../utils/createCountFetcher";
import { getPrimaryKey } from "../utils/getPrimaryKey";
import fetchTableMetadata from "./fetchTableMetadata";
import type { FilterValue } from "../../types/filters";

export interface UseFiltersOptions {
  debounceMs?: number;
  urlSync?: boolean;
  route?: {
    query: Record<
      string,
      string | string[] | (string | null)[] | null | undefined
    >;
  };
  router?: { replace: (opts: Record<string, unknown>) => void };
  schemaId?: string;
  tableId?: string;
}

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
  if (v === undefined) return "";
  if (depth > 10) return String(v);
  if (typeof v === "string") return v;
  if (typeof v !== "object" || v === null) return String(v);
  const obj = v as Record<string, unknown>;
  if (typeof obj.name === "string") return obj.name;
  const values = Object.values(obj);
  if (values.length === 0) return "";
  return extractStringKey(values[0], depth + 1);
}
const RANGE_TYPES = [
  "INT",
  "LONG",
  "DECIMAL",
  "DATE",
  "DATETIME",
  "NON_NEGATIVE_INT",
  "INT_ARRAY",
  "LONG_ARRAY",
  "DECIMAL_ARRAY",
  "DATE_ARRAY",
  "NON_NEGATIVE_INT_ARRAY",
  "DATETIME_ARRAY",
];
const MULTI_VALUE_SEPARATOR = "|";
const RESERVED_PREFIX = "mg_";
const SEARCH_PARAM = "mg_search";
export const MG_FILTERS_PARAM = "mg_filters";
export const MAX_VISIBLE_FILTERS = 25;

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

    case "notNull":
      return "!null";

    case "isNull":
      return "null";

    case "like":
      return String(val);

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
  if (
    Array.isArray(val) &&
    val.length &&
    typeof val[0] === "object" &&
    val[0] !== null
  ) {
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
    if (urlValue.includes(MULTI_VALUE_SEPARATOR)) {
      const values = urlValue.split(MULTI_VALUE_SEPARATOR);
      return { operator: "equals", value: values.map((v) => ({ [field]: v })) };
    }
    const refValue = { [field]: urlValue };
    return { operator: "equals", value: [refValue] };
  }

  if (RANGE_TYPES.includes(columnType)) {
    if (urlValue.includes("..")) {
      const [minStr = "", maxStr = ""] = urlValue.split("..");
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

  const STRING_TYPES = [
    "STRING",
    "TEXT",
    "EMAIL",
    "HYPERLINK",
    "AUTO_ID",
    "JSON",
    "STRING_ARRAY",
    "TEXT_ARRAY",
    "EMAIL_ARRAY",
    "HYPERLINK_ARRAY",
  ];

  if (columnType === "UUID" || columnType === "UUID_ARRAY") {
    return { operator: "equals", value: urlValue };
  }

  if (STRING_TYPES.includes(columnType)) {
    return { operator: "like", value: urlValue };
  }

  if (urlValue.includes(MULTI_VALUE_SEPARATOR)) {
    return { operator: "equals", value: urlValue.split(MULTI_VALUE_SEPARATOR) };
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
  query: Record<
    string,
    string | string[] | (string | null)[] | null | undefined
  >,
  columns: IColumn[]
): { filters: Map<string, IFilterValue>; search: string } {
  const filters = new Map<string, IFilterValue>();
  let search = "";

  for (const [key, value] of Object.entries(query)) {
    if (key === SEARCH_PARAM && typeof value === "string") {
      search = value.replace(/\+/g, " ");
      continue;
    }

    if (key.startsWith(RESERVED_PREFIX)) continue;

    const segments = key.split(".");
    const firstSegment = segments[0]!;

    const column = columns.find((c) => c.id === firstSegment);
    if (!column || typeof value !== "string") continue;
    const decodedValue = value.replace(/\+/g, " ");

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

    const filterValue = parseFilterValue(decodedValue, column, refField);
    if (filterValue) {
      filters.set(filterKey, filterValue);
    }
  }

  return { filters, search };
}

function getNonFilterParams(
  query: Record<string, unknown>,
  cols: IColumn[]
): Record<string, unknown> {
  const columnIds = new Set(cols.map((c) => c.id));
  const preserved: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(query)) {
    if (key === SEARCH_PARAM) continue;
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

const REF_COLUMN_UNFILTERABLE = ["HEADING", "SECTION"];

export function useFilters(
  columns: Ref<IColumn[]>,
  options?: UseFiltersOptions
) {
  let urlSyncEnabled = !!options?.urlSync;
  let route: {
    query: Record<
      string,
      string | string[] | (string | null)[] | null | undefined
    >;
  } | null = null;
  let router: {
    replace: (opts: Record<string, unknown>) => void;
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

  const filterStatesRef = ref<Map<string, IFilterValue>>(new Map());
  const searchValueRef = ref("");

  const actualSearchValue = urlSyncEnabled
    ? computed(() => parsedUrlState.value.search)
    : searchValueRef;

  const actualFilterStates = computed({
    get: () =>
      urlSyncEnabled ? parsedUrlState.value.filters : filterStatesRef.value,
    set: (newFilters: Map<string, IFilterValue>) => {
      if (urlSyncEnabled) {
        if (!router || !route) return;
        const params = serializeFiltersToUrl(
          newFilters,
          actualSearchValue.value,
          columns.value
        );
        const preservedParams = getNonFilterParams(route.query, columns.value);
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
    const newFilter = _gqlFilter.value;
    if (JSON.stringify(newFilter) !== JSON.stringify(gqlFilter.value)) {
      gqlFilter.value = newFilter;
    }
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
    const preservedParams = getNonFilterParams(route.query, columns.value);
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

  const activeFilters = computed<ActiveFilter[]>(() => {
    const result: ActiveFilter[] = [];
    for (const [columnId, filterValue] of actualFilterStates.value) {
      const column = columns.value.find((c) => c.id === columnId);
      const label = column ? column.label || column.id : columnId;
      const { displayValue, values } = formatFilterValue(filterValue);
      if (displayValue) {
        result.push({ columnId, label, displayValue, values });
      }
    }
    return result;
  });

  // --- Visibility ---

  const schemaId = options?.schemaId ?? "";
  const tableId = options?.tableId ?? "";

  const defaultFilterIds = computed(() => computeDefaultFilters(columns.value));

  function getInitialVisibleFilters(): string[] {
    const urlParam = route?.query?.[MG_FILTERS_PARAM];
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
    typeof route?.query?.[MG_FILTERS_PARAM] === "string"
  );

  watch(visibleFilterIds, async (newIds) => {
    userHasCustomized.value = true;
    const isDefault = arraysEqual(newIds, defaultFilterIds.value);
    await nextTick();
    if (!router || !route) return;
    const currentQuery = { ...route.query };

    if (isDefault) {
      delete currentQuery[MG_FILTERS_PARAM];
    } else {
      currentQuery[MG_FILTERS_PARAM] = newIds.join(",");
    }

    router.replace({ query: currentQuery });
  });

  watch(defaultFilterIds, (newDefaults) => {
    if (userHasCustomized.value) return;
    visibleFilterIds.value = [...newDefaults];
  });

  function toggleFilter(columnId: string) {
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
    const newDefaults = [...defaultFilterIds.value];
    const removedIds = visibleFilterIds.value.filter(
      (id) => !newDefaults.includes(id)
    );
    for (const id of removedIds) {
      removeFilter(id);
    }
    userHasCustomized.value = false;
    visibleFilterIds.value = newDefaults;
  }

  // --- Ref column resolution ---

  const refColumnsCache = ref<Map<string, IColumn[]>>(new Map());
  const refLoadingKeys = ref<Set<string>>(new Set());

  async function loadRefColumns(parentPath: string, column: IColumn) {
    if (
      refColumnsCache.value.has(parentPath) ||
      refLoadingKeys.value.has(parentPath)
    )
      return;
    if (!column.refTableId) return;

    refLoadingKeys.value.add(parentPath);
    const refSchemaId = column.refSchemaId || schemaId;
    try {
      const meta = await fetchTableMetadata(refSchemaId, column.refTableId);
      refColumnsCache.value.set(
        parentPath,
        meta.columns.filter(
          (c) =>
            !c.id.startsWith("mg_") &&
            !REF_COLUMN_UNFILTERABLE.includes(c.columnType)
        )
      );
    } catch {
    } finally {
      refLoadingKeys.value.delete(parentPath);
    }
  }

  async function loadRefColumnsForPath(fullPath: string) {
    const segments = fullPath.split(".");
    let currentColumns: IColumn[] = columns.value;
    let currentSchemaId = schemaId;

    for (
      let depth = 0;
      depth < segments.length && depth < MAX_NESTING_DEPTH;
      depth++
    ) {
      const segment = segments[depth]!;
      const pathSoFar = segments.slice(0, depth + 1).join(".");

      const column = currentColumns.find((c) => c.id === segment);
      if (!column || !column.refTableId) return;

      if (!refColumnsCache.value.has(pathSoFar)) {
        const refSchemaId = column.refSchemaId || currentSchemaId;
        try {
          const tableMetadata = await fetchTableMetadata(
            refSchemaId,
            column.refTableId
          );
          refColumnsCache.value.set(
            pathSoFar,
            tableMetadata.columns.filter(
              (col) =>
                !col.id.startsWith("mg_") &&
                !REF_COLUMN_UNFILTERABLE.includes(col.columnType)
            )
          );
        } catch {
          return;
        }
      }

      currentColumns = refColumnsCache.value.get(pathSoFar) || [];
      currentSchemaId = column.refSchemaId || currentSchemaId;
    }
  }

  async function loadMissingRefColumns(ids: string[]) {
    for (const id of ids) {
      if (!id.includes(".")) continue;
      const segments = id.split(".");
      for (let depth = 0; depth < segments.length - 1; depth++) {
        const pathSoFar = segments.slice(0, depth + 1).join(".");
        if (!refColumnsCache.value.has(pathSoFar)) {
          await loadRefColumnsForPath(id);
          break;
        }
      }
    }
  }

  watch(visibleFilterIds, loadMissingRefColumns, { immediate: true });

  watch(columns, () => {
    loadMissingRefColumns(visibleFilterIds.value);
  });

  function getRefColumns(path: string): IColumn[] {
    return refColumnsCache.value.get(path) ?? [];
  }

  function findColumnForPath(fullPath: string): IColumn | undefined {
    const segments = fullPath.split(".");
    if (segments.length === 1) {
      return columns.value.find((c) => c.id === segments[0]);
    }

    let currentColumns: IColumn[] = columns.value;
    for (let depth = 0; depth < segments.length - 1; depth++) {
      const pathSoFar = segments.slice(0, depth + 1).join(".");
      const cached = refColumnsCache.value.get(pathSoFar);
      if (!cached) return undefined;
      currentColumns = cached;
    }

    return currentColumns.find((c) => c.id === segments[segments.length - 1]);
  }

  const resolvedFilters = computed(() => {
    const result: { fullPath: string; column: IColumn; label: string }[] = [];

    for (const filterId of visibleFilterIds.value) {
      const segments = filterId.split(".");

      if (segments.length === 1) {
        const column = columns.value.find((c) => c.id === filterId);
        if (column) {
          result.push({
            fullPath: filterId,
            column,
            label: column.label || column.id,
          });
        }
      } else {
        const column = findColumnForPath(filterId);
        if (!column) continue;

        const labels: string[] = [];
        let currentColumns: IColumn[] = columns.value;

        for (let depth = 0; depth < segments.length - 1; depth++) {
          const seg = segments[depth]!;
          const parentCol = currentColumns.find((c) => c.id === seg);
          if (!parentCol) break;
          labels.push(parentCol.label || parentCol.id);
          const pathSoFar = segments.slice(0, depth + 1).join(".");
          currentColumns = refColumnsCache.value.get(pathSoFar) || [];
        }

        result.push({
          fullPath: filterId,
          column,
          label: labels.join(".") + "." + (column.label || column.id),
        });
      }
    }

    return result;
  });

  // --- Ref pkey stripping ---

  async function extractRefPkey(
    column: IColumn,
    val: FilterValue
  ): Promise<FilterValue> {
    if (val === null || typeof val !== "object") return val;
    if (!column.refTableId) return val;
    const refSchemaId = column.refSchemaId || schemaId;
    try {
      if (Array.isArray(val)) {
        return (await Promise.all(
          val.map((item) =>
            typeof item === "object" && item !== null
              ? getPrimaryKey(item as IRow, column.refTableId!, refSchemaId)
              : item
          )
        )) as FilterValue;
      }
      return await getPrimaryKey(val as IRow, column.refTableId, refSchemaId);
    } catch {
      return val;
    }
  }

  async function setFilterValue(
    columnId: string,
    value: IFilterValue | null | undefined
  ) {
    if (value === null || value === undefined) {
      removeFilter(columnId);
    } else {
      const column = findColumnForPath(columnId);
      if (column && column.refTableId && value.value !== null) {
        const stripped = await extractRefPkey(column, value.value);
        setFilter(columnId, { ...value, value: stripped });
      } else {
        setFilter(columnId, value);
      }
    }
  }

  // --- Cross-filter and count fetching ---

  const crossFilterMap = computed(() => {
    const map = new Map<string, ReturnType<typeof buildGraphQLFilter>>();
    for (const filterId of visibleFilterIds.value) {
      const crossFilterStates = new Map<string, IFilterValue>();
      actualFilterStates.value.forEach((value, key) => {
        if (key !== filterId) {
          crossFilterStates.set(key, value);
        }
      });
      map.set(
        filterId,
        buildGraphQLFilter(
          crossFilterStates,
          columns.value,
          actualSearchValue.value
        )
      );
    }
    return map;
  });

  const countFetcherCache = new Map<string, ICountFetcher>();

  function getCountFetcher(columnPath: string): ICountFetcher {
    let fetcher = countFetcherCache.get(columnPath);
    if (!fetcher) {
      fetcher = createCountFetcher({
        schemaId,
        tableId,
        columnPath,
        getCrossFilter: () => crossFilterMap.value.get(columnPath),
      });
      countFetcherCache.set(columnPath, fetcher);
    }
    return fetcher;
  }

  const result: UseFilters = {
    filterStates: actualFilterStates,
    searchValue: actualSearchValue,
    gqlFilter,
    activeFilters,
    setFilter,
    setSearch,
    clearFilters,
    removeFilter,
    columns,
    visibleFilterIds,
    defaultFilterIds,
    toggleFilter,
    resetFilters,
    loadRefColumns,
    getRefColumns,
    resolvedFilters,
    findColumnForPath,
    setFilterValue,
    crossFilterMap,

    getCountFetcher,
  };

  return result;
}

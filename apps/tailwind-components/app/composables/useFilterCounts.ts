import { shallowRef, computed, watch, type Ref, type ComputedRef } from "vue";
import { useDebounceFn } from "@vueuse/core";
import type { IColumn } from "../../../metadata-utils/src/types";
import type {
  IFilterValue,
  IGraphQLFilter,
  NestedColumnMeta,
} from "../../types/filters";
import { buildGraphQLFilter } from "../utils/buildGqlFilter";
import {
  fetchCounts,
  type CountedOption,
  type FetchCountsResult,
} from "../utils/fetchCounts";
import { isCountableType } from "../utils/filterTypes";
import fetchGraphql from "./fetchGraphql";

export interface UseFilterCountsDeps {
  schemaId: string;
  tableId: string;
  debounceMs: number;
  columns: ComputedRef<IColumn[]>;
  visibleFilterIds: Ref<string[]>;
  filterStates: ComputedRef<Map<string, IFilterValue>>;
  searchValue: ComputedRef<string>;
  nestedColumnMeta: Ref<Map<string, NestedColumnMeta>>;
  resolveColumn: (columnId: string) => IColumn | null;
  columnTypeMap: ComputedRef<Map<string, string>>;
}

export interface UseFilterCountsResult {
  baseCounts: Ref<Map<string, CountedOption[]>>;
  fetchColumnCounts: (columnId: string, useBase?: boolean) => Promise<void>;
  getCountedOptions: (columnId: string) => ComputedRef<CountedOption[]>;
  isCountLoading: (columnId: string) => ComputedRef<boolean>;
  isSaturated: (columnId: string) => ComputedRef<boolean>;
  debouncedRefetchCounts: () => void;
}

export function useFilterCounts(
  deps: UseFilterCountsDeps
): UseFilterCountsResult {
  const {
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
  } = deps;

  const countsMap = shallowRef<Map<string, CountedOption[]>>(new Map());
  const loadingSet = shallowRef<Set<string>>(new Set());
  const baseCounts = shallowRef<Map<string, CountedOption[]>>(new Map());
  const saturatedMap = shallowRef<Map<string, boolean>>(new Map());
  const abortControllers = new Map<string, AbortController>();

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
        overlap: match?.overlap ?? 0,
        ...(children !== undefined ? { children } : {}),
      };
    });
  }

  function buildCrossFilter(excludeColumnId: string): IGraphQLFilter {
    const crossStates = new Map(filterStates.value);
    crossStates.delete(excludeColumnId);
    return buildGraphQLFilter(crossStates, columns.value, searchValue.value);
  }

  async function fetchColumnCounts(columnId: string, useBase = false) {
    const col = resolveColumn(columnId);
    const columnType = col?.columnType ?? null;
    if (!columnType || !isCountableType(columnType)) return;

    const prior = abortControllers.get(columnId);
    if (prior) prior.abort();
    const controller = new AbortController();
    abortControllers.set(columnId, controller);

    const newLoading = new Set(loadingSet.value);
    newLoading.add(columnId);
    loadingSet.value = newLoading;

    try {
      const crossFilter = useBase ? {} : buildCrossFilter(columnId);
      const refTableId = col?.refTableId ?? null;
      const refSchemaId = col?.refSchemaId ?? null;
      const refLabel = col?.refLabel ?? col?.refLabelDefault ?? null;
      const signalledFetcher = (
        sId: string,
        query: string,
        variables: any
      ): Promise<any> =>
        fetchGraphql(sId, query, variables, { signal: controller.signal });

      const facetHasSelection = !useBase && filterStates.value.has(columnId);
      const crossFilterIncludeAll = facetHasSelection
        ? buildGraphQLFilter(
            filterStates.value,
            columns.value,
            searchValue.value,
            columnTypeMap.value
          )
        : undefined;

      const result: FetchCountsResult = await fetchCounts(
        schemaId,
        tableId,
        columnId,
        columnType,
        crossFilter,
        signalledFetcher,
        refTableId,
        refSchemaId,
        refLabel,
        crossFilterIncludeAll
      );

      const { options: results, saturated } = result;

      const newSaturated = new Map(saturatedMap.value);
      newSaturated.set(columnId, saturated);
      saturatedMap.value = newSaturated;

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
    } catch (err: any) {
      if (err?.name !== "AbortError") {
        console.error(`fetchColumnCounts failed for ${columnId}:`, err);
      }
    } finally {
      const newLoading = new Set(loadingSet.value);
      newLoading.delete(columnId);
      loadingSet.value = newLoading;
      if (abortControllers.get(columnId) === controller) {
        abortControllers.delete(columnId);
      }
    }
  }

  const debouncedRefetchCounts = useDebounceFn(async () => {
    const countableIds = visibleFilterIds.value.filter((id) => {
      const colType = resolveColumn(id)?.columnType ?? null;
      return colType && isCountableType(colType);
    });
    await Promise.all(countableIds.map((id) => fetchColumnCounts(id, false)));
  }, debounceMs);

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

  function isSaturated(columnId: string): ComputedRef<boolean> {
    return computed(() => saturatedMap.value.get(columnId) === true);
  }

  return {
    baseCounts,
    fetchColumnCounts,
    getCountedOptions,
    isCountLoading,
    isSaturated,
    debouncedRefetchCounts,
  };
}

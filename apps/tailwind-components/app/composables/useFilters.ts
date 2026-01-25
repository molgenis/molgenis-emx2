import { ref, computed, type Ref } from "vue";
import { useDebounceFn } from "@vueuse/core";
import type { IColumn } from "../../../metadata-utils/src/types";
import type { IFilterValue, IGraphQLFilter } from "../../types/filters";
import { buildGraphQLFilter } from "../utils/buildFilter";

export interface UseFiltersOptions {
  debounceMs?: number;
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

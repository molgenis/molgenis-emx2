import { defineStore } from "pinia";
import { computed, ref, watch } from "vue";
import { createFilters } from "../filter-config/facetConfigurator";
import { applyFiltersToQuery } from "../functions/applyFiltersToQuery";
import { useBiobanksStore } from "./biobanksStore";
import { useSettingsStore } from "./settingsStore";

export const useFiltersStore = defineStore("filtersStore", () => {
  const biobankStore = useBiobanksStore();
  const { baseQuery, updateBiobankCards } = biobankStore;

  const settingsStore = useSettingsStore();

  let filters = ref({});
  let filterType = ref({});
  let filterTypeUpdatedFromFilter = ref("");
  let filterOptionsCache = ref({});
  let filterFacets = createFilters(filters);
  const facetDetails = {};

  /** extract the components types so we can use that in adding the correct query parts */
  filterFacets.forEach((filterFacet) => {
    facetDetails[filterFacet.facetIdentifier] = { ...filterFacet };
  });

  function resetFilters() {
    this.baseQuery.resetFilters();
  }

  const hasActiveFilters = computed(() => {
    return Object.keys(filters.value).length > 0;
  });

  let queryDelay = undefined;

  watch(
    filters,
    (filters) => {
      if (queryDelay) {
        clearTimeout(queryDelay);
      }
      /** reset pagination */
      settingsStore.currentPage = 1;

      queryDelay = setTimeout(async () => {
        clearTimeout(queryDelay);

        applyFiltersToQuery(baseQuery, filters, facetDetails, filterType);
        await updateBiobankCards();
      }, 750);
    },
    { deep: true }
  );

  watch(
    filterType,
    (filterType) => {
      if (queryDelay) {
        clearTimeout(queryDelay);
      }
      /** reset pagination */
      settingsStore.currentPage = 1;
      queryDelay = setTimeout(async () => {
        clearTimeout(queryDelay);
        applyFiltersToQuery(baseQuery, filters.value, facetDetails, filterType);

        /** only update if we have something to update, switching the radiobutton itself should not trigger a refresh */
        if (
          hasActiveFilters.value &&
          filters.value[filterTypeUpdatedFromFilter].length
        ) {
          await updateBiobankCards();
        }
      }, 750);
    },
    { deep: true, immediate: true }
  );

  function updateFilter(filterName, value) {
    /** filter reset, so delete */
    if (value === "" || value === undefined || value.length === 0) {
      delete filters.value[filterName];
    } else {
      filters.value[filterName] = value;
    }
  }

  function getFilterValue(filterName) {
    return filters.value[filterName];
  }

  function updateFilterType(filterName, value) {
    /** filter reset, so delete */
    if (value === "" || value === undefined || value.length === 0) {
      delete filterType.value[filterName];
    } else {
      filterType.value[filterName] = value;
    }
    filterTypeUpdatedFromFilter = filterName;
  }

  function getFilterType(filterName) {
    return filterType.value[filterName] || "any";
  }

  return {
    resetFilters,
    updateFilter,
    getFilterValue,
    updateFilterType,
    getFilterType,
    filterOptionsCache,
    hasActiveFilters,
    filters,
    filterFacets,
  };
});

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

  function resetFilters () {
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


  function flattenOntologyBranch (branch, flattendBranches) {
    if (!branch.children || !branch.children.length) {
      if (!flattendBranches) {
        return [branch]
      }
      else {
        flattendBranches.push(branch)
      }
      return flattendBranches
    }
    else {
      for (const child of branch.children) {
        flattendBranches = flattenOntologyBranch(child, flattendBranches)
      }
    }
    return flattendBranches
  }


  /**
   * 
   * @param {string} filterName the name of the ontology filter
   * @param {string | Array<string>} value array with identifiers or a string with an identifier
   * @param {boolean} add
   */
  function updateOntologyFilter (filterName, value, add) {
    /** value can be a child (single value), or a parent with its children > make it into an array of values */
    let processedValue = value

    if (value.children && value.children.length) {
      const copyBranch = JSON.parse(JSON.stringify(value))
      let allChildrenValues = flattenOntologyBranch(copyBranch)
      delete copyBranch.children
      allChildrenValues.push(copyBranch)
      processedValue = allChildrenValues
    }

    const multipleOptions = Array.isArray(processedValue)

    if (add) {
      multipleOptions ? addOntologyOptions(filterName, processedValue) : addOntologyOption(filterName, processedValue)
    }
    else {
      multipleOptions ? removeOntologyOptions(filterName, processedValue) : removeOntologyOption(filterName, processedValue)
    }
  }

  /**
   * @param {string} filterName name of ontology filter
   * @param {string} value the identifier 'value' of the filter option
   */
  function addOntologyOption (filterName, value) {
    if (filters.value[filterName]) {
      /** sanity check, if it is there already then the job is done */
      if (filters.value[filterName].some(option => option.name === value.name)) return

      filters.value[filterName].push(value)
    }
    else {
      filters.value[filterName] = [value]
    }
  }

  /**
   * @param {string} filterName name of ontology filter
   * @param {Array<string>} value array with identifier 'value' of the filter option
   */
  function addOntologyOptions (filterName, value) {

    if (filters.value[filterName]) {

      const existingValues = filters.value[filterName].map(option => option.name)
      const filterOptionsToAdd = value.filter(newValue => !existingValues.includes(newValue.name))
      filters.value[filterName] = filters.value[filterName].concat(filterOptionsToAdd)
    }
    else {
      filters.value[filterName] = value
    }
  }

  /**
   * @param {string} filterName name of ontology filter
   * @param {string} value the identifier 'value' of the filter option
   */
  function removeOntologyOption (filterName, value) {
    /** can't remove an option which is not present. Jobs done. */
    if (!filters.value[filterName]) return

    filters.value[filterName] = filters.value[filterName].filter(option => option.name !== value.name)

    /** everything is deselected, remove the filter entirely */
    if (filters.value[filterName].length === 0) delete filters.value[filterName];
  }

  /**
   * @param {string} filterName name of ontology filter
   * @param {Array<string>} value array with identifier 'value' of the filter option
   */
  function removeOntologyOptions (filterName, value) {
    /** can't remove an option which is not present. Jobs done. */
    if (!filters.value[filterName]) return

    const valuesToRemove = value.map(value => value.name)

    filters.value[filterName] = filters.value[filterName].filter(option => !valuesToRemove.includes(option.name))

    /** everything is deselected, remove the filter entirely */
    if (filters.value[filterName].length === 0) delete filters.value[filterName];
  }

  function updateFilter (filterName, value) {
    /** filter reset, so delete */
    if (value === "" || value === undefined || value.length === 0) {
      delete filters.value[filterName];
    } else {
      filters.value[filterName] = value;
    }
  }

  function getFilterValue (filterName) {
    return filters.value[filterName];
  }

  function updateFilterType (filterName, value) {
    /** filter reset, so delete */
    if (value === "" || value === undefined || value.length === 0) {
      delete filterType.value[filterName];
    } else {
      filterType.value[filterName] = value;
    }
    filterTypeUpdatedFromFilter = filterName;
  }

  function getFilterType (filterName) {
    return filterType.value[filterName] || "any";
  }

  return {
    resetFilters,
    updateFilter,
    updateOntologyFilter,
    getFilterValue,
    updateFilterType,
    getFilterType,
    filterOptionsCache,
    hasActiveFilters,
    filters,
    filterFacets,
  };
});

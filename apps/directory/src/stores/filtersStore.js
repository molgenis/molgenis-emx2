import { defineStore } from "pinia";
import { computed, ref, watch } from "vue";
import { createFilters } from "../filter-config/facetConfigurator";
import { applyFiltersToQuery } from "../functions/applyFiltersToQuery";
import { useBiobanksStore } from "./biobanksStore";
import { useSettingsStore } from "./settingsStore";
import { useCheckoutStore } from "./checkoutStore";
import { applyBookmark, createBookmark } from "../functions/bookmarkMapper";
import { QueryEMX2 } from "molgenis-components";
import { convertArrayToChunks } from "../functions/arrayUtilities";

export const useFiltersStore = defineStore("filtersStore", () => {
  const biobankStore = useBiobanksStore();
  const checkoutStore = useCheckoutStore();

  const { baseQuery, updateBiobankCards } = biobankStore;

  const settingsStore = useSettingsStore();

  const graphqlEndpoint = settingsStore.config.graphqlEndpoint;

  let bookmarkWaitingForApplication = ref(false);

  /** check for url manipulations */
  let bookmarkTriggeredFilter = ref(false);

  /** check for filter manipulations */
  let filterTriggeredBookmark = ref(false);

  let filters = ref({});
  let filterType = ref({});
  let filterOptionsCache = ref({});
  let filterFacets = ref({});
  const facetDetailsDictionary = ref({});

  let filtersReadyToRender = ref(false);

  watch(
    () => settingsStore.configurationFetched,
    (e) => {
      filterFacets.value = createFilters(settingsStore.config.filterFacets);
      filtersReadyToRender.value = true;
    }
  );

  const facetDetails = computed(() => {
    if (
      !Object.keys(facetDetailsDictionary.value).length &&
      settingsStore.configurationFetched
    )
      /** extract the components types so we can use that in adding the correct query parts */
      filterFacets.value.forEach((filterFacet) => {
        facetDetailsDictionary.value[filterFacet.facetIdentifier] = {
          ...filterFacet,
        };
      });

    return facetDetailsDictionary.value;
  });

  const filtersReady = computed(() => {
    return filterOptionsCache.value
      ? Object.keys(filterOptionsCache.value).length > 0
      : false;
  });

  function getValuePropertyForFacet(facetIdentifier) {
    return facetDetails[facetIdentifier].filterValueAttribute;
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

      /** when we reset the filters on bookmark update, we do not want to search, so hold your horses */

      queryDelay = setTimeout(async () => {
        applyFiltersToQuery(
          baseQuery,
          filters,
          facetDetails.value,
          filterType.value
        );

        if (!bookmarkTriggeredFilter.value) {
          createBookmark(filters, checkoutStore.selectedCollections);
        }
        bookmarkTriggeredFilter.value = false;

        await updateBiobankCards();
        clearTimeout(queryDelay);
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

      /** when we reset the filters on bookmark update, we do not want to search, so hold your horses */
      queryDelay = setTimeout(async () => {
        clearTimeout(queryDelay);
        applyFiltersToQuery(
          baseQuery,
          filters.value,
          facetDetails.value,
          filterType
        );

        if (!bookmarkTriggeredFilter.value) {
          createBookmark(filters.value, checkoutStore.selectedCollections);
        }
        bookmarkTriggeredFilter.value = false;

        if (hasActiveFilters.value) {
          await updateBiobankCards();
          clearTimeout(queryDelay);
        }
      }, 750);
    },
    { deep: true, immediate: true }
  );

  watch(filtersReady, (filtersReady) => {
    if (filtersReady) {
      const waitForStore = setTimeout(() => {
        applyBookmark();
        clearTimeout(waitForStore);
      }, 350);
    }
  });

  function checkOntologyDescendantsIfMatches(
    ontologyDescendants,
    ontologyQuery
  ) {
    let finalVerdict = false;

    for (const ontologyItem of ontologyDescendants) {
      if (finalVerdict) return true;

      if (this.ontologyItemMatchesQuery(ontologyItem, ontologyQuery)) {
        finalVerdict = true;
        break;
      } else if (ontologyItem.children) {
        finalVerdict = this.checkOntologyDescendantsIfMatches(
          ontologyItem.children,
          ontologyQuery
        );
      }
    }
    return finalVerdict;
  }

  function ontologyItemMatchesQuery(ontologyItem, ontologyQuery) {
    const findString = ontologyQuery.toLowerCase();
    const codeFound = ontologyItem.code.toLowerCase().includes(findString);
    const nameFound = ontologyItem.name.toLowerCase().includes(findString);
    const labelFound = ontologyItem.label.toLowerCase().includes(findString);

    return codeFound || nameFound || labelFound;
  }

  function flattenOntologyBranch(branch, flattendBranches) {
    if (!branch.children || !branch.children.length) {
      if (!flattendBranches) {
        return [branch];
      } else {
        flattendBranches.push(branch);
      }
      return flattendBranches;
    } else {
      for (const child of branch.children) {
        flattendBranches = flattenOntologyBranch(child, flattendBranches);
        delete child.children;
        flattendBranches.push(child);
      }
    }
    return flattendBranches;
  }

  /**
   *
   * @param {string} filterName the name of the ontology filter
   * @param {string | Array<string>} value array with identifiers or a string with an identifier
   * @param {boolean} add
   */
  function updateOntologyFilter(filterName, value, add, fromBookmark) {
    bookmarkTriggeredFilter.value = fromBookmark;

    /** value can be a child (single value), or a parent with its children > make it into an array of values */
    let processedValue = value;

    if (value.children && value.children.length) {
      const copyBranch = JSON.parse(JSON.stringify(value));
      let allChildrenValues = flattenOntologyBranch(copyBranch);
      delete copyBranch.children;
      allChildrenValues.push(copyBranch);

      const deduplicatedValues = [];
      const codesProcessed = [];

      for (const childValue of allChildrenValues) {
        if (!codesProcessed.includes(childValue.code)) {
          deduplicatedValues.push(childValue);
          codesProcessed.push(childValue.code);
        }
      }

      processedValue = deduplicatedValues;
    }

    const multipleOptions = Array.isArray(processedValue);

    if (add) {
      multipleOptions
        ? addOntologyOptions(filterName, processedValue)
        : addOntologyOption(filterName, processedValue);
    } else {
      multipleOptions
        ? removeOntologyOptions(filterName, processedValue)
        : removeOntologyOption(filterName, processedValue);
    }
  }

  /**
   * @param {string} filterName name of ontology filter
   * @param {string} value the identifier 'value' of the filter option
   */
  function addOntologyOption(filterName, value) {
    if (filters.value[filterName]) {
      /** sanity check, if it is there already then the job is done */
      if (
        filters.value[filterName].some((option) => option.name === value.name)
      )
        return;

      filters.value[filterName].push(value);
    } else {
      filters.value[filterName] = [value];
    }
  }

  /**
   * @param {string} filterName name of ontology filter
   * @param {Array<string>} value array with identifier 'value' of the filter option
   */
  function addOntologyOptions(filterName, value) {
    if (filters.value[filterName]) {
      const existingValues = filters.value[filterName].map(
        (option) => option.name
      );
      const filterOptionsToAdd = value.filter(
        (newValue) => !existingValues.includes(newValue.name)
      );
      filters.value[filterName] = filters.value[filterName].concat(
        filterOptionsToAdd
      );
    } else {
      filters.value[filterName] = value;
    }
  }
  /** did not move this to be used in filteroptions because the store is async. */
  function getOntologyAttributes(filterFacet) {
    const { filterLabelAttribute, filterValueAttribute } = filterFacet;
    return [
      filterLabelAttribute,
      filterValueAttribute,
      "code",
      `parent.${filterValueAttribute}`,
      `children.${filterValueAttribute}`,
      `children.children.${filterValueAttribute}`,
      `children.children.children.${filterValueAttribute}`,
      `children.children.children.children.${filterValueAttribute}`,
      `children.children.children.children.children.${filterValueAttribute}`,
      "children.code",
      "children.children.code",
      "children.children.children.code",
      "children.children.children.children.code",
      "children.children.children.children.children.code",
      `children.${filterLabelAttribute}`,
      `children.children.${filterLabelAttribute}`,
      `children.children.children.${filterLabelAttribute}`,
      `children.children.children.children.${filterLabelAttribute}`,
      `children.children.children.children.children.${filterLabelAttribute}`,
    ];
  }

  async function getOntologyOptionsForCodes(filterFacet, codes) {
    const { sourceTable, sortColumn, sortDirection } = filterFacet;
    const attributes = getOntologyAttributes(filterFacet);

    let codesToQuery = convertArrayToChunks(codes, 600);
    const ontologyResults = [];

    for (const codeBlock of codesToQuery) {
      const ontologyResult = await new QueryEMX2(graphqlEndpoint)
        .table(sourceTable)
        .select(attributes)
        .where("code")
        .in(codeBlock)
        .orderBy(sourceTable, sortColumn, sortDirection)
        .execute();

      if (ontologyResult && ontologyResult[sourceTable]) {
        ontologyResults.push(...ontologyResult[sourceTable]);
      }
    }

    return ontologyResults;
  }

  /**
   * @param {string} filterName name of ontology filter
   * @param {string} value the identifier 'value' of the filter option
   */
  function removeOntologyOption(filterName, value) {
    /** can't remove an option which is not present. Jobs done. */
    if (!filters.value[filterName]) return;

    filters.value[filterName] = filters.value[filterName].filter(
      (option) => option.name !== value.name
    );

    /** everything is deselected, remove the filter entirely */
    if (filters.value[filterName].length === 0)
      delete filters.value[filterName];
  }

  /**
   * @param {string} filterName name of ontology filter
   * @param {Array<string>} value array with identifier 'value' of the filter option
   */
  function removeOntologyOptions(filterName, value) {
    /** can't remove an option which is not present. Jobs done. */
    if (!filters.value[filterName]) return;

    const valuesToRemove = value.map((value) => value.name);

    filters.value[filterName] = filters.value[filterName].filter(
      (option) => !valuesToRemove.includes(option.name)
    );

    /** everything is deselected, remove the filter entirely */
    if (filters.value[filterName].length === 0)
      delete filters.value[filterName];
  }

  function clearAllFilters() {
    filters.value = {};
    createBookmark(filters.value, checkoutStore.selectedCollections);
  }

  function updateFilter(filterName, value, fromBookmark) {
    bookmarkTriggeredFilter.value = fromBookmark;

    /** filter reset, so delete */
    if (
      value === null ||
      value === "" ||
      value === undefined ||
      value.length === 0
    ) {
      delete filters.value[filterName];
      checkoutStore.setSearchHistory(`Filter ${filterName} removed`);
    } else {
      filters.value[filterName] = value;

      checkoutStore.setSearchHistory(
        `${filterName} filtered on ${value.map((v) => v.text).join(", ")}`
      );
    }
  }

  function getFilterValue(filterName) {
    return filters.value[filterName];
  }

  function updateFilterType(filterName, value, fromBookmark) {
    bookmarkTriggeredFilter.value = fromBookmark;

    /** filter reset, so delete */
    if (value === "" || value === undefined || value.length === 0) {
      delete filterType.value[filterName];
    } else {
      filterType.value[filterName] = value;
    }
  }

  function getFilterType(filterName) {
    return filterType.value[filterName] || "any";
  }

  return {
    facetDetails,
    updateFilter,
    clearAllFilters,
    updateOntologyFilter,
    getFilterValue,
    updateFilterType,
    getFilterType,
    getValuePropertyForFacet,
    checkOntologyDescendantsIfMatches,
    ontologyItemMatchesQuery,
    getOntologyOptionsForCodes,
    filterOptionsCache,
    hasActiveFilters,
    filters,
    filterFacets,
    filtersReady,
    bookmarkWaitingForApplication,
    filterTriggeredBookmark,
    filtersReadyToRender,
  };
});

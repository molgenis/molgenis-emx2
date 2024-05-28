import { defineStore } from "pinia";
import { computed, ref, watch } from "vue";
import { createFilters } from "../filter-config/facetConfigurator";
import { applyFiltersToQuery } from "../functions/applyFiltersToQuery";
import { useBiobanksStore } from "./biobanksStore";
import { useSettingsStore } from "./settingsStore";
import { useCheckoutStore } from "./checkoutStore";
import { applyBookmark, createBookmark } from "../functions/bookmarkMapper";
//@ts-ignore
import { QueryEMX2 } from "molgenis-components";
import { convertArrayToChunks } from "../functions/arrayUtilities";
import { IOntologyItem } from "../interfaces/interfaces";

export const useFiltersStore = defineStore("filtersStore", () => {
  const biobankStore = useBiobanksStore();
  const checkoutStore = useCheckoutStore();

  const { baseQuery, getBiobankCards } = biobankStore;

  const settingsStore = useSettingsStore();

  const graphqlEndpoint = settingsStore.config.graphqlEndpoint;
  const graphqlEndpointOntologyFilter = "/DirectoryOntologies/graphql";

  let bookmarkWaitingForApplication = ref(false);

  /** check for url manipulations */
  let bookmarkTriggeredFilter = ref(false);

  /** check for filter manipulations */
  let filterTriggeredBookmark = ref(false);

  let filters = ref<Record<string, any>>({});
  let filterType = ref<Record<string, any>>({});
  let filterOptionsCache = ref<Record<string, any>>({});
  let filterFacets = ref<any[]>([]);
  const facetDetailsDictionary = ref<Record<string, any>>({});

  let filtersReadyToRender = ref(false);

  watch(
    () => settingsStore.configurationFetched,
    () => {
      filterFacets.value = createFilters(settingsStore.config.filterFacets);
      filtersReadyToRender.value = true;
    }
  );

  const facetDetails = computed<Record<string, any>>(() => {
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

  function getValuePropertyForFacet(facetIdentifier: string) {
    return facetDetails.value[facetIdentifier].filterValueAttribute;
  }

  const hasActiveFilters = computed(() => {
    return Object.keys(filters.value).length > 0;
  });

  const hasActiveBiobankOnlyFilters = computed(() => {
    return !!(getFilterValue("Biobankservices") || getFilterValue("Countries"));
  });

  let queryDelay: any;
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

        await getBiobankCards();
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
          hasActiveBiobankOnlyFilters.value;
          await getBiobankCards();
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
    ontologyDescendants: IOntologyItem[],
    ontologyQuery: any
  ): boolean {
    let finalVerdict = false;

    for (const ontologyItem of ontologyDescendants) {
      if (finalVerdict) return true;

      if (ontologyItemMatchesQuery(ontologyItem, ontologyQuery)) {
        finalVerdict = true;
        break;
      } else if (ontologyItem.children) {
        finalVerdict = checkOntologyDescendantsIfMatches(
          ontologyItem.children,
          ontologyQuery
        );
      }
    }
    return finalVerdict;
  }

  function ontologyItemMatchesQuery(
    ontologyItem: IOntologyItem,
    ontologyQuery: any
  ) {
    const findString = ontologyQuery.toLowerCase();
    const codeFound = ontologyItem.code.toLowerCase().includes(findString);
    const nameFound = ontologyItem.name.toLowerCase().includes(findString);
    const labelFound = ontologyItem.label.toLowerCase().includes(findString);

    return codeFound || nameFound || labelFound;
  }

  function flattenOntologyBranch(
    branch: IOntologyItem,
    flattenedBranches: IOntologyItem[] = []
  ) {
    if (!branch.children?.length) {
      return [...flattenedBranches, branch];
    } else {
      for (const child of branch.children) {
        flattenedBranches = flattenOntologyBranch(child, flattenedBranches);
        delete child.children;
        flattenedBranches?.push(child);
      }
    }
    return flattenedBranches;
  }

  function updateOntologyFilter(
    filterName: string,
    value: any,
    add: boolean,
    fromBookmark: any
  ) {
    bookmarkTriggeredFilter.value = fromBookmark;

    /** value can be a child (single value), or a parent with its children > make it into an array of values */
    let processedValue = value;

    if (value.children?.length) {
      let copyBranch = JSON.parse(JSON.stringify(value));
      let allChildrenValues = flattenOntologyBranch(copyBranch);
      delete copyBranch.children;
      allChildrenValues.push(copyBranch);

      let deduplicatedValues: IOntologyItem[] = [];
      let codesProcessed: string[] = [];

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

  function addOntologyOption(filterName: string, value: IOntologyItem) {
    if (filters.value[filterName]) {
      /** sanity check, if it is there already then the job is done */
      if (
        filters.value[filterName].some(
          (option: IOntologyItem) => option.name === value.name
        )
      )
        return;

      filters.value[filterName].push(value);
    } else {
      filters.value[filterName] = [value];
    }
  }

  function addOntologyOptions(filterName: string, value: IOntologyItem[]) {
    if (filters.value[filterName]) {
      const existingValues = filters.value[filterName].map(
        (option: IOntologyItem) => option.name
      );
      const filterOptionsToAdd = value.filter(
        (newValue: IOntologyItem) => !existingValues.includes(newValue.name)
      );
      filters.value[filterName] =
        filters.value[filterName].concat(filterOptionsToAdd);
    } else {
      filters.value[filterName] = value;
    }
  }

  /** did not move this to be used in filteroptions because the store is async. */
  function getOntologyAttributes(filterFacet: Record<string, any>) {
    const { filterLabelAttribute, filterValueAttribute } = filterFacet;
    return [
      filterLabelAttribute,
      filterValueAttribute,
      "code",
      `parent.${filterValueAttribute}`,
    ];
  }

  async function getOntologyOptionsForCodes(
    filterFacet: Record<string, any>,
    codes: any[][]
  ) {
    const { sourceTable, sortColumn, sortDirection } = filterFacet;
    const attributes = getOntologyAttributes(filterFacet);

    let codesToQuery = convertArrayToChunks(codes, 600);
    const ontologyResults = [];

    for (const codeBlock of codesToQuery) {
      const ontologyResult = await new QueryEMX2(graphqlEndpointOntologyFilter)
        .table(sourceTable)
        .select(attributes)
        .orWhere("code")
        .in(codeBlock)
        .orderBy(sourceTable, sortColumn, sortDirection)
        .execute();

      if (ontologyResult && ontologyResult[sourceTable]) {
        ontologyResults.push(...ontologyResult[sourceTable]);
      }
    }

    return ontologyResults;
  }

  function removeOntologyOption(filterName: string, value: IOntologyItem) {
    /** can't remove an option which is not present. Jobs done. */
    if (!filters.value[filterName]) return;

    filters.value[filterName] = filters.value[filterName].filter(
      (option: IOntologyItem) => option.name !== value.name
    );

    /** everything is deselected, remove the filter entirely */
    if (filters.value[filterName].length === 0)
      delete filters.value[filterName];
  }

  function removeOntologyOptions(filterName: string, value: IOntologyItem[]) {
    /** can't remove an option which is not present. Jobs done. */
    if (!filters.value[filterName]) return;

    const valuesToRemove = value.map((value) => value.name);

    filters.value[filterName] = filters.value[filterName].filter(
      (option: IOntologyItem) => !valuesToRemove.includes(option.name)
    );

    /** everything is deselected, remove the filter entirely */
    if (filters.value[filterName].length === 0)
      delete filters.value[filterName];
  }

  function clearAllFilters() {
    filters.value = {};
    createBookmark(filters.value, checkoutStore.selectedCollections);
  }

  function updateFilter(filterName: string, value: any, fromBookmark: any) {
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
      if (typeof value === "string" || typeof value === "boolean") {
        checkoutStore.setSearchHistory(`${filterName} filtered on ${value}`);
      } else {
        checkoutStore.setSearchHistory(
          `${filterName} filtered on ${value
            .map((v: Record<string, any>) => v.text)
            .join(", ")}`
        );
      }
    }
  }

  function getFilterValue(filterName: string) {
    return filters.value[filterName];
  }

  function updateFilterType(filterName: string, value: any, fromBookmark: any) {
    bookmarkTriggeredFilter.value = fromBookmark;

    if (value === "" || value === undefined || value.length === 0) {
      /** filter reset, so delete */
      delete filterType.value[filterName];
    } else {
      filterType.value[filterName] = value;
    }
  }

  function getFilterType(filterName: string) {
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
    hasActiveBiobankOnlyFilters,
    filters,
    filterFacets,
    filtersReady,
    bookmarkWaitingForApplication,
    filterTriggeredBookmark,
    filtersReadyToRender,
  };
});

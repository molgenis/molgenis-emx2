import * as _ from "lodash";
import { defineStore } from "pinia";
import { computed, ref, watch } from "vue";
import { createFilters } from "../filter-config/facetConfigurator";
import { applyFiltersToQuery } from "../functions/applyFiltersToQuery";
import { createBookmark } from "../functions/bookmarkMapper";
import { useBiobanksStore } from "./biobanksStore";
import { useCheckoutStore } from "./checkoutStore";
import { useSettingsStore } from "./settingsStore";
//@ts-ignore
import { QueryEMX2 } from "molgenis-components";
import useErrorHandler from "../composables/errorHandler";
import flattenOntologyBranch from "../functions/flattenOntologyBranch";
import { IFilterOption, IOntologyItem } from "../interfaces/interfaces";

const { setError, clearError } = useErrorHandler();
const DIAGNOSIS_AVAILABLE = "Diagnosisavailable";

export const useFiltersStore = defineStore("filtersStore", () => {
  const biobankStore = useBiobanksStore();
  const checkoutStore = useCheckoutStore();
  const settingsStore = useSettingsStore();

  const graphqlEndpointOntologyFilter = "/DirectoryOntologies/graphql";

  const filters = ref<Record<string, any>>({});
  const filterType = ref<Record<string, any>>({});

  const filterFacets = ref<Record<string, any>[]>([]); //Facet = settings of the filters
  const filterFacetsById = ref<Record<string, any>>({});
  const filterOptions = ref<Record<string, IFilterOption[]>>({});

  const filtersReadyToRender = ref(false);
  const filterPromise = ref<Promise<any> | null>(null);

  const indeterminateDiseases = ref<Record<string, boolean>>({});
  const selectedDiseases = ref<Record<string, boolean>>({});
  const diseases = ref<Record<string, IOntologyItem>>({});

  filterPromise.value = settingsStore.configurationPromise.then(async () => {
    filterFacets.value = createFilters(settingsStore.config.filterFacets);
    filterFacetsById.value = _.keyBy(filterFacets.value, "facetIdentifier");
    filtersReadyToRender.value = true;
  });

  const hasActiveFilters = computed(() => {
    return Object.keys(filters.value).length > 0;
  });

  const hasActiveBiobankOnlyFilters = computed(() => {
    return !!getFilterValue("Biobankservices") || !!getFilterValue("Countries");
  });

  watch(selectedDiseases, setIndeterminateDiseases, {
    deep: true,
  });

  function getValuePropertyForFacet(facetIdentifier: string) {
    return filterFacetsById.value[facetIdentifier].filterValueAttribute;
  }

  function checkOntologyDescendantsIfMatches(
    ontologyDescendants: IOntologyItem[],
    ontologyQuery: string
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
    ontologyQuery: string
  ) {
    const findString = ontologyQuery.toLowerCase();
    const codeFound = ontologyItem.code.toLowerCase().includes(findString);
    const nameFound = ontologyItem.name.toLowerCase().includes(findString);
    const labelFound = ontologyItem.label.toLowerCase().includes(findString);

    return codeFound || nameFound || labelFound;
  }

  function updateOntologyFilter(
    filterName: string,
    value: IOntologyItem,
    add: boolean,
    fromBookmark: any = false
  ) {
    const childValues = flattenOntologyBranch(value);
    const processedValues = _.uniqBy(childValues, "code");

    if (add) {
      addOntologyOptions(filterName, processedValues);
    } else {
      removeOntologyOptions(filterName, processedValues);
    }

    updateQuery();

    if (!fromBookmark) {
      createBookmark(
        filters.value,
        checkoutStore.selectedCollections,
        checkoutStore.selectedServices,
        filterType.value
      );
    }
  }

  function addOntologyOptions(filterName: string, options: IOntologyItem[]) {
    let ontologySet = options;

    if (getFilterType(DIAGNOSIS_AVAILABLE) === "all") {
      const diagnosisAvailableCount =
        filters.value.Diagnosisavailable?.length || 0;
      const limit = 50;
      const slotsRemaining = limit - diagnosisAvailableCount;
      ontologySet = ontologySet.slice(0, slotsRemaining);
    }

    const newSelectedDiseases = ontologySet.reduce(
      (accum: Record<string, boolean>, option: IOntologyItem) => {
        accum[option.name] = true;
        return accum;
      },
      {}
    );

    selectedDiseases.value = {
      ...selectedDiseases.value,
      ...newSelectedDiseases,
    };

    if (filters.value[filterName]) {
      filters.value[filterName] = _.uniqBy(
        filters.value[filterName].concat(ontologySet),
        "name"
      );
    } else {
      filters.value[filterName] = ontologySet;
    }
  }

  /** did not move this to be used in filterOptions because the store is async. */
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
    codes: any[]
  ) {
    const { sourceTable, sortColumn, sortDirection } = filterFacet;
    const attributes = getOntologyAttributes(filterFacet);

    const codesToQuery = _.chunk(codes, 600);
    const ontologyResults = [];

    for (const codeBlock of codesToQuery) {
      let ontologyResult;
      try {
        clearError();
        ontologyResult = await new QueryEMX2(graphqlEndpointOntologyFilter)
          .table(sourceTable)
          .select(attributes)
          .orWhere("code")
          .in(codeBlock)
          .orderBy(sourceTable, sortColumn, sortDirection)
          .execute();
      } catch (error) {
        setError(error);
      }

      if (ontologyResult && ontologyResult[sourceTable]) {
        ontologyResults.push(...ontologyResult[sourceTable]);
      }
    }

    return ontologyResults;
  }

  function removeOntologyOptions(filterName: string, values: IOntologyItem[]) {
    if (!filters.value[filterName]) {
      selectedDiseases.value = {};
      indeterminateDiseases.value = {};
      return;
    }

    const valuesToRemove = getValuesToRemove(values);

    valuesToRemove.forEach((name: string) => {
      delete selectedDiseases.value[name];
    });

    filters.value[filterName] = filters.value[filterName].filter(
      (option: IOntologyItem) => !valuesToRemove.includes(option.name)
    );

    if (filters.value[filterName].length === 0) {
      delete filters.value[filterName];
    }
  }

  function getValuesToRemove(values: IOntologyItem[]) {
    return values.reduce((accum: string[], value) => {
      accum.push(value.name);
      if (value.parent) {
        value.parent.forEach((parent) => {
          accum.push(parent.name);
        });
      }
      return accum;
    }, []);
  }

  function clearAllFilters() {
    filters.value = {};
    selectedDiseases.value = {};
    indeterminateDiseases.value = {};
    updateQuery();

    createBookmark(
      filters.value,
      checkoutStore.selectedCollections,
      checkoutStore.selectedServices,
      filterType.value
    );
  }

  function updateFilter(
    filterName: string,
    value?: IFilterOption[] | string | boolean,
    fromBookmark?: boolean
  ) {
    if (typeof value === "string" || typeof value === "boolean") {
      if (value === "") {
        delete filters.value[filterName];
        checkoutStore.setSearchHistory(`Filter ${filterName} removed`);
      } else {
        filters.value[filterName] = value;
        checkoutStore.setSearchHistory(`${filterName} filtered on ${value}`);
      }
    } else if (Array.isArray(value) && value.length) {
      filters.value[filterName] = value;

      checkoutStore.setSearchHistory(
        `${filterName} filtered on ${value
          .map((v: Record<string, any>) => v.text)
          .join(", ")}`
      );
    } else {
      delete filters.value[filterName];
      checkoutStore.setSearchHistory(`Filter ${filterName} removed`);
    }

    updateQuery();

    if (!fromBookmark) {
      createBookmark(
        filters.value,
        checkoutStore.selectedCollections,
        checkoutStore.selectedServices,
        filterType.value
      );
    }
  }

  function getFilterValue(filterName: string) {
    return filters.value[filterName];
  }

  function updateFilterType(filterName: string, value: any) {
    if (
      filterName === DIAGNOSIS_AVAILABLE &&
      (filterType.value[filterName] === "any" ||
        filterType.value[filterName] === undefined) &&
      filters.value[filterName]?.length > 50
    ) {
      filters.value[filterName] = filters.value[filterName].slice(0, 50);
    }

    if (value === "" || value === undefined || value.length === 0) {
      delete filterType.value[filterName];
    } else {
      filterType.value[filterName] = value;
    }
  }

  function getFilterType(filterName: string) {
    return filterType.value[filterName] || "any";
  }

  function updateQuery() {
    applyFiltersToQuery(
      biobankStore.getBaseQuery(),
      filters.value,
      filterFacetsById.value,
      filterType.value
    );
  }

  function setDiseases(newDiseases: IOntologyItem[]): void {
    const allDiseases = newDiseases.flatMap((diseaseRoot) => {
      return flattenOntologyBranch(diseaseRoot);
    });
    diseases.value = _.keyBy(allDiseases, "name");
    setIndeterminateDiseases();
  }

  function setIndeterminateDiseases() {
    indeterminateDiseases.value = {};
    const stack = Object.keys(selectedDiseases.value);
    while (stack.length) {
      const key: string = stack.pop()!;
      const node = diseases.value[key];
      node?.parent?.forEach((parent: Record<string, any>) => {
        indeterminateDiseases.value[parent.name] = true;
        stack.push(parent.name);
      });
    }
  }

  function isIndeterminate(diseaseName: string): boolean {
    return indeterminateDiseases.value[diseaseName];
  }

  function deselectDiseaseLeavingChildren(diseaseName: string): void {
    if (selectedDiseases.value[diseaseName]) {
      delete selectedDiseases.value[diseaseName];
    }
  }

  return {
    checkOntologyDescendantsIfMatches,
    clearAllFilters,
    deselectDiseaseLeavingChildren,
    getFilterType,
    getFilterValue,
    getOntologyOptionsForCodes,
    getValuePropertyForFacet,
    isIndeterminate,
    ontologyItemMatchesQuery,
    setDiseases,
    updateFilter,
    updateFilterType,
    updateOntologyFilter,
    facetDetails: filterFacetsById,
    filterFacets,
    filtersReadyToRender,
    filterPromise,
    filters,
    filterOptions,
    filterType,
    hasActiveFilters,
    hasActiveBiobankOnlyFilters,
    selectedDiseases,
  };
});

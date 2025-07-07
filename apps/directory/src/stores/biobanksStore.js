import QueryEMX2 from "../../../molgenis-components/src/queryEmx2/queryEmx2.ts";
import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { extractValue } from "../functions/extractValue";
import { getPropertyByPath } from "../functions/getPropertyByPath";
import { useCollectionStore } from "./collectionStore";
import { useFiltersStore } from "./filtersStore";
import { useSettingsStore } from "./settingsStore";
import useErrorHandler from "../composables/errorHandler";

export const useBiobanksStore = defineStore("biobanksStore", () => {
  const settingsStore = useSettingsStore();
  const collectionStore = useCollectionStore();
  const filtersStore = useFiltersStore();
  const { setError, clearError } = useErrorHandler();

  const biobankReportColumns = settingsStore.config.biobankReportColumns;
  const biobankColumns = settingsStore.config.biobankColumns;
  const graphqlEndpoint = settingsStore.config.graphqlEndpoint;
  const biobankCardGraphql = biobankReportColumns.flatMap(
    (biobankColumn) => biobankColumn.column
  );

  let facetBiobankColumnDetails = ref([]);
  let biobankCards = ref([]);
  let waitingForResponse = ref(true);

  let lastRequestTime = 0;

  const collectionColumns = collectionStore.getCollectionColumns();
  const biobankProperties = biobankColumns
    .flatMap((biobankColumn) => biobankColumn.column)
    .concat({ collections: collectionColumns });

  const baseQuery = new QueryEMX2(graphqlEndpoint)
    .table("Biobanks")
    .select([...biobankCardGraphql, ...getFacetColumnDetails()])
    .orderBy("Biobanks", "name", "asc")
    .orderBy("collections", "id", "asc");

  const biobankCardsHaveResults = computed(
    () => !waitingForResponse.value && biobankCards.value?.length
  );

  const biobankCardsBiobankCount = computed(() => {
    return (
      biobankCards.value?.filter((biobankCard) => !biobankCard.withdrawn)
        .length || 0
    );
  });

  const biobankCardsSubcollectionCount = computed(() => {
    if (!biobankCards.value.length) {
      return 0;
    } else {
      return biobankCards.value
        .filter((bc) => bc.collections)
        .flatMap((biobank) =>
          biobank.collections.filter(
            (collection) =>
              !collection.withdrawn && collection.parent_collection
          )
        ).length;
    }
  });

  const biobankCardsCollectionCount = computed(() => {
    const totalCount = biobankCards.value
      .filter((bc) => bc.collections)
      .flatMap((biobank) =>
        biobank.collections.filter((collection) => !collection.withdrawn)
      ).length;
    return totalCount - biobankCardsSubcollectionCount.value;
  });

  const biobankCardsServicesCount = computed(
    () =>
      biobankCards.value
        .filter((bc) => bc.services)
        .flatMap((biobank) => biobank.services).length
  );

  function getFacetColumnDetails() {
    if (!facetBiobankColumnDetails.value.length) {
      const filterFacetProperties = [];

      const filterFacets = settingsStore.config.filterFacets.filter(
        (facet) =>
          !facet.builtIn &&
          facet.filterValueAttribute &&
          facet.filterLabelAttribute
      );

      for (const facet of filterFacets) {
        const columns = Array.isArray(facet.applyToColumn)
          ? facet.applyToColumn
          : [facet.applyToColumn];

        for (const column of columns) {
          let nestedpath = column.split(".");
          /** pop off the trail because that is not what the user always needs */
          nestedpath.pop();

          const nestedPathBase = nestedpath.join(".");

          filterFacetProperties.push(
            `${nestedPathBase}.${facet.filterLabelAttribute}`,
            `${nestedPathBase}.${facet.filterValueAttribute}`
          );
        }
        facetBiobankColumnDetails = filterFacetProperties;
      }
    }

    return facetBiobankColumnDetails;
  }

  /** This method is called upon page load and when a filter is applied. */
  /** Therefore it can be executed multiple times in parallel. */
  async function getBiobankCards() {
    if (!filtersStore.bookmarkWaitingForApplication) {
      waitingForResponse.value = true;

      const requestTime = Date.now();
      lastRequestTime = requestTime;

      let biobankResult = [];
      try {
        clearError();
        biobankResult = await baseQuery.execute();
      } catch (error) {
        setError(error);
      }

      /* Update biobankCards only if the result is the most recent one*/
      if (requestTime === lastRequestTime) {
        let foundBiobanks = biobankResult.Biobanks;
        if (
          filtersStore.hasActiveFilters &&
          !filtersStore.hasActiveBiobankOnlyFilters
        ) {
          foundBiobanks = foundBiobanks?.filter(
            (biobank) => biobank.collections
          );
        }
        biobankCards.value = filterWithdrawn(foundBiobanks);
        waitingForResponse.value = false;
        filtersStore.bookmarkWaitingForApplication = false;
      }
    }
  }

  async function getBiobank(id) {
    const biobankReportQuery = new QueryEMX2(graphqlEndpoint)
      .table("Biobanks")
      .select(biobankProperties)
      .orderBy("Biobanks", "name", "asc")
      .orderBy("collections", "id", "asc")
      .where("id")
      .equals(id);
    try {
      clearError();
      return await biobankReportQuery.execute();
    } catch (error) {
      setError(error);
      return null;
    }
  }

  function getPresentFilterOptions(facetIdentifier) {
    const { applyToColumn, adaptive } =
      filtersStore.facetDetails[facetIdentifier];
    if (
      biobankCards.value === undefined ||
      !biobankCards.value.length ||
      !adaptive
    ) {
      return [];
    }

    let columnPath = applyToColumn;
    if (!Array.isArray(applyToColumn)) {
      columnPath = [applyToColumn];
    }

    let valuesKnown;

    for (const path of columnPath) {
      const pathParts = path.split(".");

      if (pathParts[0] === "collections") {
        pathParts.shift(); /** removing 'collections' */
        const finalProperty = pathParts.pop();

        const collectionsFromBiobanks = biobankCards.value.flatMap(
          (biobank) => biobank.collections
        );

        if (collectionsFromBiobanks.length === 0) break;

        let valuesPresent = collectionsFromBiobanks.flatMap((collection) => {
          return extractValue(
            getPropertyByPath(collection, pathParts),
            finalProperty
          );
        });

        if (!valuesKnown) {
          valuesKnown = valuesPresent.filter((uv) => uv);
        } else {
          valuesKnown = valuesPresent.concat(valuesKnown).filter((uv) => uv);
        }
      } else {
        const finalProperty = pathParts.pop();
        const valuesPresent = biobankCards.value.map((biobank) => {
          return extractValue(
            getPropertyByPath(biobank, pathParts),
            finalProperty
          );
        });

        if (!valuesKnown) {
          valuesKnown = valuesPresent.filter((uv) => uv);
        } else {
          valuesKnown = valuesPresent.concat(valuesKnown).filter((uv) => uv);
        }
      }
    }
    return [...new Set(valuesKnown)];
  }

  return {
    getBiobankCards,
    getBiobank,
    getPresentFilterOptions,
    waiting: waitingForResponse,
    biobankCardsHaveResults,
    biobankCardsBiobankCount,
    biobankCardsCollectionCount,
    biobankCardsServicesCount,
    biobankCardsSubcollectionCount,
    biobankCards,
    baseQuery,
  };
});

function filterWithdrawn(biobanks) {
  let filteredBanks = biobanks?.filter((biobank) => !biobank.withdrawn) || [];
  filteredBanks.forEach((biobank) => {
    biobank.collections = biobank.collections?.filter(
      (collection) => !collection.withdrawn
    );
  });
  return filteredBanks;
}

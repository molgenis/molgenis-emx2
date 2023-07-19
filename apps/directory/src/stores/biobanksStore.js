import { defineStore } from "pinia";
import { computed, ref } from "vue";
import QueryEMX2 from "../functions/queryEMX2";
import { useSettingsStore } from "./settingsStore";
import { useCollectionStore } from "./collectionStore";
import { useFiltersStore } from "./filtersStore";

export const useBiobanksStore = defineStore("biobanksStore", () => {
  const settingsStore = useSettingsStore();
  const collectionStore = useCollectionStore();
  const filtersStore = useFiltersStore();

  const biobankReportColumns = settingsStore.config.biobankReportColumns;
  const biobankColumns = settingsStore.config.biobankColumns;
  const graphqlEndpoint = settingsStore.config.graphqlEndpoint;
  const biobankCardGraphql = biobankReportColumns.flatMap(
    (biobankColumn) => biobankColumn.column
  );

  let facetBiobankColumnDetails = ref([]);

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

  const collectionColumns = collectionStore.getCollectionColumns();

  const biobankProperties = biobankColumns
    .flatMap((biobankColumn) => biobankColumn.column)
    .concat({ collections: collectionColumns });

  let biobankCards = ref([]);
  let waitingForResponse = ref(false);

  const baseQuery = new QueryEMX2(graphqlEndpoint)
    .table("Biobanks")
    .select([...biobankCardGraphql, ...getFacetColumnDetails()])
    .orderBy("Biobanks", "name", "asc")
    .orderBy("collections", "id", "asc");

  /** GraphQL query to get all the data necessary for the home screen 'aka biobank card view */
  async function getBiobankCards() {
    if (!filtersStore.bookmarkWaitingForApplication) {
      waitingForResponse.value = true;
      if (biobankCards.value.length === 0) {
        const biobankResult = await baseQuery.execute();
        biobankCards.value = biobankResult.Biobanks;
      }
      waitingForResponse.value = false;
    }
    return biobankCards.value;
  }

  async function getBiobank(id) {
    const biobankReportQuery = new QueryEMX2(graphqlEndpoint)
      .table("Biobanks")
      .select(biobankProperties)
      .orderBy("Biobanks", "name", "asc")
      .orderBy("collections", "id", "asc")
      .where("id")
      .like(id);

    return await biobankReportQuery.execute();
  }

  async function updateBiobankCards() {
    if (!waitingForResponse.value) {
      waitingForResponse.value = true;
      biobankCards.value = [];
      const biobankResult = await baseQuery.execute();

      /** only show biobanks that have collections */
      const foundBiobanks = biobankResult.Biobanks
        ? biobankResult.Biobanks.filter((biobank) => biobank.collections)
        : [];
      biobankCards.value = foundBiobanks;
      waitingForResponse.value = false;

      filtersStore.bookmarkWaitingForApplication = false;
    }
  }

  const biobankCardsHaveResults = computed(() => {
    return !waitingForResponse.value && biobankCards.value.length > 0;
  });

  const biobankCardsBiobankCount = computed(() => {
    return biobankCards.value.length;
  });

  const biobankCardsCollectionCount = computed(() => {
    return biobankCards.value
      .filter((bc) => bc.collections)
      .flatMap((biobank) => biobank.collections).length;
  });

  const biobankCardsSubcollectionCount = computed(() => {
    if (!biobankCards.value.length) return 0;
    const collections = biobankCards.value
      .filter((bc) => bc.collections)
      .flatMap((biobank) => biobank.collections);
    if (!collections.length) return 0;
    return collections
      .filter((c) => c.sub_collections)
      .flatMap((collection) => collection.sub_collections).length;
  });

  return {
    updateBiobankCards,
    getBiobankCards,
    getBiobank,
    waiting: waitingForResponse,
    biobankCardsHaveResults,
    biobankCardsBiobankCount,
    biobankCardsCollectionCount,
    biobankCardsSubcollectionCount,
    biobankCards,
    baseQuery,
  };
});

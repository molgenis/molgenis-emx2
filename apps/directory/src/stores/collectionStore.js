import { defineStore } from "pinia";
import { QueryEMX2 } from "molgenis-components";
import { useSettingsStore } from "./settingsStore";
import { ref } from "vue";
import { useCheckoutStore } from "./checkoutStore";

export const useCollectionStore = defineStore("collectionStore", () => {
  const settingsStore = useSettingsStore();

  const collectionColumns = settingsStore.config.collectionColumns;
  const graphqlEndpoint = settingsStore.config.graphqlEndpoint;

  const commercialAvailableCollections = ref([]);

  function getCollectionColumns() {
    const properties = collectionColumns
      .filter((column) => column.column)
      .flatMap((collectionColumn) => collectionColumn.column);

    const rangeProperties = collectionColumns.filter(
      (column) => column.type === "range"
    );

    for (const property of rangeProperties) {
      properties.push(property.min, property.max, property.unit_column);
    }

    return properties;
  }

  /** when we hydrate a bookmark, we need some information for the cart */
  async function getMissingCollectionInformation(collectionIds) {
    const checkoutStore = useCheckoutStore();

    const collectionIdsToCheck = Array.isArray(collectionIds)
      ? collectionIds
      : [collectionIds];
    const biobanksInCart = Object.keys(checkoutStore.selectedCollections);

    const collectionIdsInCart = [];

    if (biobanksInCart.length) {
      for (const biobank of biobanksInCart) {
        collectionIdsInCart.push(
          ...checkoutStore.selectedCollections[biobank].map(
            (collection) => collection.value
          )
        );
      }
    }

    const idsMissing = collectionIdsToCheck.filter(
      (colId) => !collectionIdsInCart.includes(colId)
    );

    if (idsMissing.length) {
      const missingCollectionQuery = new QueryEMX2(graphqlEndpoint)
        .table("Collections")
        .select(["id", "name", "biobank.name"])
        .where("id")
        .orLike(idsMissing);
      const result = await missingCollectionQuery.execute();

      return result.Collections;
    } else {
      return {};
    }
  }

  async function getCommercialAvailableCollections() {
    if (!commercialAvailableCollections.value.length) {
      const commercialCollectionQuery = new QueryEMX2(graphqlEndpoint)
        .table("Collections")
        .select("id")
        .where("commercial_use")
        .equals(true);
      const commercialAvailableCollectionsResponse =
        await commercialCollectionQuery.execute();
      if (
        commercialAvailableCollectionsResponse.Collections &&
        commercialAvailableCollectionsResponse.Collections.length
      ) {
        commercialAvailableCollections.value =
          commercialAvailableCollectionsResponse.Collections.map(
            (collection) => collection.id
          );
      }
    }

    return commercialAvailableCollections.value;
  }

  async function getCollectionReport(id) {
    const collectionReportQuery = new QueryEMX2(graphqlEndpoint)
      .table("Collections")
      .select(getCollectionColumns())
      .orderBy("Collections", "id", "asc")
      .where("id")
      .like(id);

    const reportResults = await collectionReportQuery.execute();

    const factQuery = new QueryEMX2(graphqlEndpoint)
      .table("CollectionFacts")
      .select([
        "id",
        "number_of_samples",
        "number_of_donors",
        "sample_type.label",
        "sex.label",
        "age_range.label",
        "disease.label",
        "disease.name",
      ])
      .where("id")
      .like(id);

    const factResults = await factQuery.execute();
    reportResults.CollectionFacts = factResults.CollectionFacts;
    return reportResults;
  }

  return {
    getCollectionColumns,
    getMissingCollectionInformation,
    getCollectionReport,
    getCommercialAvailableCollections,
  };
});

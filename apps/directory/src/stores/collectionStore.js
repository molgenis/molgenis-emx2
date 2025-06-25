import { QueryEMX2 } from "molgenis-components";
import { defineStore } from "pinia";
import useErrorHandler from "../composables/errorHandler";
import { useCheckoutStore } from "./checkoutStore";
import { useSettingsStore } from "./settingsStore";

const { setError } = useErrorHandler();
export const useCollectionStore = defineStore("collectionStore", () => {
  const settingsStore = useSettingsStore();

  const collectionColumns = settingsStore.config.collectionColumns;
  const graphqlEndpoint = settingsStore.config.graphqlEndpoint;

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
        .select(["id", "name", "biobank.name", "also_known.url"])
        .where("id")
        .orLike(idsMissing);

      try {
        clearError();
        const result = await missingCollectionQuery.execute();
        return result.Collections;
      } catch (error) {
        setError(error);
        return {};
      }
    } else {
      return {};
    }
  }

  async function getCollectionReport(id) {
    const collectionReportQuery = new QueryEMX2(graphqlEndpoint)
      .table("Collections")
      .select(getCollectionColumns())
      .orderBy("Collections", "id", "asc")
      .where("id")
      .equals(id);

    let reportResults = [];
    try {
      clearError();
      reportResults = await collectionReportQuery.execute();
    } catch (error) {
      setError(error);
    }

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
      .where("collection.id")
      .like(id);

    let factResults = [];
    try {
      clearError();
      factResults = await factQuery.execute();
    } catch (error) {
      setError(error);
    }

    reportResults.CollectionFacts = factResults.CollectionFacts;
    return reportResults;
  }

  return {
    getCollectionColumns,
    getMissingCollectionInformation,
    getCollectionReport,
  };
});

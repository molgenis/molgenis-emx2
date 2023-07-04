import { defineStore } from "pinia";
import QueryEMX2 from "../functions/queryEMX2";
import { useSettingsStore } from "./settingsStore";
import { ref } from "vue";

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

    /** add defaults */
    properties.push("id", "name");

    return properties;
  }

  async function getCommercialAvailableCollections() {
    if (!commercialAvailableCollections.value.length) {
      const commercialCollectionQuery = new QueryEMX2(graphqlEndpoint)
        .table("Collections")
        .select("id")
        .where("commercial_use")
        .equals(true);
      const commercialAvailableCollectionsResponse = await commercialCollectionQuery.execute();
      if (
        commercialAvailableCollectionsResponse.Collections &&
        commercialAvailableCollectionsResponse.Collections.length
      ) {
        commercialAvailableCollections.value = commercialAvailableCollectionsResponse.Collections.map(
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
    return await collectionReportQuery.execute();
  }

  return {
    getCollectionReport,
    getCollectionColumns,
    getCommercialAvailableCollections,
  };
});

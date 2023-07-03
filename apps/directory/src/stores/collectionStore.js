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
      .map((collectionColumn) => collectionColumn.column);

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
        .where("collaboration_commercial")
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
      .select([
        "name",
        "size",
        "description",
        "biobank.id",
        "biobank.name",
        "biobank.withdrawn",
        "biobank.url",
        "biobank.juridical_person",
        "biobank.contact.first_name",
        "biobank.contact.last_name",
        "biobank.contact.email",
        "biobank.contact.role",
        "contact.first_name",
        "contact.last_name",
        "contact.email",
        "contact.role",
        "contact.country.label",
        "head.first_name",
        "head.last_name",
        "head.role",
        "country.label",
        "network.name",
        "network.id",
        "url",
        "withdrawn",
        "parent_collection.id",
        "parent_collection.name",
        ...getCollectionColumns(),
      ])
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

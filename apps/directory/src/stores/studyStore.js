import { defineStore } from "pinia";
import { QueryEMX2 } from "molgenis-components";
import { useSettingsStore } from "./settingsStore";

export const useStudyStore = defineStore("studyStore", () => {
  const settingsStore = useSettingsStore();

  const studyColumns = settingsStore.config.studyColumns;
  const graphqlEndpoint = settingsStore.config.graphqlEndpoint;

  function getStudyColumns() {
    const properties = studyColumns
      .filter((column) => column.column)
      .flatMap((studyColumn) => studyColumn.column);

    const rangeProperties = studyColumns.filter(
      (column) => column.type === "range"
    );

    for (const property of rangeProperties) {
      properties.push(property.min, property.max, property.unit_column);
    }

    return properties;
  }


  async function getStudyReport(id) {
    const studyReportQuery = new QueryEMX2(graphqlEndpoint)
      .table("Studies")
      .select(getStudyColumns())
      .orderBy("Studies", "id", "asc")
      .where("id")
      .like(id);
    const reportResults = await studyReportQuery.execute();

    return reportResults;
  }

  return {
    getStudyColumns,
    getStudyReport,
  };
});

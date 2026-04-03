import { defineStore } from "pinia";
//@ts-ignore
import { QueryEMX2 } from "molgenis-components";
import useErrorHandler from "../composables/errorHandler";
import { useSettingsStore } from "./settingsStore";

const { setError, clearError } = useErrorHandler();

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

  async function getStudyReport(id: string) {
    clearError();
    const studyReportQuery = new QueryEMX2(graphqlEndpoint)
      .table("Studies")
      .select(getStudyColumns())
      .orderBy("Studies", "id", "asc")
      .where("id")
      .equals(id);

    try {
      return await studyReportQuery.execute();
    } catch (error) {
      setError(error);
    }
  }

  return {
    getStudyReport,
  };
});

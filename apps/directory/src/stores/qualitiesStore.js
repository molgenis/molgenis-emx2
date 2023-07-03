import { defineStore } from "pinia";
import QueryEMX2 from "../functions/queryEMX2";
import { useSettingsStore } from "./settingsStore";
import { ref } from "vue";

export const useQualitiesStore = defineStore("qualitiesStore", () => {
  const settingsStore = useSettingsStore();
  const graphqlEndpoint = settingsStore.config.graphqlEndpoint;

  const qualityStandardsDictionary = ref({});
  let waitingOnResults = ref(false)

  async function getQualityStandardInformation () {

    if (Object.keys(qualityStandardsDictionary.value).length === 0) {
      if (!waitingOnResults.value) {
        waitingOnResults.value = true

        let labQualitiesResult = await new QueryEMX2(graphqlEndpoint)
          .table("LaboratoryStandards")
          .select(["name", "label", "definition"])
          .orderBy("LaboratoryStandards", "name", "asc")
          .execute();

        let labQualities = labQualitiesResult.LaboratoryStandards;

        let operationQualitiesResult = await new QueryEMX2(graphqlEndpoint)
          .table("OperationalStandards")
          .select(["name", "label", "definition"])
          .orderBy("OperationalStandards", "name", "asc")
          .execute();

       let operationQualities = operationQualitiesResult.OperationalStandards;

        const allQualities = labQualities.concat(operationQualities);
        const qualityNameDictionary = {};

        for (const quality of allQualities) {
          if (!qualityNameDictionary[quality.name]) {
            qualityNameDictionary[quality.name] = {
              label: quality.label,
              definition: quality.definition,
            };
          }
        }
        this.qualityStandardsDictionary = qualityNameDictionary;
        waitingOnResults.value = false
      }
    }
  }

  return {
    getQualityStandardInformation,
    qualityStandardsDictionary,
  };
});

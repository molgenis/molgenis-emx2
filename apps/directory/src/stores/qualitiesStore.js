import { defineStore } from "pinia";
import { QueryEMX2 } from "molgenis-components";
import { useSettingsStore } from "./settingsStore";
import { ref } from "vue";

export const useQualitiesStore = defineStore("qualitiesStore", () => {
  const settingsStore = useSettingsStore();
  const graphqlEndpoint = settingsStore.config.graphqlEndpoint;

  const qualityStandardsDictionary = ref({});
  let waitingOnResults = ref(false);

  async function getQualityStandardInformation() {
    if (Object.keys(qualityStandardsDictionary.value).length === 0) {
      if (!waitingOnResults.value) {
        waitingOnResults.value = true;

        let qualityStandardsQueryResult = await new QueryEMX2(graphqlEndpoint)
          .table("QualityStandards")
          .select(["name", "label", "definition"])
          .execute();

        if (qualityStandardsQueryResult.QualityStandards) {
          for (const quality of qualityStandardsQueryResult.QualityStandards) {
            this.qualityStandardsDictionary[quality.name] = {
              label: quality.label,
              definition: quality.definition
            };
          }
        }
        waitingOnResults.value = false;
      }
    }
  }

  return {
    getQualityStandardInformation,
    qualityStandardsDictionary,
  };
});

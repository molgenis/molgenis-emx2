import { QueryEMX2 } from "molgenis-components";
import { defineStore } from "pinia";
import { ref } from "vue";
import useErrorHandler from "../composables/errorHandler";

const { setError, clearError } = useErrorHandler();

export const useQualitiesStore = defineStore("qualitiesStore", () => {
  const qualityStandardsDictionary = ref({});
  let waitingOnResults = ref(false);

  async function getQualityStandardInformation() {
    clearError();
    if (Object.keys(qualityStandardsDictionary.value).length === 0) {
      if (!waitingOnResults.value) {
        waitingOnResults.value = true;

        const endpoint = `${window.location.protocol}//${window.location.host}/DirectoryOntologies/graphql`;

        let qualityStandardsQueryResult;
        try {
          qualityStandardsQueryResult = await new QueryEMX2(endpoint)
            .table("QualityStandards")
            .select(["name", "label", "definition"])
            .execute();
        } catch (error) {
          setError(error);
          return;
        }

        if (qualityStandardsQueryResult.QualityStandards) {
          for (const quality of qualityStandardsQueryResult.QualityStandards) {
            this.qualityStandardsDictionary[quality.name] = {
              label: quality.label,
              definition: quality.definition,
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

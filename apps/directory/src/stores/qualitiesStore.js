import { defineStore } from "pinia";
import QueryEMX2 from "../functions/queryEMX2";
import { useSettingsStore } from "./settingsStore";
import { ref } from "vue";

export const useQualitiesStore = defineStore("qualitiesStore", () => {
    const settingsStore = useSettingsStore();
    const graphqlEndpoint = settingsStore.config.graphqlEndpoint;


    const qualityStandardsDictionary = ref({});

    async function getQualityStandardInformation () {
        if (Object.keys(qualityStandardsDictionary.value).length === 0) {
            let labQualities = await new QueryEMX2(graphqlEndpoint)
                .table("LaboratoryStandards")
                .select([
                    "name",
                    "label",
                    "definition"
                ])
                .orderBy("LaboratoryStandards", "name", "asc")
                .execute()

            labQualities = labQualities.LaboratoryStandards

            let operationQualities = await new QueryEMX2(graphqlEndpoint)
                .table("OperationalStandards")
                .select([
                    "name",
                    "label",
                    "definition"
                ])
                .orderBy("OperationalStandards", "name", "asc")
                .execute()

            operationQualities = operationQualities.OperationalStandards

            const allQualities = labQualities.concat(operationQualities);
            const qualityNameDictionary = {}


            for (const quality of allQualities) {
                if (!qualityNameDictionary[quality.name]) {
                    qualityNameDictionary[quality.name] = { label: quality.label, definition: quality.definition }
                }
            }
            this.qualityStandardsDictionary = qualityNameDictionary;
        }
    }

    return {
        getQualityStandardInformation,
        qualityStandardsDictionary
    };
});

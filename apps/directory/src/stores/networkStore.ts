import { defineStore } from "pinia";
import { ref } from "vue";
import QueryEMX2 from "../functions/queryEMX2";
import { useSettingsStore } from "./settingsStore";

export const useNetworkStore = defineStore("networkStore", () => {
  const settingsStore = useSettingsStore();
  const graphqlEndpoint = settingsStore.config.graphqlEndpoint;

  let networkReport = ref<any>({});

  async function getNetworkReport(netWorkId: string) {
    const reportQuery = new QueryEMX2(graphqlEndpoint).table(netWorkId);
    // TODO build query
    // networkReport.value = await reportQuery.execute();
    networkReport.value = {
      network: {
        name: "test",
        id: "networkId",
        description: "network description",
        common_network_elements: [
          { label: "label", description: "description" },
          { description: "description" },
        ],
      },
      biobanks: [
        {
          name: "biobank name",
          id: "biobankId",
          description: "biobank description",
        },
        {
          name: "biobank2 name",
          id: "biobank2Id",
          description: "biobank2 description",
        },
      ],
      collections: [
        { name: "collection name", id: "collectionID", viewmodel: {} },
        { name: "collection2 name", id: "collection2ID", viewmodel: {} },
      ],
    };
  }

  return {
    networkReport,

    getNetworkReport,
  };
});

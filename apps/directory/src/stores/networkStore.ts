import { defineStore } from "pinia";
import { ref } from "vue";
import QueryEMX2 from "../functions/queryEMX2";
import { useSettingsStore } from "./settingsStore";
import { useCollectionStore } from "./collectionStore";

export const useNetworkStore = defineStore("networkStore", () => {
  const settingsStore = useSettingsStore();
  const collectionsStore = useCollectionStore();
  const graphqlEndpoint = settingsStore.config.graphqlEndpoint;

  let networkReport = ref<any>({});

  async function loadNetworkReport(netWorkId: string) {
    const biobanksQuery = new QueryEMX2(graphqlEndpoint)
      .table("Biobanks")
      .select(["name", "id", "description"])
      .where("network.id")
      .like(netWorkId);
    const biobanksResult = await biobanksQuery.execute();

    const reportQuery = new QueryEMX2(graphqlEndpoint)
      .table("Networks")
      .select([
        "name",
        "id",
        "description",
        "common_network_elements.label",
        "common_network_elements.definition",
        "contact.first_name",
        "contact.last_name",
        "contact.email",
        "contact.role",
        "contact.country.label",
      ])
      .where("id")
      .like(netWorkId);
    const networkResult = await reportQuery.execute();

    const collectionsColumns = collectionsStore.getCollectionColumns() as any;
    const collectionsQuery = new QueryEMX2(graphqlEndpoint)
      .table("Collections")
      .select(collectionsColumns)
      .where("network.id")
      .like(netWorkId);
    const collectionsResult = await collectionsQuery.execute();

    networkReport.value = {
      network: networkResult.Networks[0],
      biobanks: biobanksResult.Biobanks,
      collections: collectionsResult.Collections,
    };
  }

  return {
    networkReport,
    loadNetworkReport,
  };
});

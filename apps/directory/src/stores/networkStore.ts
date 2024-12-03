import { defineStore } from "pinia";
import { ref } from "vue";
//@ts-ignore
import { QueryEMX2 } from "molgenis-components";
import { useSettingsStore } from "./settingsStore";
import { useCollectionStore } from "./collectionStore";
import { ContactInfoColumns } from "../property-config/contactInfoColumns";

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
      .equals(netWorkId);
    const biobanksResult = await biobanksQuery.execute();

    const reportQuery = new QueryEMX2(graphqlEndpoint)
      .table("Networks")
      .select([
        "name",
        "id",
        "description",
        "common_network_elements.label",
        "common_network_elements.definition",
        "also_known.url",
        "also_known.name_system",
        ...ContactInfoColumns,
      ])
      .where("id")
      .equals(netWorkId);
    const networkResult = await reportQuery.execute();

    const collectionsColumns = collectionsStore.getCollectionColumns() as any;
    const collectionsQuery = new QueryEMX2(graphqlEndpoint)
      .table("Collections")
      .select(collectionsColumns)
      .where("network.id")
      .equals(netWorkId);
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

import { defineStore } from "pinia";
import { ref } from "vue";
//@ts-ignore
import { QueryEMX2 } from "molgenis-components";
import useErrorHandler from "../composables/errorHandler";
import { ContactInfoColumns } from "../property-config/contactInfoColumns";
import { useCollectionStore } from "./collectionStore";
import { useSettingsStore } from "./settingsStore";

const { setError, clearError } = useErrorHandler();

export const useNetworkStore = defineStore("networkStore", () => {
  const settingsStore = useSettingsStore();
  const collectionsStore = useCollectionStore();
  const graphqlEndpoint = settingsStore.config.graphqlEndpoint;

  let networkReport = ref<any>({});

  async function loadNetworkReport(netWorkId: string) {
    clearError();
    const biobanksQuery = new QueryEMX2(graphqlEndpoint)
      .table("Biobanks")
      .select(["name", "id", "description", "withdrawn"])
      .where("network.id")
      .equals(netWorkId);

    let biobanksResult;
    try {
      biobanksResult = await biobanksQuery.execute();
    } catch (error) {
      setError(Error);
      return;
    }

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

    let networkResult;
    try {
      networkResult = await reportQuery.execute();
    } catch (error) {
      setError(error);
      return;
    }

    const collectionsColumns = collectionsStore.getCollectionColumns() as any;
    const collectionsQuery = new QueryEMX2(graphqlEndpoint)
      .table("Collections")
      .select(collectionsColumns)
      .where("network.id")
      .equals(netWorkId);

    let collectionsResult;
    try {
      collectionsResult = await collectionsQuery.execute();
    } catch (error) {
      setError(error);
      return;
    }

    if (networkResult?.Networks?.length) {
      networkReport.value = {
        network: networkResult?.Networks[0],
        biobanks: biobanksResult?.Biobanks,
        collections: collectionsResult?.Collections,
      };
    } else {
      setError("Network not found");
      networkReport.value = {
        network: undefined,
        biobanks: biobanksResult?.Biobanks,
        collections: collectionsResult?.Collections,
      };
    }
  }

  return {
    networkReport,
    loadNetworkReport,
  };
});

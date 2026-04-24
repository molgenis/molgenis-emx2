import { fetchSetting } from "#imports";
import { defineStore } from "pinia";
import { reactive, ref } from "vue";
import type { IResources } from "../../interfaces/catalogue";

export const useDatasetStore = defineStore("datasets", () => {
  const datasets = reactive<Record<string, IResources>>({});
  const isEnabled = ref<boolean>(false);
  const datasetStoreUrl = ref<string>("");
  const storeVersion = ref<string>("");

  const CATALOGUE_STORE_IS_ENABLED = "CATALOGUE_STORE_IS_ENABLED";
  const CATALOGUE_STORE_URL = "CATALOGUE_STORE_URL";
  const CATALOGUE_STORE_VERSION = "CATALOGUE_STORE_VERSION";

  isDatastoreEnabled();
  setDatasetStoreUrl();
  setNegotiatorVersion();

  async function isDatastoreEnabled() {
    const enabledSetting = await getSetting(CATALOGUE_STORE_IS_ENABLED);
    isEnabled.value = enabledSetting === "true";
  }

  async function setDatasetStoreUrl() {
    datasetStoreUrl.value = (await getSetting(CATALOGUE_STORE_URL)) || "";
  }

  async function setNegotiatorVersion() {
    storeVersion.value = (await getSetting(CATALOGUE_STORE_VERSION)) || "";
  }

  function addToCart(resource: IResources) {
    datasets[resource.id as keyof IResources] = resource;
  }

  function removeFromCart(resourceId: string) {
    delete datasets[resourceId as keyof IResources];
  }

  function resourceIsInCart(resourceId: string) {
    return Object.keys(datasets).includes(resourceId);
  }

  async function getSetting(settingConst: string) {
    const response = await fetchSetting(settingConst);
    const settingResponse = response.data._settings.find(
      (setting: { key: string; value: string }) => {
        return setting.key === settingConst;
      }
    );
    return settingResponse.value;
  }

  function clearCart() {
    for (const key in datasets) {
      delete datasets[key as keyof IResources];
    }
  }

  return {
    datasets,
    datasetStoreUrl,
    storeVersion,
    isEnabled,
    addToCart,
    clearCart,
    removeFromCart,
    resourceIsInCart,
  };
});

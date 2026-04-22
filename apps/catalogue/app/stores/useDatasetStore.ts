import { fetchSetting } from "#imports";
import { defineStore } from "pinia";
import { reactive, ref, type Ref } from "vue";
import type { IResources } from "../../interfaces/catalogue";
import type { IShoppingCart } from "../../interfaces/types";

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

  async function getDatasetStoreUrl() {
    if (!datasetStoreUrl.value) {
      await setDatasetStoreUrl();
    }
    return datasetStoreUrl.value;
  }

  async function setNegotiatorVersion() {
    getSetting(CATALOGUE_STORE_VERSION).then((version) => {
      storeVersion.value = version || "";
    });
  }

  async function getNegotiatorVersion() {
    if (!storeVersion.value) {
      await setNegotiatorVersion();
    }
    return storeVersion.value;
  }

  function addToCart(resource: IResources) {
    const newCartDataset: IShoppingCart = { [resource.id]: resource };
    datasets.value = Object.assign(newCartDataset, datasets.value);
  }

  function removeFromCart(resourceId: string) {
    if (datasets.value) {
      delete datasets.value[resourceId as keyof IResources];
    }
  }

  function resourceIsInCart(resourceId: string) {
    if (datasets.value) {
      return Object.keys(datasets.value).includes(resourceId);
    } else {
      return false;
    }
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

  return {
    datasets,
    isEnabled,
    addToCart,
    removeFromCart,
    resourceIsInCart,
    getDatasetStoreUrl,
    getNegotiatorVersion,
  };
});

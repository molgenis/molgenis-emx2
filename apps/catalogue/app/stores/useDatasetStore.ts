import { fetchSetting } from "#imports";
import { defineStore } from "pinia";
import { reactive, ref } from "vue";
import type { IResources } from "../../interfaces/catalogue";
import type { IShoppingCart } from "../../interfaces/types";

export const useDatasetStore = defineStore("datasets", () => {
  const datasets = reactive<Record<string, IResources>>({});
  const isEnabled = ref<boolean>(false);
  const datasetStoreUrl = ref<string>("");

  const CATALOGUE_STORE_IS_ENABLED: string = "CATALOGUE_STORE_IS_ENABLED";
  const CATALOGUE_STORE_URL: string = "CATALOGUE_STORE_URL";

  async function isDatastoreEnabled() {
    const enabledResponse = await fetchSetting(CATALOGUE_STORE_IS_ENABLED);
    const status = enabledResponse.data._settings.find(
      (setting: { key: string; value: string }) => {
        return setting.key === CATALOGUE_STORE_IS_ENABLED;
      }
    );
    if (status) {
      isEnabled.value = status.value === "true";
    }
  }

  async function getDatasetStoreUrl() {
    if (!datasetStoreUrl.value) {
      const urlResponse = await fetchSetting(CATALOGUE_STORE_URL);
      const url = urlResponse.data._settings.find(
        (setting: { key: string; value: string }) => {
          return setting.key === CATALOGUE_STORE_URL;
        }
      );

      datasetStoreUrl.value = url.value || "";
    }
    return datasetStoreUrl.value;
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

  return {
    datasets,
    isEnabled,
    addToCart,
    removeFromCart,
    resourceIsInCart,
    isDatastoreEnabled,
    getDatasetStoreUrl,
  };
});

import { fetchSetting } from "#imports";
import { defineStore } from "pinia";
import { reactive, ref } from "vue";
import type { IResources } from "~/interfaces/catalogue";
import type { IShoppingCart } from "~/interfaces/types";

export const useDatasetStore = defineStore("datasets", () => {
  const datasets = reactive<Record<string, IResources>>({});
  const isEnabled = ref<boolean>(false);
  const CATALOGUE_STORE_IS_ENABLED: string = "CATALOGUE_STORE_IS_ENABLED";

  async function isDatastoreEnabled() {
    const response = await fetchSetting(CATALOGUE_STORE_IS_ENABLED);
    const status = response.data._settings.find(
      (setting: { key: string; value: string }) => {
        return setting.key === CATALOGUE_STORE_IS_ENABLED;
      }
    );
    if (status) {
      isEnabled.value = status.value === "true";
    }
  }

  function addToCart(resource: IResources) {
    const newCartDataset: IShoppingCart = {};
    newCartDataset[resource.id] = resource;
    datasets.value = Object.assign(newCartDataset, datasets.value);
  }

  function removeFromCart(resourceId: string) {
    delete datasets.value[resourceId as keyof IResources];
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
  };
});

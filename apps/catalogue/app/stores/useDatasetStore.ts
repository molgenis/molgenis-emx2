import { fetchSettings } from "#imports";
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

  setStoreVariables();

  async function setStoreVariables() {
    const response = await fetchSettings([
      CATALOGUE_STORE_IS_ENABLED,
      CATALOGUE_STORE_URL,
      CATALOGUE_STORE_VERSION,
    ]);

    const settings = response.data._settings;

    const isEnabledSetting = settings.find(
      (setting: { key: string; value: string }) =>
        setting.key === CATALOGUE_STORE_IS_ENABLED
    );

    if (isEnabledSetting) {
      isEnabled.value = isEnabledSetting.value === "true";
      const urlSetting = settings.find(
        (setting: { key: string; value: string }) =>
          setting.key === CATALOGUE_STORE_URL
      );
      const versionSetting = settings.find(
        (setting: { key: string; value: string }) =>
          setting.key === CATALOGUE_STORE_VERSION
      );
      if (!urlSetting || !versionSetting) {
        throw new Error("Catalogue store URL or version setting not found");
      } else {
        datasetStoreUrl.value = urlSetting.value;
        storeVersion.value = versionSetting.value;
      }
    }
  }

  function addToCart(resource: IResources) {
    datasets[resource.id as keyof IResources] = resource;
  }

  function removeFromCart(resourceId: string) {
    delete datasets[resourceId as keyof IResources];
  }

  function resourceIsInCart(resourceId: string) {
    return !!datasets[resourceId as keyof IResources];
  }

  function clearCart() {
    for (const key in datasets) {
      delete datasets[key as keyof IResources];
    }
  }

  return {
    datasets,
    datasetStoreUrl,
    isEnabled,
    storeVersion,
    addToCart,
    clearCart,
    removeFromCart,
    resourceIsInCart,
  };
});

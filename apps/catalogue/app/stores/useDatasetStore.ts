import { fetchSettings } from "../composables/fetchSettings";
import { defineStore } from "pinia";
import { reactive, ref } from "vue";
import type { IResources } from "../../interfaces/catalogue";
import type { ISetting } from "../../../metadata-utils/src";
import {
  getHumanReadableString,
  handleV3Error,
  toNegotiatorFormat,
} from "./util/datasetStoreUtils";

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
    const isEnabledSetting = findSetting(CATALOGUE_STORE_IS_ENABLED, settings);

    if (isEnabledSetting) {
      isEnabled.value = isEnabledSetting.value === "true";
      const urlSetting = findSetting(CATALOGUE_STORE_URL, settings);
      const versionSetting = findSetting(CATALOGUE_STORE_VERSION, settings);

      if (!urlSetting || !versionSetting) {
        throw new Error("Catalogue store URL or version setting not found");
      } else {
        datasetStoreUrl.value = urlSetting.value;
        storeVersion.value = versionSetting.value;
      }
    }
  }

  function findSetting(setting: string, settings: ISetting[]) {
    return settings.find(
      (sett: { key: string; value: string }) => sett.key === setting
    );
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

  async function doStoreRequest() {
    if (!Object.keys(datasets).length) {
      return;
    }

    switch (storeVersion.value) {
      case "REMS":
        window.open(datasetStoreUrl.value, "_blank");
        break;
      case "negotiatorV3":
        return await doNegotiatorV3Request();
      default:
        return "Unknown data store version, cannot send to store.";
    }
  }

  async function doNegotiatorV3Request() {
    const url = window.location.origin;
    const humanReadable = getHumanReadableString(datasets);
    const resources = toNegotiatorFormat(datasets);
    const payload: Record<string, any> = { url, humanReadable, resources };

    const response = await fetch(datasetStoreUrl.value, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    if (response.ok) {
      clearCart();
      const body = await response.json();
      window.location.href = body.redirectUrl;
    } else {
      return handleV3Error(response);
    }
  }

  return {
    datasets,
    isEnabled,
    storeVersion,
    addToCart,
    clearCart,
    doStoreRequest,
    removeFromCart,
    resourceIsInCart,
  };
});

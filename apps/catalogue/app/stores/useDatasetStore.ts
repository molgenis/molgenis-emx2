import { defineStore } from "pinia";
import { reactive, ref } from "vue";
import type { IResources } from "../../interfaces/catalogue";
import {
  doNegotiatorV3Request,
  getStoreVariables,
} from "./util/datasetStoreClient";
import { handleV3Error } from "./util/datasetStoreUtils";

export const useDatasetStore = defineStore("datasets", () => {
  const datasets = reactive<Record<string, IResources>>({});
  const isEnabled = ref<boolean>(false);
  const datasetStoreUrl = ref<string>("");
  const storeVersion = ref<string>("");

  setStoreVariables();

  async function setStoreVariables() {
    const { enabled, url, version } = await getStoreVariables();
    isEnabled.value = enabled;
    datasetStoreUrl.value = url;
    storeVersion.value = version;
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
        return await doNegotiatorV3Request(
          datasets,
          datasetStoreUrl.value
        ).then(async (response: Response) => {
          if (response.ok) {
            clearCart();
            const body = await response.json();
            window.location.href = body.redirectUrl;
          } else {
            return handleV3Error(response);
          }
        });
      default:
        return "Unknown data store version, cannot send to store.";
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

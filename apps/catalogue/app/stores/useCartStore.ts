/// <reference types="vite/client" />
import { acceptHMRUpdate, defineStore } from "pinia";
import { reactive, ref } from "vue";
import type { IResources } from "../../interfaces/catalogue";
import {
  doNegotiatorV3Request,
  handleNegotiatorV3Error,
} from "./util/negotiatorClient";
import { getCatalogueStoreConfig } from "./util/catalogueStoreConfig";

export const useCartStore = defineStore("cart", () => {
  const datasets = reactive<Record<string, IResources>>({});
  const isEnabled = ref<boolean>(false);
  const catalogueStoreUrl = ref<string>("");
  const catalogueStoreVersion = ref<string>("");

  setCatalogueStoreVariables();

  async function setCatalogueStoreVariables() {
    const { enabled, url, version } = await getCatalogueStoreConfig();
    isEnabled.value = enabled;
    catalogueStoreUrl.value = url;
    catalogueStoreVersion.value = version;
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

  async function doCartRequest() {
    if (!Object.keys(datasets).length) {
      return;
    }

    switch (catalogueStoreVersion.value) {
      case "negotiatorV3":
        return await doNegotiatorV3Request(datasets, catalogueStoreUrl.value)
          .then(async (response: Response) => {
            if (response.ok) {
              clearCart();
              const body = await response.json();
              window.location.href = body.redirectUrl;
            } else {
              return handleNegotiatorV3Error(response);
            }
          })
          .catch((error) => {
            console.error("Error during Negotiator V3 request:", error);
          });
      default:
        return "Unknown data store version, cannot send to store.";
    }
  }

  function getVersionText() {
    switch (catalogueStoreVersion.value) {
      case "negotiatorV3":
        return "Negotiator";
      default:
        return "Unknown data store";
    }
  }

  return {
    datasets,
    isEnabled,
    catalogueStoreVersion,
    addToCart,
    clearCart,
    doCartRequest,
    getVersionText,
    removeFromCart,
    resourceIsInCart,
  };
});

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useCartStore, import.meta.hot));
}

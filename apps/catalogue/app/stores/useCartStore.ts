/// <reference types="vite/client" />
import { acceptHMRUpdate, defineStore } from "pinia";
import { computed, reactive, ref } from "vue";
import type { ICart, ICartItem } from "../../interfaces/types";
import {
  doNegotiatorV3Request,
  handleNegotiatorV3Error,
} from "./util/negotiatorClient";
import { getCatalogueStoreConfig } from "./util/catalogueStoreConfig";

export const useCartStore = defineStore("cart", () => {
  const cart = reactive<ICart>(new Map());
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

  const cartItems = computed(() => [...cart.values()]);

  const resourcesInCart = computed(() =>
    cartItems.value.filter(
      (item): item is Extract<ICartItem, { type: "resource" }> =>
        item.type === "resource"
    )
  );

  function addToCart(item: ICartItem) {
    cart.set(item.id, item);
  }

  function removeFromCart(itemId: string) {
    cart.delete(itemId);
  }

  function isInCart(itemId: string) {
    return cart.has(itemId);
  }

  function isEmpty() {
    return cart.size === 0;
  }

  function clearCart() {
    cart.clear();
  }

  async function doCartRequest() {
    if (!resourcesInCart.value.length) {
      return;
    }

    switch (catalogueStoreVersion.value) {
      case "negotiatorV3":
        return await doNegotiatorV3Request(cartItems.value, catalogueStoreUrl.value)
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
    cart,
    cartItems,
    resourcesInCart,
    isEnabled,
    addToCart,
    clearCart,
    doCartRequest,
    getVersionText,
    removeFromCart,
    isInCart,
    isEmpty
  };
});

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useCartStore, import.meta.hot));
}
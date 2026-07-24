/// <reference types="vite/client" />
import { onNuxtReady, useRuntimeConfig } from "#app";
import { acceptHMRUpdate, defineStore } from "pinia";
import { computed, reactive, ref, watch } from "vue";
import type { ICart, ICartItem } from "../../interfaces/types";
import {
  doNegotiatorV3Request,
  handleNegotiatorV3Error,
} from "./util/negotiatorClient";
import { getCatalogueStoreConfig } from "./util/catalogueStoreConfig";
import { cartItemsOfType } from "../utils/cartItem";

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
    cartItemsOfType(cartItems.value, "resource")
  );

  const variablesInCart = computed(() =>
    cartItemsOfType(cartItems.value, "variable")
  );

  const CART_STORAGE_KEY = `emx2-catalogue-cart:${
    useRuntimeConfig().public.schema
  }`;

  // load after hydration: reading localStorage during setup would make the
  // first client render differ from the server render
  if (import.meta.client) {
    onNuxtReady(() => {
      try {
        const saved = window.localStorage.getItem(CART_STORAGE_KEY);
        if (saved) {
          const items = (JSON.parse(saved) as ICartItem[]).filter(
            (item) =>
              item?.id &&
              item?.label &&
              (item.type === "variable" ||
                (item.type === "resource" && item.pid && item.name))
          );
          for (const item of items) {
            cart.set(item.id, item);
          }
        }
      } catch {
        window.localStorage.removeItem(CART_STORAGE_KEY);
      }

      watch(cartItems, (items) => {
        window.localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(items));
      });
    });
  }

  function addToCart(item: ICartItem) {
    cart.set(item.id, item);
  }

  function removeFromCart(itemId: string) {
    cart.delete(itemId);
  }

  function isInCart(itemId: string) {
    return cart.has(itemId);
  }

  const isEmpty = computed(() => cart.size === 0);

  function clearCart() {
    cart.clear();
  }

  async function doCartRequest() {
    if (!resourcesInCart.value.length) {
      return;
    }

    switch (catalogueStoreVersion.value) {
      case "negotiatorV3":
        return await doNegotiatorV3Request(
          cartItems.value,
          catalogueStoreUrl.value
        )
          .then(async (response: Response) => {
            if (!response.ok) {
              return handleNegotiatorV3Error(response);
            }
            const body = await response.json();
            if (!body.redirectUrl) {
              return "The store response did not contain a redirect URL.";
            }
            clearCart();
            window.location.href = body.redirectUrl;
          })
          .catch((error) => {
            console.error("Error during Negotiator V3 request:", error);
            return "Could not reach the store.";
          });
      default:
        return "Unknown data store version, cannot send to store.";
    }
  }

  const versionText = computed(() => {
    switch (catalogueStoreVersion.value) {
      case "negotiatorV3":
        return "Negotiator";
      default:
        return "Unknown data store";
    }
  });

  return {
    cart,
    cartItems,
    resourcesInCart,
    variablesInCart,
    isEnabled,
    addToCart,
    clearCart,
    doCartRequest,
    versionText,
    removeFromCart,
    isInCart,
    isEmpty,
  };
});

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useCartStore, import.meta.hot));
}

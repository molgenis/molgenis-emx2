import { defineStore } from "pinia";
import type { IResources } from "~/interfaces/catalogue";
import type { IShoppingCart } from "~/interfaces/types";

export const useDatasetStore = defineStore("datasets", () => {
  const datasets = reactive<Record<string, IResources>>({});

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

  return { datasets, addToCart, removeFromCart, resourceIsInCart };
});

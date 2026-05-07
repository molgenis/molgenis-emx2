<script setup lang="ts">
import { computed } from "vue";
import BaseIcon from "../../../../tailwind-components/app/components/BaseIcon.vue";
import type { IResources } from "../../../interfaces/catalogue";
import { useDatasetStore } from "../../stores/useDatasetStore";

const props = withDefaults(
  defineProps<{
    resource: IResources;
    compact?: boolean;
    invert?: boolean;
  }>(),
  {
    compact: false,
    invert: false,
  }
);

const isInShoppingCart = computed(() =>
  datasetStore.resourceIsInCart(props.resource.id)
);

const datasetStore = useDatasetStore();
function onInput() {
  if (isInShoppingCart.value) {
    datasetStore.removeFromCart(props.resource.id);
  } else {
    datasetStore.addToCart(props.resource);
  }
}
</script>
<template>
  <label
    :for="`${resource.id}-shopping-cart-input`"
    class="xl:flex xl:justify-end px-2 py-1 rounded-3px cursor-pointer text-link hover:text-blue-800 focus:text-blue-800"
    :class="{
      'items-baseline xl:items-center mt-0.5 xl:mt-0': !props.compact,
      'bg-blue-500 text-white hover:text-white':
        isInShoppingCart && !props.invert,
      'bg-white text-blue-500 hover:text-blue-500':
        isInShoppingCart && props.invert,
    }"
  >
    <BaseIcon name="shopping-cart-add" :width="21" />
    <span class="sr-only"></span>
    <input
      type="checkbox"
      :id="`${resource.id}-shopping-cart-input`"
      class="sr-only"
      :modelValue="isInShoppingCart"
      @input="onInput"
    />
  </label>
</template>

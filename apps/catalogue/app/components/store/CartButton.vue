<script setup lang="ts">
import { computed } from "vue";
import BaseIcon from "../../../../tailwind-components/app/components/BaseIcon.vue";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import IconButton from "../../../../tailwind-components/app/components/button/IconButton.vue";
import type { IResources } from "../../../interfaces/catalogue";
import { useDatasetStore } from "../../stores/useDatasetStore";

const props = withDefaults(
  defineProps<{
    resource: IResources;
    compact?: boolean;
    isButton?: boolean;
  }>(),
  {
    compact: false,
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
  <span v-if="isButton">
    <Button
      :type="isInShoppingCart ? 'secondary' : 'primary'"
      size="medium"
      :label="isInShoppingCart ? 'Remove' : 'Add'"
      @click="onInput"
      buttonAlignment="right"
      :icon="isInShoppingCart ? 'shopping-cart-remove' : 'shopping-cart-add'"
    />
  </span>
  <label
    v-else
    :for="`${resource.id}-shopping-cart-input`"
    class="xl:flex xl:justify-end px-2 py-1 rounded-3px cursor-pointer text-link hover:text-blue-800 focus:text-blue-800"
    :class="{
      'items-baseline xl:items-center mt-0.5 xl:mt-0': !props.compact,
      'bg-blue-500 text-white hover:text-white': isInShoppingCart,
    }"
  >
    <BaseIcon
      :name="isInShoppingCart ? 'shopping-cart-remove' : 'shopping-cart-add'"
      :width="21"
    />
    <span class="sr-only">Add to cart</span>
    <input
      type="checkbox"
      :id="`${resource.id}-shopping-cart-input`"
      class="sr-only"
      :modelValue="isInShoppingCart"
      @input="onInput"
    />
  </label>
</template>

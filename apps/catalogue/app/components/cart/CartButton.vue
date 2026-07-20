<script setup lang="ts">
import { computed } from "vue";
import BaseIcon from "../../../../tailwind-components/app/components/BaseIcon.vue";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import type { ICartItem } from "../../../interfaces/types";
import { useCartStore } from "../../stores/useCartStore";

const props = withDefaults(
  defineProps<{
    item: ICartItem;
    compact?: boolean;
    isButton?: boolean;
  }>(),
  {
    compact: false,
  }
);

const cartStore = useCartStore();

const isInCart = computed(() => {
  return cartStore.isInCart(props.item.id);
});

function onInput() {
  if (isInCart.value) {
    cartStore.removeFromCart(props.item.id);
  } else {
    cartStore.addToCart(props.item);
  }
}
</script>
<template>
  <span v-if="isButton">
    <Button
      :type="isInCart ? 'secondary' : 'primary'"
      :label="isInCart ? 'Remove from cart' : 'Add to cart'"
      :icon="isInCart ? 'shopping-cart-remove' : 'shopping-cart-add'"
      size="medium"
      buttonAlignment="right"
      @click="onInput"
    />
  </span>
  <label
    v-else
    :for="`${item.id}-shopping-cart-input`"
    class="xl:flex xl:justify-end px-2 py-1 rounded-base cursor-pointer items-baseline xl:items-center mt-0.5 xl:mt-0"
    :class="{
      'text-button-cart-add hover:text-button-cart-add-hover':
        !isInCart,
      'text-button-cart-remove bg-button-cart-remove': isInCart,
    }"
  >
    <BaseIcon
      :name="isInCart ? 'shopping-cart-remove' : 'shopping-cart-add'"
      :width="21"
    />
    <span class="sr-only">Add to cart</span>
    <input
      type="checkbox"
      :id="`${item.id}-shopping-cart-input`"
      class="sr-only"
      :modelValue="isInCart"
      @input="onInput"
    />
  </label>
</template>

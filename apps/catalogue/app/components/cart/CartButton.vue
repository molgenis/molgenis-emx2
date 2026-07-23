<script setup lang="ts">
import { computed } from "vue";
import BaseIcon from "../../../../tailwind-components/app/components/BaseIcon.vue";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import type { ICartItem } from "../../../interfaces/types";
import { useCartStore } from "../../stores/useCartStore";

const props = withDefaults(
  defineProps<{
    item: ICartItem;
    variant?: "button" | "icon";
    size?: "tiny" | "small" | "medium" | "large";
  }>(),
  {
    variant: "icon",
    size: "medium",
  }
);

const cartStore = useCartStore();

const isInCart = computed(() => {
  return cartStore.isInCart(props.item.id);
});

const label = computed(() =>
  isInCart.value ? "Remove from cart" : "Add to cart"
);

function onInput() {
  if (isInCart.value) {
    cartStore.removeFromCart(props.item.id);
  } else {
    cartStore.addToCart(props.item);
  }
}
</script>
<template>
  <Button
    v-if="variant === 'button'"
    :type="isInCart ? 'secondary' : 'primary'"
    :label="label"
    :icon="isInCart ? 'shopping-cart-remove' : 'shopping-cart-add'"
    :size="size"
    buttonAlignment="right"
    @click="onInput"
  />
  <label
    v-else
    class="inline-flex items-center px-2 py-1 rounded-base cursor-pointer align-middle"
    :class="{
      'text-button-cart-add hover:text-button-cart-add-hover': !isInCart,
      'text-button-cart-remove bg-button-cart-remove': isInCart,
    }"
  >
    <BaseIcon
      :name="isInCart ? 'shopping-cart-remove' : 'shopping-cart-add'"
      :width="21"
    />
    <span class="sr-only">{{ label }}</span>
    <input type="checkbox" class="sr-only" :checked="isInCart" @input="onInput" />
  </label>
</template>

<script setup lang="ts">
import { computed } from "vue";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import InputCheckboxIcon from "../../../../tailwind-components/app/components/input/CheckboxIcon.vue";
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
  isInCart.value
    ? `Remove ${props.item.label} from cart`
    : `Add ${props.item.label} to cart`
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
    class="group inline-flex items-center px-1 py-1 rounded-base cursor-pointer align-middle"
    :class="{ 'bg-button-filter': isInCart }"
  >
    <input
      type="checkbox"
      class="peer sr-only"
      :checked="isInCart"
      @change="onInput"
    />
    <InputCheckboxIcon :checked="isInCart" class="!mr-0" />
    <span class="sr-only">{{ label }}</span>
  </label>
</template>

<template>
  <Button
    label="Request"
    size="small"
    icon="shopping-cart"
    @click="$emit('click')"
  >
    <span class="inline-block w-6 h-6 bg-button-cart-count rounded-full">
      <span
        class="body-xs/1 text-button-cart-count flex items-center justify-center pl-0.5 w-full h-full leading-none"
      >
        {{ numberOfItemsInStore }}
      </span>
    </span>
  </Button>
</template>

<script lang="ts" setup>
import { computed } from "vue";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import { useCartStore } from "../../stores/useCartStore";

const cartStore = useCartStore();

defineEmits<{ (e: "click"): void }>();

const numberOfItemsInStore = computed<number | string>(() => {
  const count = Object.keys(cartStore.datasets).length;
  if (count > 99) {
    return "99+";
  } else {
    return count;
  }
});
</script>

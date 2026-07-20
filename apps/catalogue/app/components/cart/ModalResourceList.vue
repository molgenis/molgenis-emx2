<template>
  <ul class="list-style-none">
    <li
      v-for="resource in cartStore.datasets"
      class="border-b-[1px] mb-2 last:border-none last:mb-none"
    >
      <CartModalResourceListItem :resource="resource" />
    </li>
  </ul>
</template>

<script setup lang="ts">
import { useCartStore } from "../../stores/useCartStore";
import CartModalResourceListItem from "./ModalResourceListItem.vue";
const cartStore = useCartStore();
</script>

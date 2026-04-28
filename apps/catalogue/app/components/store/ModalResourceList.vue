<template>
  <ul class="list-style-none">
    <li
      v-for="resource in datasetStore.datasets"
      class="border-b-[1px] border-b-gray-200 mb-2 last:border-none last:mb-none"
    >
      <StoreModalResourceListItem :resource="resource" />
    </li>
  </ul>
</template>

<script setup lang="ts">
import { useDatasetStore } from "../../stores/useDatasetStore";
import StoreModalResourceListItem from "./ModalResourceListItem.vue";
const datasetStore = useDatasetStore();
</script>

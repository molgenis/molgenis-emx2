<template>
  <ul class="list-style-none">
    <li
      v-for="resource in datasetStore.datasets.value"
      class="border-b-[1px] border-b-gray-200 mb-2 last:border-none last:mb-none"
    >
      <StoreModalResourceListItem :resource="(resource as IResources)" />
    </li>
  </ul>
</template>

<script setup lang="ts">
import type { IResources } from "~/interfaces/catalogue";
import { useDatasetStore } from "~/stores/useDatasetStore";
const datasetStore = useDatasetStore();
</script>

<template>
  <ul class="list-style-none">
    <li
      v-for="resource in datasetStore.datasets.value"
      class="border-b-[1px] border-b-gray-200 mb-2 last:border-none last:mb-none"
    >
      <div class="flex justfy-center items-center">
        <div class="grow mb-2">
          <span class="block font-bold">{{ (resource as IResources).id }}</span>
          <span>{{ (resource as IResources).name }}</span>
        </div>
        <div>
          <IconButton
            icon="trash"
            @click="() => datasetStore.removeFromCart((resource as IResources).id)"
            class="text-red-500"
            label="remove from cart"
          />
        </div>
      </div>
    </li>
  </ul>
</template>

<script setup lang="ts">
import type { IResources } from "~/interfaces/catalogue";
const datasetStore = useDatasetStore();
</script>

<script setup lang="ts">
import type { HarmonisationStatus } from "~/interfaces/types";

const props = defineProps<{
  collectionsWithMapping: {
    collection: { id: string };
    status: HarmonisationStatus | HarmonisationStatus[];
  }[];
}>();

const aggregatedHarmonisationStatus = computed(() => {
  return props.collectionsWithMapping
    .map((cwm) => {
      const status = Array.isArray(cwm.status) ? cwm.status : [cwm.status];
      return { collection: cwm.collection, status };
    })
    .map((statusPerCollection) => {
      if (
        statusPerCollection.status.includes("partial") ||
        statusPerCollection.status.includes("complete")
      ) {
        if (
          statusPerCollection.status.includes("partial") ||
          statusPerCollection.status.includes("unmapped")
        ) {
          // at least one partial or complete
          return {
            collection: statusPerCollection.collection,
            status: "partial",
          };
        } else {
          // all complete
          return {
            collection: statusPerCollection.collection,
            status: "complete",
          };
        }
      } else {
        // no mappings
        return {
          collection: statusPerCollection.collection,
          status: "unmapped",
        };
      }
    });
});
</script>
<template>
  <div class="grid grid-cols-3 gap-4">
    <div>
      <ul>
        <li
          v-for="{ collection, status } in aggregatedHarmonisationStatus.slice(
            0,
            10
          )"
          class="pb-2"
        >
          <div class="flex items-center gap-2">
            <HarmonisationStatusIcon :status="status" size="large" />
            <span>{{ collection.id }}</span>
          </div>
        </li>
      </ul>
    </div>
    <div>
      <ul>
        <li
          v-for="{ collection, status } in aggregatedHarmonisationStatus.slice(
            10,
            20
          )"
          class="pb-2"
        >
          <div class="flex items-center gap-2">
            <HarmonisationStatusIcon :status="status" size="large" />
            <span>{{ collection.id }}</span>
          </div>
        </li>
      </ul>
    </div>
    <div>
      <ul>
        <li
          v-for="{ collection, status } in aggregatedHarmonisationStatus.slice(
            20,
            30
          )"
          class="pb-2"
        >
          <div class="flex items-center gap-2">
            <HarmonisationStatusIcon :status="status" size="large" />
            <span>{{ collection.id }}</span>
          </div>
        </li>
      </ul>
    </div>
  </div>
  <HarmonisationLegendDatailed class="mt-12" />
</template>

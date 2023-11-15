<script setup lang="ts">
import type { HarmonizationStatus } from "~/interfaces/types";

defineProps<{
  cohortsWithMapping: { cohort: { id: string }; status: HarmonizationStatus }[];
}>();
</script>
<template>
  <div class="grid grid-cols-3 gap-4">
    <div>
      <ul>
        <li
          v-for="{ cohort, status } in cohortsWithMapping.slice(0, 10)"
          class="pb-2"
        >
          <div class="flex items-center gap-2">
            <HarmonizationStatusIcon :status="status" />
            <span>{{ cohort.id }}</span>
          </div>
        </li>
      </ul>
    </div>
    <div>
      <ul>
        <li
          v-for="{ cohort, status } in cohortsWithMapping.slice(10, 20)"
          class="pb-2"
        >
          <div class="flex items-center gap-2">
            <HarmonizationStatusIcon :status="status" />
            <span>{{ cohort.id }}</span>
          </div>
        </li>
      </ul>
    </div>
    <div>
      <ul>
        <li
          v-for="{ cohort, status } in cohortsWithMapping.slice(20, 30)"
          class="pb-2"
        >
          <div class="flex items-center gap-2">
            <HarmonizationStatusIcon :status="status" />
            <span>{{ cohort.id }}</span>
          </div>
        </li>
      </ul>
    </div>
  </div>
  <HarmonizationLegend class="mt-12" />
</template>

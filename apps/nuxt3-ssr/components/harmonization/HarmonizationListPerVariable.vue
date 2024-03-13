<script setup lang="ts">
import type { HarmonizationStatus } from "~/interfaces/types";

const props = defineProps<{
  cohortsWithMapping: {
    cohort: { id: string };
    status: HarmonizationStatus | HarmonizationStatus[];
  }[];
}>();

const aggregatedHarmonizationStatus = computed(() => {
  return props.cohortsWithMapping
    .map((cwm) => {
      const status = Array.isArray(cwm.status) ? cwm.status : [cwm.status];
      return { cohort: cwm.cohort, status };
    })
    .map((statusPerCohort) => {
      if (
        statusPerCohort.status.includes("partial") ||
        statusPerCohort.status.includes("complete")
      ) {
        if (
          statusPerCohort.status.includes("partial") ||
          statusPerCohort.status.includes("unmapped")
        ) {
          // at least one partial or complete
          return { cohort: statusPerCohort.cohort, status: "partial" };
        } else {
          // all complete
          return { cohort: statusPerCohort.cohort, status: "complete" };
        }
      } else {
        // no mappings
        return { cohort: statusPerCohort.cohort, status: "unmapped" };
      }
    });
});
</script>
<template>
  <div class="grid grid-cols-3 gap-4">
    {{}}
    <div>
      <ul>
        <li
          v-for="{ cohort, status } in aggregatedHarmonizationStatus.slice(
            0,
            10
          )"
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
          v-for="{ cohort, status } in aggregatedHarmonizationStatus.slice(
            10,
            20
          )"
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
          v-for="{ cohort, status } in aggregatedHarmonizationStatus.slice(
            20,
            30
          )"
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

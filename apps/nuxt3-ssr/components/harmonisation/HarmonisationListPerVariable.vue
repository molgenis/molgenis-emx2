<script setup lang="ts">
import type { HarmonisationStatus } from "~/interfaces/types";

const props = defineProps<{
  cohortsWithMapping: {
    cohort: { id: string };
    status: HarmonisationStatus | HarmonisationStatus[];
  }[];
}>();

const aggregatedHarmonisationStatus = computed(() => {
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
    <div>
      <ul>
        <li
          v-for="{ cohort, status } in aggregatedHarmonisationStatus.slice(
            0,
            10
          )"
          class="pb-2"
        >
          <div class="flex items-center gap-2">
            <HarmonisationStatusIcon :status="status" size="large" />
            <span>{{ cohort.id }}</span>
          </div>
        </li>
      </ul>
    </div>
    <div>
      <ul>
        <li
          v-for="{ cohort, status } in aggregatedHarmonisationStatus.slice(
            10,
            20
          )"
          class="pb-2"
        >
          <div class="flex items-center gap-2">
            <HarmonisationStatusIcon :status="status" size="large" />
            <span>{{ cohort.id }}</span>
          </div>
        </li>
      </ul>
    </div>
    <div>
      <ul>
        <li
          v-for="{ cohort, status } in aggregatedHarmonisationStatus.slice(
            20,
            30
          )"
          class="pb-2"
        >
          <div class="flex items-center gap-2">
            <HarmonisationStatusIcon :status="status" size="large" />
            <span>{{ cohort.id }}</span>
          </div>
        </li>
      </ul>
    </div>
  </div>
  <HarmonisationLegendDatailed class="mt-12" />
</template>

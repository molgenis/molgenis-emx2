<script setup lang="ts">
import type { IVariable, IVariableMappings } from "~/interfaces/types";

type VariableDetailsWithMapping = IVariable & IVariableMappings;
const props = defineProps<{
  variable: VariableDetailsWithMapping;
  cohorts: { id: string }[];
}>();

const cohortsWithMapping = computed(() => {
  return props.cohorts
    .map((cohort) => {
      const status = calcHarmonizationStatus([props.variable], [cohort])[0][0];
      return {
        cohort,
        status,
      };
    })
    .filter(({ status }) => status !== "unmapped");
});
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

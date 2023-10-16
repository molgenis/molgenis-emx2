<script setup lang="ts">
import type { IVariable, IVariableMappings } from "~/interfaces/types";

type VariableDetailsWithMapping = IVariable & IVariableMappings;
const props = defineProps<{
  variable: VariableDetailsWithMapping;
  cohorts: { id: string }[];
}>();

const harmonizationPerCohort = computed(() => {
  return props.cohorts.map((cohort) => {
    if (!Array.isArray(props.variable.mappings)) {
      // no mapping
      return "unmapped";
    } else if (props.variable.repeats) {
      // handle repeats
      return calcStatusForRepeatingVariable(props.variable, cohort);
    } else {
      // handle non repeating
      return calcStatusForSingleVariable(props.variable, cohort);
    }
  });
});
</script>
<template>
  <div class="grid grid-cols-3 gap-4">
    <div>
      <ul>
        <li v-for="(cohort, index) in cohorts.slice(0, 10)" class="pb-2">
          <div class="flex items-center gap-2">
            <HarmonizationStatusIcon :status="harmonizationPerCohort[index]" />
            <span>{{ cohort.id }}</span>
          </div>
        </li>
      </ul>
    </div>
    <div>
      <ul>
        <li v-for="(cohort, index) in cohorts.slice(10, 20)" class="pb-2">
          <div class="flex items-center gap-2">
            <HarmonizationStatusIcon :status="harmonizationPerCohort[index]" />
            <span>{{ cohort.id }}</span>
          </div>
        </li>
      </ul>
    </div>
    <div>
      <ul>
        <li v-for="(cohort, index) in cohorts.slice(20, 30)" class="pb-2">
          <div class="flex items-center gap-2">
            <HarmonizationStatusIcon :status="harmonizationPerCohort[index]" />
            <span>{{ cohort.id }}</span>
          </div>
        </li>
      </ul>
    </div>
  </div>
  <HarmonizationLegend class="mt-12" />
</template>

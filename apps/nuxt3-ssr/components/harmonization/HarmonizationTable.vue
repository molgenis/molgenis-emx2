<script setup lang="ts">
import type { ICohort, IMapping } from "~/interfaces/types";

interface IVariableWithMapping {
  name: string;
  label: string;
  description: string;
  mappings?: IMapping[];
  repeats?: {
    name: string;
    mappings: IMapping[];
  }[];
}

const props = defineProps<{
  variables: IVariableWithMapping[];
  cohorts: ICohort[];
}>();

const statusMap = computed(() => {
  return props.variables.map((v) => {
    return props.cohorts.map((c) => {
      if (!Array.isArray(v.mappings)) {
        // no mapping
        return "unmapped";
      } else if (v.repeats) {
        // handle repeats
        return calcStatusForRepeatingVariable(v, c);
      } else {
        // handle non repeating
        return calcStatusForSingleVariable(v, c);
      }
    });
  });
});
</script>

<template>
  <div class="pb-5 relative">
    <table class="table-auto">
      <thead>
        <tr class="border-y-2">
          <th></th>
          <th
            v-for="cohort in cohorts"
            class="align-bottom hover:bg-button-outline-hover border-x-2"
          >
            <div class="rotate-180 [writing-mode:vertical-lr]">
              {{ cohort.id }}
            </div>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(variable, rowIndex) in variables" class="border-b-2">
          <td class="border-r-2">{{ variable.name }}</td>
          <HarmonizationTableCell
            v-for="(_, colIndex) in cohorts"
            :status="statusMap[rowIndex][colIndex]"
          ></HarmonizationTableCell>
        </tr>
      </tbody>
    </table>
  </div>
</template>

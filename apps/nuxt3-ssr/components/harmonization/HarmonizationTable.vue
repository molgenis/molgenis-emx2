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
  <!-- temp 'fix' for table y overflow -->
  <div class="pb-5 relative">
    <div class="flex flex-row-reverse items-center h-16">
      <a class="ml-6 mr-4 hover:underline text-blue-500 cursor-help"
        >About statuses</a
      >
      No Data
      <HarmonizationStatusIcon status="unmapped" class="mr-1 ml-6" /> Completed
      <HarmonizationStatusIcon status="complete" class="mr-1 ml-6" /> Partial
      <HarmonizationStatusIcon status="partial" class="mr-1 ml-6" />
    </div>

    <div class="overflow-x-auto max-w-table">
      <table class="table-auto">
        <thead>
          <tr class="border-y-2">
            <th></th>
            <th
              v-for="cohort in cohorts"
              class="align-bottom hover:bg-button-outline-hover border-x-2"
            >
              <div
                class="text-blue-500 font-normal rotate-180 [writing-mode:vertical-lr] py-2"
              >
                {{ cohort.id }}
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(variable, rowIndex) in variables"
            class="border-b-2 hover:bg-button-outline-hover"
          >
            <td class="text-blue-500 border-r-2 px-2">{{ variable.name }}</td>
            <HarmonizationTableCell
              v-for="(_, colIndex) in cohorts"
              :status="statusMap[rowIndex][colIndex]"
            ></HarmonizationTableCell>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

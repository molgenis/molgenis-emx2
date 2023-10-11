<script setup lang="ts">
import type { ICohort, IVariable } from "~/interfaces/types";
const { variables, cohorts } = defineProps<{
  variables: IVariable[];
  cohorts: ICohort[];
}>();

const statusMap = variables.map((variable) => {
  return cohorts.map((cohort) => {
    return Math.random() > 0.5 ? "g" : Math.random() > 0.5 ? "" : "r";
  });
});
</script>

<template>
  <div class="pb-5">
    <table class="table-auto">
      <thead>
        <tr class="border-y-2">
          <th></th>
          <th
            v-for="(cohort, colIndex) in cohorts"
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
          <td v-for="(cohort, colIndex) in cohorts" class="text-center">
            {{ statusMap[rowIndex][colIndex] }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

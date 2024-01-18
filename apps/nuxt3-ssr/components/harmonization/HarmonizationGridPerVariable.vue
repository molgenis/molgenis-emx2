<script setup lang="ts">
import type {
  HarmonizationStatus,
  IVariable,
  IVariableMappings,
} from "~/interfaces/types";
import StickyTable from "../table/StickyTable.vue";

type VariableDetailsWithMapping = IVariable & IVariableMappings;

defineProps<{
  variable: VariableDetailsWithMapping;
  cohortsWithMapping: {
    cohort: { id: string };
    status: HarmonizationStatus | HarmonizationStatus[];
  }[];
}>();
</script>
<template>
  <div class="mb-7 relative">
    <HarmonizationLegend size="small" />
    <div class="overflow-x-auto xl:max-w-table border-t">
      <StickyTable
        :columns="cohortsWithMapping"
        :rows="[variable, ...variable.repeats]"
        class="h-screen overflow-auto"
      >
        <template #column="columnProps">
          <div
            class="font-normal min-w-[2rem] rotate-180 [writing-mode:vertical-lr] max-h-title min-h-title hover:max-h-none truncate hover:text-clip hover:overflow-visible"
          >
            <span
              class="hover:flex items-center justify-items-end align-middle min-w-[2rem] hover:z-50 py-2"
            >
              {{ columnProps.value.cohort.id }}
            </span>
          </div>
        </template>

        <template #row="rowProps">
          <div
            class="text-body-base font-normal px-2 truncate hover:text-clip hover:overflow-visible"
          >
            <span class="hover:inline-block z-50">
              {{ rowProps.value.row.name }}
            </span>
          </div>
        </template>

        <template #cell="cell">
          <HarmonizationTableCellStatusIcon
            :status="(cohortsWithMapping[cell.value.columnIndex].status[cell.value.rowIndex] as HarmonizationStatus)"
          />
        </template>
      </StickyTable>
    </div>
  </div>
</template>

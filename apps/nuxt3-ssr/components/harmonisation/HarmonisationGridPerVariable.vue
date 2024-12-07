<script setup lang="ts">
import type {
  HarmonisationStatus,
  IMapping,
  IVariable,
  IVariableDetails,
  IVariableMappings,
} from "~/interfaces/types";

const props = defineProps<{
  variable: IVariableDetails & IVariableMappings;
}>();

const relevantMappings = computed(() =>
  props.variable.mappings?.filter((m) =>
    ["partial", "complete"].includes(m.match.name)
  )
);
const sourceIds = computed(() => [
  ...new Set(relevantMappings.value?.map((mapping) => mapping.source.id)),
]);
const repeats = computed(() => {
  const min = props.variable.repeatMin | 1;
  const max = props.variable.repeatMax | 1;
  return Array.from({ length: max - min }, (_, i) => min + i);
});
</script>
<template>
  <div class="relative">
    <HarmonisationLegendDetailed size="small" />
    <div class="overflow-x-auto xl:max-w-table border-t">
      <TableSticky
        :columns="sourceIds"
        :rows="repeats"
        class="h-screen overflow-auto"
      >
        <template #column="columnProps">
          <div
            class="font-normal min-w-[2rem] rotate-180 [writing-mode:vertical-lr] max-h-title min-h-title hover:max-h-none truncate hover:text-clip hover:overflow-visible"
          >
            <span
              class="hover:flex items-center justify-items-end align-middle min-w-[2rem] hover:z-50 py-2"
            >
              {{ columnProps.value }}
            </span>
          </div>
        </template>

        <template #row="rowProps">
          <div
            class="text-body-base font-normal px-2 truncate hover:text-clip hover:overflow-visible"
          >
            <span class="hover:inline-block z-50">
              {{ variable.repeatUnit.name }} {{ rowProps.value.row }}
            </span>
          </div>
        </template>

        <template #cell="cell">
          <HarmonisationTableCellStatusIcon
            :status="
              variable.mappings?.find(
                (m) =>
                  m.source.id === cell.value.column &&
                  m.repeats.includes('' + cell.value.row)
              )?.match.name || 'unmapped'
            "
          />
        </template>
      </TableSticky>
    </div>
  </div>
</template>

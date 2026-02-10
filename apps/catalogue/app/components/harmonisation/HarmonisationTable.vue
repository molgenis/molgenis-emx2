<script setup lang="ts">
import { useRoute } from "#app/composables/router";
import { computed, ref } from "vue";
import type { IResources, IVariables } from "../../../interfaces/catalogue";
import { calcAggregatedHarmonisationStatus } from "~/utils/harmonisation";
import { getKey } from "../../utils/variableUtils";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import SideModal from "../../../../tailwind-components/app/components/SideModal.vue";
import TableSticky from "../table/Sticky.vue";
import HarmonisationTableCellAvailableIcon from "./HarmonisationTableCellAvailableIcon.vue";
import VariableDisplay from "../VariableDisplay.vue";
import type { IVariableMappings } from "~~/interfaces/types";

const route = useRoute();

const props = defineProps<{
  variables: (IVariables & IVariableMappings)[];
  resources: IResources[];
}>();

const statusMap = computed(() =>
  calcAggregatedHarmonisationStatus(props.variables, props.resources)
);

let activeRowIndex = ref(-1);

// list of optional computed values that are non null when the side panel is shown
let showSidePanel = computed(() => activeRowIndex.value !== -1);
let activeVariable = computed(() =>
  showSidePanel ? props.variables[activeRowIndex.value] : null
);

let activeVariableKey = computed(() =>
  activeVariable.value ? getKey(activeVariable.value) : null
);

const catalogueParam = route.query.catalogue || route.params.resourceId;
const activeVariableUrl = computed(() => {
  if (!activeVariable.value || !activeVariableKey.value) return "";
  const variable = activeVariable.value;
  const path = `/${variable.resource.id}/datasets/${variable.dataset.name}/${variable.name}?keys=${JSON.stringify(activeVariableKey.value)}`;
  return catalogueParam ? `${path}&catalogue=${catalogueParam}` : path;
});
</script>

<template>
  <div class="mb-7 relative">
    <HarmonisationLegendMatrix size="small" />
    <div class="overflow-x-auto xl:max-w-table border-t">
      <TableSticky
        :columns="
          resources.sort((a, b) =>
            a.id.toLowerCase().localeCompare(b.id.toLowerCase())
          )
        "
        :rows="variables"
        class="h-screen overflow-auto"
      >
        <template #column="columnProps">
          <div
            class="hover:bg-gray-100 text-link font-normal min-w-[2rem] rotate-180 [writing-mode:vertical-lr] max-h-title min-h-title hover:max-h-none truncate hover:text-clip hover:overflow-visible"
          >
            <span
              class="hover:bg-gray-100 hover:flex items-center justify-items-end align-middle min-w-[2rem] hover:z-50 py-2"
            >
              {{ columnProps.value.id }}
            </span>
          </div>
        </template>

        <template #row="rowProps">
          <div
            class="text-body-base text-link font-normal hover:underline px-2 cursor-pointer truncate hover:text-clip hover:overflow-visible"
            @click="activeRowIndex = rowProps.value.rowIndex"
          >
            <span
              class="hover:bg-gray-100 hover:inline-block hover:border-r hover:pr-3 z-50"
            >
              {{ rowProps.value.row.name }}
            </span>
          </div>
        </template>

        <template #cell="cell">
          <HarmonisationTableCellAvailableIcon
            :status="
              ['complete', 'partial'].includes(
                statusMap?.[cell.value.rowIndex]?.[cell.value.columnIndex] ?? ''
              )
                ? 'available'
                : 'unmapped'
            "
            @click="activeRowIndex = cell.value.rowIndex"
          ></HarmonisationTableCellAvailableIcon>
        </template>
      </TableSticky>
    </div>

    <SideModal
      :key="activeRowIndex"
      :show="showSidePanel"
      :fullScreen="false"
      :slideInRight="true"
      @close="activeRowIndex = -1"
      buttonAlignment="right"
    >
      <template v-if="activeVariableKey">
        <VariableDisplay :variableKey="activeVariableKey" />
      </template>

      <template #footer>
        <NuxtLink
          :to="activeVariableUrl"
        >
          <Button type="primary" size="small" label="More details " />
        </NuxtLink>
      </template>
    </SideModal>
  </div>
</template>

<script setup lang="ts">
import type { ICohort, IVariableWithMappings } from "~/interfaces/types";
const route = useRoute();

const props = defineProps<{
  variables: IVariableWithMappings[];
  cohorts: ICohort[];
}>();

const statusMap = computed(() =>
  calcHarmonizationStatus(props.variables, props.cohorts)
);

let showSidePanel = computed(() => activeVariableName.value !== "");
let activeVariableName = ref("");
</script>

<template>
  <div class="pb-5 relative">
    <HarmonizationLegend class="flex-row-reverse" />

    <!-- temp 'fix' for table y overflow -->
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
            <td
              class="text-body-base text-blue-500 hover:underline hover:bg-blue-50 border-r-2 px-2 cursor-pointer"
              @click="activeVariableName = variable.name"
            >
              {{ variable.name }}
            </td>
            <HarmonizationTableCell
              v-for="(_, colIndex) in cohorts"
              :status="statusMap[rowIndex][colIndex]"
            ></HarmonizationTableCell>
          </tr>
        </tbody>
      </table>
    </div>

    <SideModal
      :key="activeVariableName"
      :show="showSidePanel"
      :fullScreen="false"
      :slideInRight="true"
      @close="activeVariableName = ''"
      buttonAlignment="right"
    >
      <VariableDisplay :name="activeVariableName" />

      <template #footer>
        <NuxtLink
          :to="`/${route.params.schema}/ssr-catalogue/variables/${activeVariableName}`"
        >
          <Button type="primary" size="small" label="More details " />
        </NuxtLink>
      </template>
    </SideModal>
  </div>
</template>

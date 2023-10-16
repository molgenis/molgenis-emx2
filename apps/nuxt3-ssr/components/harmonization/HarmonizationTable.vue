<script setup lang="ts">
import type { ICohort, IVariableWithMappings } from "~/interfaces/types";
const route = useRoute();

const props = defineProps<{
  variables: IVariableWithMappings[];
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

let showSidePanel = computed(() => activeVariableName.value !== "");
let activeVariableName = ref("");
</script>

<template>
  <!-- temp 'fix' for table y overflow -->
  <div class="pb-5 relative">
    <HarmonizationLegend class="flex-row-reverse" />

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
              class="text-blue-500 border-r-2 px-2"
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

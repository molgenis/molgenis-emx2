<script setup lang="ts">
import type { IVariable, IVariableMappings } from "~/interfaces/types";

type VariableDetailsWithMapping = IVariable & IVariableMappings;
const props = defineProps<{
  variable: VariableDetailsWithMapping;
  cohorts: { id: string }[];
}>();

const activeIndex = ref(0);

const statusPerCohort = computed(
  () => calcHarmonizationStatus([props.variable], props.cohorts)[0]
);

const activeMappingDescription = computed(() => {
  const activeCohortId = props.cohorts[activeIndex.value].id;
  if (!props.variable.mappings) {
    return "No description";
  } else {
    const activeCohortMapping = props.variable?.mappings.find(
      (mapping) => mapping.sourceDataset.resource.id === activeCohortId
    );
    return activeCohortMapping?.description;
  }
});
</script>

<template>
  <div class="mb-5 pb-[50px]">
    <div class="overflow-x-scroll max-w-5xl">
      <div class="flex flex-nowrap">
        <Tab
          v-for="(cohort, index) in cohorts"
          :active="index === activeIndex"
          @click="activeIndex = index"
        >
          {{ cohort.id }}
        </Tab>
      </div>
    </div>
  </div>

  <!-- {{ variable.mappings }} -->
  <!-- :items="[
      {
        label: 'Cohort',
        content: cohorts[activeIndex].id,
      },
      {
        label: 'Harmonization status',
        content: statusPerCohort[activeIndex],
      },
      {
        label: 'Description',
        content: activeMappingDescription,
      },
      {
        label: 'Variables used',
        content: 'None',
      },
      {
        label: 'Syntax',
        content: variable.mappings?.find(
          (mapping) => mapping.sourceDataset.resource.id === cohorts[activeIndex].id
        )?.syntax || 'None',
      },
    ]" -->
  <DefinitionList>
    <DefinitionListTerm>Cohort</DefinitionListTerm>
    <DefinitionListDefinition>
      {{ cohorts[activeIndex].id }}
    </DefinitionListDefinition>

    <DefinitionListTerm>Harmonization status</DefinitionListTerm>
    <DefinitionListDefinition>
      <HarmonizationStatus :status="statusPerCohort[activeIndex]" />
    </DefinitionListDefinition>

    <DefinitionListTerm>Description</DefinitionListTerm>
    <DefinitionListDefinition>{{
      activeMappingDescription
    }}</DefinitionListDefinition>

    <DefinitionListTerm>Variables used</DefinitionListTerm>
    <DefinitionListDefinition> None </DefinitionListDefinition>

    <DefinitionListTerm>Syntax</DefinitionListTerm>
    <DefinitionListDefinition>
      {{
        variable.mappings?.find(
          (mapping) =>
            mapping.sourceDataset.resource.id === cohorts[activeIndex].id
        )?.syntax || "None"
      }}
    </DefinitionListDefinition>
  </DefinitionList>
</template>

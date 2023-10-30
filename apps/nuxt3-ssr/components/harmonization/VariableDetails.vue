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

const activeCohortId = computed(() => props.cohorts[activeIndex.value].id);

const activeMappingDescription = computed(() => {
  const activeCohortMapping = props.variable?.mappings?.find(
    (mapping) => mapping.sourceDataset.resource.id === activeCohortId.value
  );
  return activeCohortMapping?.description || "No description";
});

const variablesUsed = computed(() => {
  return props.variable.mappings
    ?.filter(
      (mapping) => mapping.sourceDataset.resource.id === activeCohortId.value
    )
    .map((mapping) => {
      return [
        ...mapping.sourceVariables.map((variable) => variable.name),
        ...mapping.sourceVariablesOtherDatasets.map(
          (variable) => variable.name
        ),
      ];
    });
});
</script>

<template>
  <div class="mb-5 pb-[50px]">
    <HorizontalScrollHelper add-fade add-scroll-button>
      <div class="flex flex-nowrap">
        <Tab
          v-for="(cohort, index) in cohorts"
          :active="index === activeIndex"
          @click="activeIndex = index"
        >
          {{ cohort.id }}
        </Tab>
      </div>
    </HorizontalScrollHelper>
  </div>

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

    <DefinitionListDefinition v-if="variable.mappings">
      {{ variablesUsed }}
    </DefinitionListDefinition>
    <DefinitionListDefinition v-else>None</DefinitionListDefinition>

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

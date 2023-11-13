<script setup lang="ts">
import type { IVariable, IVariableMappings } from "~/interfaces/types";

type VariableDetailsWithMapping = IVariable & IVariableMappings;
const props = defineProps<{
  variable: VariableDetailsWithMapping;
  cohorts: { id: string }[];
}>();

const cohortsWithMapping = computed(() => {
  return props.cohorts
    .map((cohort) => {
      const status = calcHarmonizationStatus([props.variable], [cohort])[0][0];
      return {
        cohort,
        status,
      };
    })
    .filter(({ status }) => status !== "unmapped");
});

const activeIndex = ref(0);

const statusPerCohort = computed(
  () => calcHarmonizationStatus([props.variable], cohortsWithMapping.value.map((cwm => cwm.cohort)))[0]
);

const activeCohortId = computed(() => cohortsWithMapping.value[activeIndex.value].cohort.id);

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
      const sourceVariables = mapping.sourceVariables
        ? mapping.sourceVariables.map((variable: { name: string }) => {
            return {
              name: variable.name,
              resource: {
                id: mapping.source.id,
              },
              dataset: mapping.sourceDataset,
            };
          })
        : [];
      const sourceVariablesOtherDatasets = mapping.sourceVariablesOtherDatasets
        ? mapping.sourceVariablesOtherDatasets
        : [];
      return [...sourceVariables, ...sourceVariablesOtherDatasets];
    })
    .flatMap((x) => x);
});

const syntax = computed(() => {
  return props.variable.mappings?.find(
    (mapping) => mapping.sourceDataset.resource.id === activeCohortId.value
  )?.syntax;
});

let activeVariableKey = ref();
let showSidePanel = computed(() => activeVariableKey.value?.name);
</script>

<template>
  <div class="mb-5 pb-[50px]">
    <HorizontalScrollHelper add-fade add-scroll-button>
      <div class="flex flex-nowrap">
        <Tab
          v-for="(cohortWithMapping, index) in cohortsWithMapping"
          :active="index === activeIndex"
          @click="activeIndex = index"
        >
          {{ cohortWithMapping.cohort.id }}
        </Tab>
      </div>
    </HorizontalScrollHelper>
  </div>

  <DefinitionList>
    <DefinitionListTerm>Cohort</DefinitionListTerm>
    <DefinitionListDefinition>
      {{ cohortsWithMapping[activeIndex].cohort.id }}
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
      <ul>
        <li v-for="variableUsed in variablesUsed">
          <a
            class="text-body-base text-blue-500 hover:underline hover:bg-blue-50 cursor-pointer"
            @click="activeVariableKey = getKey(variableUsed)"
          >
            <BaseIcon
              name="caret-right"
              class="inline"
              style="margin-left: -8px"
            />
            {{ variableUsed.name }}
          </a>
        </li>
      </ul>
    </DefinitionListDefinition>
    <DefinitionListDefinition v-else>None</DefinitionListDefinition>

    <DefinitionListTerm>Syntax</DefinitionListTerm>
    <DefinitionListDefinition v-if="!syntax">None</DefinitionListDefinition>
  </DefinitionList>
  <CodeBlock v-if="syntax" class="mt-2">{{ syntax }}</CodeBlock>

  <SideModal
    :key="JSON.stringify(activeVariableKey)"
    :show="showSidePanel"
    :fullScreen="false"
    :slideInRight="true"
    @close="activeVariableKey = null"
    buttonAlignment="right"
  >
    <template v-if="activeVariableKey">
      <VariableDisplay :variableKey="activeVariableKey" />
    </template>
  </SideModal>
</template>

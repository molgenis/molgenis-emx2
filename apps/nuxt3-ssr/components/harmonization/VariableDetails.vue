<script setup lang="ts">
import type {
  HarmonizationStatus,
  IMapping,
  IVariable,
  IVariableBase,
  IVariableMappings,
} from "~/interfaces/types";

type VariableDetailsWithMapping = IVariable & IVariableMappings;
const props = defineProps<{
  variable: VariableDetailsWithMapping;
  cohortsWithMapping: { cohort: { id: string }; status: HarmonizationStatus }[];
}>();

const activeTabIndex = ref(0);

const statusPerCohort = computed(() =>
  calcIndividualVariableHarmonizationStatus(
    props.variable,
    props.cohortsWithMapping.map((cwm) => cwm.cohort)
  )
);

const activeCohortId = computed(
  () => props.cohortsWithMapping[activeTabIndex.value].cohort.id
);

const activeMappingDescriptions = computed(() => {
  if (props.variable.repeats) {
    const baseMapping = props.variable.mappings?.find(
      (mapping) => mapping.sourceDataset.resource.id === activeCohortId.value
    )?.description;
    const repeatMappings = props.variable.repeats.map((repeat) => {
      return repeat.mappings?.find(
        (mapping) => mapping.sourceDataset.resource.id === activeCohortId.value
      )?.description;
    });
    return [baseMapping, ...repeatMappings];
  } else {
    return [
      props.variable.mappings?.find(
        (mapping) => mapping.sourceDataset.resource.id === activeCohortId.value
      )?.description,
    ];
  }
});

const variablesUsed = computed(() => {
  function toVariablesUsed(mappings: IMapping[] | undefined) {
    if (!mappings) return [];
    return mappings
      ?.filter(
        (mapping) => mapping.sourceDataset.resource.id === activeCohortId.value
      )
      .map((mapping) => {
        const sourceVariables = mapping.sourceVariables
          ? mapping.sourceVariables.map((sourceVariable: IVariableBase) => {
              return {
                name: sourceVariable.name,
                resource: {
                  id: mapping.source.id,
                },
                dataset: mapping.sourceDataset,
                mg_tableclass: sourceVariable.mg_tableclass,
              };
            })
          : [];
        const sourceVariablesOtherDatasets =
          mapping.sourceVariablesOtherDatasets
            ? mapping.sourceVariablesOtherDatasets
            : [];
        return [...sourceVariables, ...sourceVariablesOtherDatasets];
      })
      .flatMap((x) => x);
  }
  const baseVariableVariablesUsed = toVariablesUsed(props.variable.mappings);

  return props.variable.repeats
    ? [
        baseVariableVariablesUsed,
        ...props.variable.repeats.map((repeat) =>
          toVariablesUsed(repeat.mappings)
        ),
      ]
    : [baseVariableVariablesUsed];
});

const syntaxList = computed(() => {
  const baseSyntax = props.variable.mappings?.find(
    (mapping) => mapping.sourceDataset.resource.id === activeCohortId.value
  )?.syntax;

  if (props.variable.repeats) {
    const repeatsSyntax = props.variable.repeats.map((repeat) => {
      return repeat.mappings?.find(
        (mapping) => mapping.sourceDataset.resource.id === activeCohortId.value
      )?.syntax;
    });

    return [baseSyntax, ...repeatsSyntax];
  } else {
    return [baseSyntax];
  }
});

let activeVariableUsedKey = ref();
let activeVariableIsRepeated = ref();
let showSidePanel = computed(() => activeVariableUsedKey.value?.name);

function handleVariableUsedClick(variableUsed: IVariableBase) {
  activeVariableUsedKey.value = getKey(variableUsed);
  activeVariableIsRepeated.value =
    variableUsed.mg_tableclass?.includes("Repeated");
}

const variableList = props.variable.repeats
  ? [props.variable, ...props.variable.repeats]
  : [props.variable];
</script>

<template>
  <div class="mb-5 pb-[50px]">
    <HorizontalScrollHelper add-fade add-scroll-button>
      <div class="flex flex-nowrap">
        <Tab
          v-for="(cohortWithMapping, index) in cohortsWithMapping"
          :active="index === activeTabIndex"
          @click="activeTabIndex = index"
        >
          {{ cohortWithMapping.cohort.id }}
        </Tab>
      </div>
    </HorizontalScrollHelper>
  </div>

  <template v-for="(variable, repeatIndex) in variableList">
    <DefinitionList>
      <DefinitionListTerm>Name</DefinitionListTerm>
      <DefinitionListDefinition>
        {{ variable.name }}
      </DefinitionListDefinition>
      <DefinitionListTerm>Harmonization status</DefinitionListTerm>
      <DefinitionListDefinition>
        <HarmonizationStatus
          :status="statusPerCohort[activeTabIndex][repeatIndex]"
        />
      </DefinitionListDefinition>

      <DefinitionListTerm>Description</DefinitionListTerm>
      <DefinitionListDefinition>{{
        activeMappingDescriptions[repeatIndex] || "None"
      }}</DefinitionListDefinition>
      <DefinitionListTerm>Variables used</DefinitionListTerm>

      <DefinitionListDefinition v-if="variable.mappings">
        <ul>
          <li v-if="!variablesUsed[repeatIndex]?.length">None</li>
          <template v-else>
            <li v-for="variableUsed in variablesUsed[repeatIndex]">
              <a
                class="text-body-base text-blue-500 hover:underline hover:bg-blue-50 cursor-pointer"
                @click="handleVariableUsedClick(variableUsed)"
              >
                <BaseIcon
                  name="caret-right"
                  class="inline"
                  style="margin-left: -8px"
                />
                {{ variableUsed.name }}
              </a>
            </li>
          </template>
        </ul>
      </DefinitionListDefinition>
      <DefinitionListDefinition v-else>None</DefinitionListDefinition>

      <DefinitionListTerm>Syntax</DefinitionListTerm>
      <DefinitionListDefinition v-if="!syntaxList[repeatIndex]"
        >None</DefinitionListDefinition
      >
    </DefinitionList>
    <CodeBlock v-if="syntaxList[repeatIndex]" class="mt-2">{{
      syntaxList[repeatIndex]
    }}</CodeBlock>

    <hr v-if="variableList.length > 1" class="my-5" />
  </template>
  <SideModal
    :key="JSON.stringify(activeVariableUsedKey)"
    :show="showSidePanel"
    :fullScreen="false"
    :slideInRight="true"
    @close="activeVariableUsedKey = null"
    buttonAlignment="right"
  >
    <template v-if="activeVariableUsedKey">
      <RepeatedVariableDisplay
        v-if="activeVariableIsRepeated"
        :variableKey="activeVariableUsedKey"
      />
      <VariableDisplay v-else :variableKey="activeVariableUsedKey" />
    </template>
  </SideModal>
</template>

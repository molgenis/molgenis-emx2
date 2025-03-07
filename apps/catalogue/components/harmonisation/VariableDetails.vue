<script setup lang="ts">
import type {
  IVariable,
  IVariableBase,
  IVariableMappings,
} from "~/interfaces/types";

type VariableDetailsWithMapping = IVariable & IVariableMappings;
const props = defineProps<{
  variable: VariableDetailsWithMapping;
}>();

const activeTabIndex = ref(0);
const relevantMappings = computed(() =>
  props.variable.mappings?.filter(
    (m) => m.match?.name && ["partial", "complete"].includes(m.match.name)
  )
);
const sourceIds = computed(() => [
  ...new Set(
    relevantMappings.value
      ?.map((m) => m.source.id)
      .sort((a, b) => a.toLowerCase().localeCompare(b.toLowerCase()))
  ),
]);
const activeSource = computed(() => sourceIds.value[activeTabIndex.value]);
const activeMappings = computed(() =>
  relevantMappings.value
    ?.filter((m) => m.source.id === activeSource.value)
    .sort((a, b) => Number(a.repeats) - Number(b.repeats))
);
let activeVariableUsedKey = ref();
let showSidePanel = computed(() => activeVariableUsedKey.value?.name);

function handleVariableUsedClick(variableUsed: IVariableBase) {
  activeVariableUsedKey.value = getKey(variableUsed);
}
</script>

<template>
  <div class="mb-5 pb-[50px]">
    <HorizontalScrollHelper add-fade add-scroll-button>
      <div class="flex flex-nowrap">
        <Tab
          v-for="(sourceId, index) in sourceIds"
          :active="index === activeTabIndex"
          @click="activeTabIndex = index"
        >
          {{ sourceId }}
        </Tab>
      </div>
    </HorizontalScrollHelper>
  </div>
  <template v-for="mapping in activeMappings">
    <DefinitionList>
      <DefinitionListTerm>Harmonisation status</DefinitionListTerm>
      <DefinitionListDefinition>
        <HarmonisationStatus :status="mapping.match?.name" />
      </DefinitionListDefinition>

      <DefinitionListTerm v-if="variable.repeatUnit"
        >Applies to</DefinitionListTerm
      >
      <DefinitionListDefinition v-if="variable.repeatUnit">
        {{ variable.repeatUnit?.name }} {{ mapping.repeats }}
      </DefinitionListDefinition>

      <DefinitionListTerm>Harmonisation description</DefinitionListTerm>
      <DefinitionListDefinition>{{
        mapping.description || "None"
      }}</DefinitionListDefinition>
      <DefinitionListTerm>Source variables</DefinitionListTerm>

      <DefinitionListDefinition v-if="relevantMappings">
        <ul>
          <li v-if="!mapping.sourceVariables?.length">None</li>
          <template v-else>
            <li v-for="variableUsed in mapping.sourceVariables">
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

      <DefinitionListTerm>Harmonisation syntax</DefinitionListTerm>
      <DefinitionListDefinition v-if="!mapping.syntax"
        >None</DefinitionListDefinition
      >
      <DefinitionListDefinition>
        <CodeBlock v-if="mapping.syntax" class="mt-2">{{
          mapping.syntax
        }}</CodeBlock>
      </DefinitionListDefinition>
    </DefinitionList>

    <hr class="my-5" />
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
      <VariableDisplay :variableKey="activeVariableUsedKey" />
    </template>
  </SideModal>
</template>

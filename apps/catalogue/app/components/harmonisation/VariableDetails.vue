<script setup lang="ts">
import { computed, ref } from "vue";
import type {
  IVariables,
  IVariableMappings,
} from "../../../interfaces/catalogue";
import { getKey } from "../../utils/variableUtils";
import DefinitionList from "../../../../tailwind-components/app/components/DefinitionList.vue";
import DefinitionListTerm from "../../../../tailwind-components/app/components/DefinitionListTerm.vue";
import DefinitionListDefinition from "../../../../tailwind-components/app/components/DefinitionListDefinition.vue";
import HorizontalScrollHelper from "../HorizontalScrollHelper.vue";
import Tab from "../../../../tailwind-components/app/components/Tab.vue";
import SideModal from "../../../../tailwind-components/app/components/SideModal.vue";
import CodeBlock from "../CodeBlock.vue";
import VariableDisplay from "../VariableDisplay.vue";
import BaseIcon from "../../../../tailwind-components/app/components/BaseIcon.vue";
import type { HarmonisationStatus } from "../../../interfaces/types";

const props = defineProps<{
  variable: IVariables & IVariableMappings;
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

function handleVariableUsedClick(variableUsed: IVariables) {
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
        <HarmonisationStatusComponent
          :status="mapping.match?.name as HarmonisationStatus "
        />
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
                class="text-body-base text-link hover:underline hover:bg-link-hover cursor-pointer"
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

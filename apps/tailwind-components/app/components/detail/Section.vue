<script setup lang="ts">
import type { cellPayload } from "../../../types/types";
import type { RecordBox } from "../../utils/groupRecordSections";
import DefinitionList from "../DefinitionList.vue";
import DefinitionListDefinition from "../DefinitionListDefinition.vue";
import DefinitionListTerm from "../DefinitionListTerm.vue";
import DetailColumn from "./Column.vue";

defineProps<{
  section: RecordBox;
}>();

defineEmits<{
  (e: "valueClick", payload: cellPayload): void;
}>();
</script>

<template>
  <section
    :id="section.id"
    class="min-w-0 scroll-mt-[30px] bg-content py-18 lg:px-12.5 px-5 text-title-contrast xl:rounded-3px last:rounded-b-50px shadow-primary xl:border-b-0 border-b-[1px]"
  >
    <h2
      v-if="section.label && section.kind === 'section'"
      class="mb-5 uppercase text-heading-4xl font-display"
    >
      {{ section.label }}
    </h2>
    <h3 v-else-if="section.label" class="mb-4 text-heading-3xl font-display">
      {{ section.label }}
    </h3>

    <DefinitionList v-if="section.fields.length" :compact="false">
      <template v-for="field in section.fields" :key="field.id">
        <DefinitionListTerm>{{ field.label }}</DefinitionListTerm>
        <DefinitionListDefinition>
          <DetailColumn
            :metadata="field.metadata"
            :value="field.value"
            @valueClick="$emit('valueClick', $event)"
          />
        </DefinitionListDefinition>
      </template>
    </DefinitionList>
  </section>
</template>

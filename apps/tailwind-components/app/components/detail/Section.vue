<script setup lang="ts">
import type { cellPayload } from "../../../types/types";
import type { RecordSection } from "../../utils/groupRecordSections";
import DefinitionList from "../DefinitionList.vue";
import DefinitionListDefinition from "../DefinitionListDefinition.vue";
import DefinitionListTerm from "../DefinitionListTerm.vue";
import DetailColumn from "./Column.vue";

defineProps<{
  section: RecordSection;
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
      v-if="section.label"
      class="mb-5 uppercase text-heading-4xl font-display"
    >
      {{ section.label }}
    </h2>

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

    <div
      v-for="heading in section.headings"
      :key="heading.id"
      :id="heading.id"
      class="scroll-mt-[30px] mt-8"
    >
      <h3 class="mb-4 text-heading-3xl font-display">{{ heading.label }}</h3>
      <DefinitionList :compact="false">
        <template v-for="field in heading.fields" :key="field.id">
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
    </div>
  </section>
</template>

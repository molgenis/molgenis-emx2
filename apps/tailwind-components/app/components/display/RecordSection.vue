<script setup lang="ts">
import { useIntersectionObserver } from "@vueuse/core";
import { computed, ref, type ComponentPublicInstance } from "vue";
import type { cellPayload } from "../../../types/types";
import type { RecordLayout, RecordSection } from "../../../types/record";
import type { RecordList } from "../../../types/recordLists";
import ContentBlock from "../content/ContentBlock.vue";
import DefinitionList from "../DefinitionList.vue";
import DefinitionListDefinition from "../DefinitionListDefinition.vue";
import DefinitionListTerm from "../DefinitionListTerm.vue";
import ValueEMX2 from "../value/EMX2.vue";
import DisplayRecords from "./Records.vue";

const props = withDefaults(
  defineProps<{
    section: RecordSection;
    layout?: RecordLayout;
    trackInView?: boolean;
    lists?: Record<string, RecordList>;
  }>(),
  {
    layout: "CARDS",
    trackInView: false,
    lists: () => ({}),
  }
);

const ordinaryFields = computed(() =>
  props.section.fields.filter((field) => !props.lists[field.id])
);
const listFields = computed(() =>
  props.section.fields.flatMap((field) => {
    const list = props.lists[field.id];
    return list ? [{ field, list }] : [];
  })
);
const isSingleListSection = computed(
  () => props.section.fields.length === 1 && listFields.value.length === 1
);

const wrapper = computed(() =>
  props.layout === "CARDS" ? ContentBlock : "div"
);

const emit = defineEmits<{
  (e: "valueClick", payload: cellPayload): void;
  (e: "inView"): void;
}>();

const TOP_FIFTH_OF_VIEWPORT = {
  root: null,
  rootMargin: "0px 0px -80% 0px",
  threshold: 0,
};

const root = ref<ComponentPublicInstance | null>(null);
const trackedSection = computed(() => (props.trackInView ? root.value : null));

useIntersectionObserver(
  trackedSection,
  ([entry]) => {
    if (entry?.isIntersecting) {
      emit("inView");
    }
  },
  TOP_FIFTH_OF_VIEWPORT
);
</script>

<template>
  <component :is="wrapper" ref="root" :id="section.id" class="scroll-mt-7.5">
    <h2
      v-if="section.label && section.kind === 'section'"
      class="mb-5 uppercase text-heading-4xl font-display"
    >
      {{ section.label }}
    </h2>
    <h3 v-else-if="section.label" class="mb-5 text-heading-4xl font-display">
      {{ section.label }}
    </h3>

    <DefinitionList v-if="ordinaryFields.length" :compact="false">
      <template v-for="field in ordinaryFields" :key="field.id">
        <DefinitionListTerm>{{ field.label }}</DefinitionListTerm>
        <DefinitionListDefinition>
          <slot name="value" :field="field">
            <ValueEMX2
              :metadata="field.metadata"
              :data="field.value"
              @valueClick="$emit('valueClick', $event)"
            />
          </slot>
        </DefinitionListDefinition>
      </template>
    </DefinitionList>

    <div
      v-for="{ field, list } in listFields"
      :key="field.id"
      class="mt-5 first:mt-0"
    >
      <h4 v-if="!isSingleListSection" class="mb-2.5 text-record-heading">
        {{ field.label }}
      </h4>
      <DisplayRecords
        :schemaId="list.schemaId"
        :tableId="list.tableId"
        :filter="list.filter"
        :displayConfig="list.displayConfig"
      />
    </div>
  </component>
</template>

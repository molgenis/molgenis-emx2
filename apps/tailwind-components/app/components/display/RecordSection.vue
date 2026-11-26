<script setup lang="ts">
import { useIntersectionObserver } from "@vueuse/core";
import { computed, ref, type ComponentPublicInstance } from "vue";
import type { cellPayload } from "../../../types/types";
import type { RecordLayout, RecordSection } from "../../../types/record";
import type { RecordList } from "../../../types/recordConfig";
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

const isSingleFieldSection = computed(() => props.section.fields.length === 1);
const fieldLists = computed(() =>
  props.section.fields.map((field) => ({ field, list: props.lists[field.id] }))
);
const singleList = computed(() =>
  isSingleFieldSection.value
    ? props.lists[props.section.fields[0]?.id ?? ""]
    : undefined
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

    <DisplayRecords v-if="singleList" v-bind="singleList" />
    <DefinitionList v-else :compact="false">
      <template v-for="{ field, list } in fieldLists" :key="field.id">
        <div v-if="isSingleFieldSection" class="md:col-span-3">
          <slot name="value" :field="field">
            <ValueEMX2
              :metadata="field.metadata"
              :data="field.value"
              @valueClick="$emit('valueClick', $event)"
            />
          </slot>
        </div>
        <template v-else>
          <DefinitionListTerm>
            <span>{{ field.label }}</span>
          </DefinitionListTerm>
          <DefinitionListDefinition>
            <DisplayRecords v-if="list" v-bind="list" />
            <slot v-else name="value" :field="field">
              <ValueEMX2
                :metadata="field.metadata"
                :data="field.value"
                @valueClick="$emit('valueClick', $event)"
              />
            </slot>
          </DefinitionListDefinition>
        </template>
      </template>
    </DefinitionList>
  </component>
</template>

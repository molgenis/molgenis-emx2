<script setup lang="ts">
import { useIntersectionObserver } from "@vueuse/core";
import { computed, ref, type ComponentPublicInstance } from "vue";
import type { cellPayload } from "../../../types/types";
import type {
  RecordField,
  RecordLayout,
  RecordSection,
} from "../../../types/record";
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

type Block =
  | { kind: "fields"; fields: RecordField[] }
  | { kind: "list"; field: RecordField; list: RecordList };

const blocks = computed(() =>
  props.section.fields.reduce<Block[]>((result, field) => {
    const list = props.lists[field.id];
    const last = result[result.length - 1];
    if (list) {
      result.push({ kind: "list", field, list });
    } else if (last?.kind === "fields") {
      last.fields.push(field);
    } else {
      result.push({ kind: "fields", fields: [field] });
    }
    return result;
  }, [])
);
const isSingleListSection = computed(
  () => props.section.fields.length === 1 && blocks.value[0]?.kind === "list"
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

    <template
      v-for="block in blocks"
      :key="block.kind === 'list' ? block.field.id : block.fields[0]?.id"
    >
      <div v-if="block.kind === 'list'" class="mt-5 first:mt-0">
        <h4
          v-if="!isSingleListSection"
          class="mb-2.5 font-bold text-body-base first-letter:uppercase"
        >
          {{ block.field.label }}
        </h4>
        <DisplayRecords
          :schemaId="block.list.schemaId"
          :tableId="block.list.tableId"
          :filter="block.list.filter"
          :displayConfig="block.list.displayConfig"
          :pageSize="block.list.pageSize"
          :seed="block.list.seed"
          :linkTo="block.list.linkTo"
        />
      </div>
      <DefinitionList v-else :compact="false">
        <template v-for="field in block.fields" :key="field.id">
          <DefinitionListTerm>
            <span class="first-letter:uppercase">{{ field.label }}</span>
          </DefinitionListTerm>
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
    </template>
  </component>
</template>

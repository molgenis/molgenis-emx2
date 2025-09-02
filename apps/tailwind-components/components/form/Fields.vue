<script setup lang="ts">
import { onMounted, ref, useTemplateRef, watch } from "vue";
import type {
  columnId,
  columnValue,
  IColumn,
  IRow,
  ITableMetaData,
} from "../../../metadata-utils/src/types";
import {
  //todo: can we have required calculation done in useForm?
  isRequired,
} from "../../../molgenis-components/src/components/forms/formUtils/formUtils";
import { vIntersectionObserver } from "@vueuse/components";
import fetchRowPrimaryKey from "../../composables/fetchRowPrimaryKey";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    metadata: ITableMetaData;
    section?: string; //filter on active section only
    onUpdate: (column: IColumn) => void;
    onBlur: (column: IColumn) => void;
    onView: (column: IColumn) => void;
    constantValues?: IRow; //provides values that shouldn't be edited
    errorMap: Record<columnId, string>; //map of errors if available
  }>(),
  {
    onUpdate: () => {},
    onBlur: () => {},
    onView: () => {},
  }
);

const modelValue = defineModel<IRow>("modelValue", {
  required: true,
});

const container = useTemplateRef<HTMLDivElement>("container");
const visibleEntries = new Map<string, IntersectionObserverEntry>();

function onIntersectionObserver(entries: IntersectionObserverEntry[]) {
  if (!container.value) return;

  // Update currently visible entries
  for (const entry of entries) {
    if (entry.isIntersecting) visibleEntries.set(entry.target.id, entry);
    else visibleEntries.delete(entry.target.id);
  }

  if (!visibleEntries.size) return;

  const containerRect = container.value.getBoundingClientRect();
  const style = getComputedStyle(container.value);
  const paddingTop = parseFloat(style.paddingTop);
  const paddingBottom = parseFloat(style.paddingBottom);

  const containerTop = containerRect.top + paddingTop;
  const containerBottom = containerRect.bottom - paddingBottom;

  // Filter only elements fully visible inside the container
  const fullyVisible = Array.from(visibleEntries.values())
    .map((entry) => {
      const el = entry.target as HTMLElement;
      const elRect = el.getBoundingClientRect();
      const isFullyVisible =
        elRect.top >= containerTop && elRect.bottom <= containerBottom;
      return { entry, isFullyVisible, top: elRect.top };
    })
    .filter((e) => e.isFullyVisible);

  if (!fullyVisible.length) return;

  // Pick the element closest to the top of the container
  const topMost = fullyVisible.reduce((prev, curr) =>
    curr.top < prev.top ? curr : prev
  ).entry;

  // Call your callback
  if (topMost?.target.id.includes("form-field")) {
    const col = props.metadata.columns.find(
      (c) => topMost.target.id === `${c.id}-form-field`
    );
    if (col) props.onView(col);
  }
}

const rowKey = ref<columnValue>();

async function updateRowKey() {
  rowKey.value = await fetchRowPrimaryKey(
    modelValue.value,
    props.metadata.id,
    props.schemaId
  );
}

function copyConstantValuesToModelValue() {
  if (props.constantValues) {
    modelValue.value = Object.assign({}, props.constantValues);
  }
}

watch(
  () => modelValue.value,
  async () => {
    if (modelValue.value) {
      await updateRowKey();
    }
  }
);

watch(
  () => props.constantValues,
  () => {
    copyConstantValuesToModelValue();
  }
);

const fields = ref<HTMLElement[]>([]);
onMounted(() => {
  updateRowKey();
  copyConstantValuesToModelValue();
});
</script>

<template>
  <div ref="container">
    <template
      v-for="(column, index) in metadata?.columns.filter(
        (c) => !section || c.section === section || c.id === c.section
      )"
    >
      <div
        v-if="
          column.columnType === 'HEADING' || column.columnType === 'SECTION'
        "
        :id="`${column.id}-form-field`"
        v-intersection-observer="[onIntersectionObserver, { root: container }]"
      >
        <h2
          class="first:pt-0 pt-10 font-display md:text-heading-5xl text-heading-5xl text-form-header pb-8"
          v-if="column.label"
        >
          {{ column.label }}
        </h2>
      </div>
      <FormField
        ref="fields"
        class="pb-8"
        v-else-if="!Object.keys(constantValues || {}).includes(column.id)"
        v-intersection-observer="[onIntersectionObserver, { root: container }]"
        v-model="modelValue[column.id]"
        :id="`${column.id}-form-field`"
        :type="column.columnType"
        :label="column.label"
        :description="column.description"
        :rowKey="rowKey"
        :required="isRequired(column.required ?? false)"
        :error-message="errorMap[column.id]"
        :ref-schema-id="column.refSchemaId || schemaId"
        :ref-table-id="column.refTableId"
        :ref-label="column.refLabel || column.refLabelDefault"
        :ref-back-id="column.refBackId"
        :invalid="errorMap[column.id]?.length > 0"
        @update:modelValue="onUpdate(column)"
        @blur="onBlur(column)"
      />
    </template>
  </div>
</template>

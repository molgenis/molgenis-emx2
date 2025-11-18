<script setup lang="ts">
import {
  onMounted,
  useTemplateRef,
  watch,
  defineProps,
  defineEmits,
  defineModel,
} from "vue";
import type {
  columnId,
  columnValue,
  IColumn,
  IRow,
} from "../../../../metadata-utils/src/types";
import { vIntersectionObserver } from "@vueuse/components";
import FormField from "./Field.vue";

const props = defineProps<{
  columns: IColumn[];
  rowKey?: columnValue;
  constantValues?: IRow; //provides values that shouldn't be edited
  visibleMap: Record<columnId, boolean | undefined>; //provides columns to be shown, including other sections
  visibleSection: string | undefined; //id of current section
  errorMap: Record<columnId, string>; //map of errors if available
}>();

const modelValue = defineModel<IRow>("modelValue", {
  required: true,
});

const emit = defineEmits(["update", "view", "blur"]);

const container = useTemplateRef<HTMLDivElement>("container");

function onIntersectionObserver(entries: IntersectionObserverEntry[]) {
  const highest = entries.find((entry) => entry.isIntersecting);
  if (highest) {
    const col = props.columns.find(
      (c) => highest.target.id === `${c.id}-form-field`
    );
    if (col) emit("view", col);
  }
}

function copyConstantValuesToModelValue() {
  if (props.constantValues) {
    modelValue.value = Object.assign({}, props.constantValues);
  }
}

watch(
  () => props.constantValues,
  () => {
    copyConstantValuesToModelValue();
  }
);

onMounted(() => {
  copyConstantValuesToModelValue();
});

const isRequired = (value: string | boolean): boolean =>
  (typeof value === "string" && value.toLowerCase() === "true") ||
  value === true;
</script>

<template>
  <div ref="container">
    <template v-for="column in columns">
      <div
        v-if="
          (column.columnType === 'HEADING' ||
            column.columnType === 'SECTION') &&
          visibleMap &&
          visibleMap[column.id] &&
          visibleSection === column.section
        "
        :id="`${column.id}-form-field`"
        v-intersection-observer="[onIntersectionObserver, { root: container }]"
      >
        <h2
          class="first:pt-0 pt-10 font-display md:text-heading-5xl text-heading-5xl text-form-header pb-8"
          :class="
            column.columnType === 'HEADING'
              ? 'md:text-heading-4xl text-heading-4xl'
              : 'md:text-heading-5xl text-heading-5xl'
          "
          v-if="column.id != 'mg_top_of_form'"
        >
          {{ column.label }}
        </h2>
      </div>
      <FormField
        class="pb-8"
        v-else-if="
          !Object.keys(constantValues || {}).includes(column.id) &&
          visibleMap &&
          visibleMap[column.id] &&
          visibleSection === column.section
        "
        v-intersection-observer="[onIntersectionObserver, { root: container }]"
        v-model="modelValue[column.id]"
        :id="`${column.id}-form-field`"
        :type="column.columnType"
        :label="column.label"
        :description="column.description"
        :disabled="
          Boolean(column.readonly === 'true' || (rowKey && column.key === 1))
        "
        :rowKey="rowKey"
        :required="isRequired(column.required ?? false)"
        :error-message="errorMap[column.id]"
        :ref-schema-id="column.refSchemaId"
        :ref-table-id="column.refTableId"
        :ref-label="column.refLabel || column.refLabelDefault"
        :ref-back-id="column.refBackId"
        :invalid="(errorMap[column.id] || '').length > 0"
        @update:modelValue="emit('update', column)"
        @blur="emit('blur', column)"
      />
    </template>
  </div>
</template>

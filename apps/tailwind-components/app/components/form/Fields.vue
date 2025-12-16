<script setup lang="ts">
import { vIntersectionObserver } from "@vueuse/components";
import { useTemplateRef } from "vue";
import type {
  columnId,
  columnValue,
  IColumn,
  IRow,
} from "../../../../metadata-utils/src/types";
import FormField from "./Field.vue";

const props = defineProps<{
  columns: IColumn[];
  rowKey?: columnValue;
  constantValues?: IRow; //provides values that shouldn't be edited
  errorMap: Record<columnId, string>; //map of errors if available
}>();

const modelValue = defineModel<IRow>("modelValue", {
  required: true,
});

const emit = defineEmits(["update", "view", "leaving-view", "blur"]);

const container = useTemplateRef<HTMLDivElement>("container");

function onIntersectionObserver(entries: IntersectionObserverEntry[]) {
  const highest = entries.find((entry) => entry.isIntersecting);

  if (highest) {
    const col = props.columns.find(
      (c) => highest.target.id === `${c.id}-form-field`
    );
    if (col) emit("view", col);
  }

  const leaving = entries.find((entry) => entry.isIntersecting === false);
  if (leaving) {
    const col = props.columns.find(
      (c) => leaving.target.id === `${c.id}-form-field`
    );
    if (col) emit("leaving-view", col);
  }
}

const isRequired = (value: string | boolean): boolean =>
  (typeof value === "string" && value.toLowerCase() === "true") ||
  value === true;
</script>

<template>
  <template v-for="column in columns" :key="column.id">
    <div
      v-if="column.columnType === 'HEADING' || column.columnType === 'SECTION'"
      :id="`${column.id}-form-field`"
      v-intersection-observer="onIntersectionObserver"
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
      v-else-if="!Object.keys(constantValues || {}).includes(column.id)"
      v-intersection-observer="onIntersectionObserver"
      v-model="modelValue[column.id]"
      :id="`${column.id}-form-field`"
      :type="column.columnType"
      :label="column.formLabel ?? column.label"
      :description="column.description"
      :disabled="
        Boolean(
          column.readonly === 'true' ||
            (rowKey && Object.keys(rowKey).length && column.key === 1) ||
            column.columnType === 'AUTO_ID'
        )
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
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import { type UseForm } from "../../composables/useForm";
import { isFieldDisabled } from "../../utils/isFieldDisabled";
import Field from "../Field.vue";

const props = defineProps<{
  form: UseForm;
  column: IColumn;
}>();

const isRequiredColumn = computed(
  () =>
    props.form.requiredFields.value.find((c) => c.id === props.column.id) !==
    undefined
);

const isDisabledField = computed(() => {
  return isFieldDisabled(props.form.rowKey.value, props.column);
});

const fieldId = computed(
  () =>
    `${props.form.metadata.value.schemaId}-${props.form.metadata.value.id}-${props.column.id}-form-field`
);
</script>

<template>
  <Field
    class="pb-8 last:pb-64"
    v-model="form.values.value[column.id]"
    :id="fieldId"
    :type="column.columnType"
    :label="column.formLabel ?? column.label"
    :description="column.description"
    :disabled="isDisabledField"
    :rowKey="form.rowKey"
    :required="isRequiredColumn"
    :error-message="form.visibleColumnErrors.value[column.id]"
    :ref-schema-id="column.refSchemaId"
    :ref-table-id="column.refTableId"
    :ref-label="column.refLabel || column.refLabelDefault"
    :ref-back-id="column.refBackId"
    :invalid="(form.visibleColumnErrors.value[column.id] || '').length > 0"
    @input="form.onUpdateColumn(props.column)"
    @blur="form.onBlurColumn(props.column)"
  />
</template>

<script setup lang="ts">
import { computed } from "vue";
import type {
  IColumn,
  columnValue,
} from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";
import type { CountedOption } from "../../utils/fetchCounts";
import { BOOL_LABELS } from "../../utils/filterUtils";
import InputCheckboxGroup from "../input/CheckboxGroup.vue";

const props = defineProps<{
  column: IColumn;
  options: CountedOption[];
  modelValue: IFilterValue | undefined;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: IFilterValue | undefined];
}>();

const choices = Object.keys(BOOL_LABELS);

const labelledChoices = computed(() =>
  Object.entries(BOOL_LABELS).map(([value, label]) => {
    const counted = props.options.find((option) => option.name === value);
    return {
      value,
      label: counted ? `${label} (${counted.count})` : label,
    };
  })
);

const chosen = computed<columnValue[]>(() =>
  props.modelValue?.operator === "equals" &&
  Array.isArray(props.modelValue.value)
    ? props.modelValue.value
    : []
);

function onChange(selected: columnValue[] | null | undefined) {
  const ordered = choices.filter((value) => selected?.includes(value));
  emit(
    "update:modelValue",
    ordered.length > 0 ? { operator: "equals", value: ordered } : undefined
  );
}
</script>

<template>
  <InputCheckboxGroup
    :id="`filter-bool-${column.id}`"
    :options="labelledChoices"
    :model-value="chosen"
    @update:model-value="onChange"
  />
</template>

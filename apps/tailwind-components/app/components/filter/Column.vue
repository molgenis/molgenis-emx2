<script setup lang="ts">
import { computed } from "vue";
import type {
  IColumn,
  CellValueType,
  columnValue,
} from "../../../../metadata-utils/src/types";
import type { CountedOption } from "../../utils/fetchCounts";
import type { IFilterValue } from "../../../types/filters";
import { isCountableType, isRangeType } from "../../utils/filterTypes";
import FilterTree from "./Tree.vue";
import FilterRange from "./Range.vue";
import FilterText from "./Text.vue";

const props = defineProps<{
  column: IColumn;
  options: CountedOption[];
  modelValue: IFilterValue | undefined;
  loading: boolean;
  saturated?: boolean;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: IFilterValue | undefined];
}>();

const isCountable = computed(() => isCountableType(props.column.columnType));
const isRange = computed(() => isRangeType(props.column.columnType));
const rangeInputType = computed(
  () => props.column.columnType.replace(/_ARRAY$/, "") as CellValueType
);

const rangeValue = computed<[columnValue, columnValue]>(() => {
  if (!props.modelValue || props.modelValue.operator !== "between")
    return [null, null];
  return props.modelValue.value;
});

function onRangeChange(val: [columnValue, columnValue]) {
  const [min, max] = val;
  if (min === null && max === null) {
    emit("update:modelValue", undefined);
  } else {
    emit("update:modelValue", { operator: "between", value: [min, max] });
  }
}
</script>

<template>
  <div>
    <FilterTree
      v-if="isCountable"
      :column="column"
      :options="options"
      :model-value="modelValue"
      :loading="loading"
      :saturated="saturated"
      @update:model-value="emit('update:modelValue', $event)"
    />
    <FilterRange
      v-else-if="isRange"
      :id="`filter-range-${column.id}`"
      :legend="column.label || column.id"
      :input-type="rangeInputType"
      :model-value="rangeValue"
      @update:model-value="onRangeChange"
    />
    <FilterText
      v-else
      :column="column"
      :model-value="modelValue"
      @update:model-value="emit('update:modelValue', $event)"
    />
  </div>
</template>

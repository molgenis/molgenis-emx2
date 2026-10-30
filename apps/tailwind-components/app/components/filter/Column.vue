<script setup lang="ts">
import { computed } from "vue";
import type {
  IColumn,
  CellValueType,
  columnValue,
} from "../../../../metadata-utils/src/types";
import type { CountedOption } from "../../utils/fetchCounts";
import type { IFilterValue } from "../../../types/filters";
import {
  isBoolColumnType,
  isCountableType,
  isEnumColumnType,
  isRangeType,
  nonEmptyRanges,
  treeSelectionToFilterValue,
} from "../../utils/filterUtils";
import FilterBool from "./Bool.vue";
import FilterTree from "./Tree.vue";
import FilterRange from "./Range.vue";
import FilterText from "./Text.vue";

const props = defineProps<{
  column: IColumn;
  options: CountedOption[];
  modelValue: IFilterValue | undefined;
  loading: boolean;
  saturated?: boolean;
  hasCountError?: boolean;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: IFilterValue | undefined];
}>();

const isBool = computed(() => isBoolColumnType(props.column.columnType));
const showsTree = computed(
  () =>
    isCountableType(props.column.columnType) ||
    isEnumColumnType(props.column.columnType)
);
const isRange = computed(() => isRangeType(props.column.columnType));
const rangeInputType = computed(
  () => props.column.columnType.replace(/_ARRAY$/, "") as CellValueType
);

const rangeValue = computed<[columnValue, columnValue][]>(() =>
  props.modelValue?.operator === "between" ? props.modelValue.value : []
);

function onRangeChange(ranges: [columnValue, columnValue][]) {
  const filled = nonEmptyRanges(ranges);
  emit(
    "update:modelValue",
    filled.length > 0 ? { operator: "between", value: filled } : undefined
  );
}

function onTreeChange(val: IFilterValue | string[] | undefined) {
  if (Array.isArray(val)) {
    emit(
      "update:modelValue",
      treeSelectionToFilterValue(val, props.column, props.options)
    );
  } else {
    emit("update:modelValue", val);
  }
}
</script>

<template>
  <div>
    <FilterBool
      v-if="isBool"
      :column="column"
      :options="options"
      :model-value="modelValue"
      @update:model-value="emit('update:modelValue', $event)"
    />
    <FilterTree
      v-else-if="showsTree"
      :column="column"
      :options="options"
      :model-value="modelValue"
      :loading="loading"
      :saturated="saturated"
      :has-count-error="hasCountError"
      @update:model-value="onTreeChange($event)"
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

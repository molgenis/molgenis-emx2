<script setup lang="ts">
import { computed } from "vue";
import type {
  IColumn,
  CellValueType,
  columnValue,
} from "../../../../metadata-utils/src/types";
import type { CountedOption } from "../../utils/fetchCounts";
import type { IFilterValue } from "../../../types/filters";
import type { ITreeNode } from "../../../types/types";
import {
  isCountableType,
  isRangeType,
  isRefFilterType,
} from "../../utils/filterTypes";
import Tree from "../input/Tree.vue";
import FilterRange from "./Range.vue";
import InputSearch from "../input/Search.vue";
import Skeleton from "../Skeleton.vue";

const props = defineProps<{
  column: IColumn;
  options: CountedOption[];
  modelValue: IFilterValue | undefined;
  loading: boolean;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: IFilterValue | undefined];
}>();

const isCountable = computed(() => isCountableType(props.column.columnType));
const isRange = computed(() => isRangeType(props.column.columnType));
const rangeInputType = computed(
  () => props.column.columnType.replace(/_ARRAY$/, "") as CellValueType
);
const treeId = computed(() => `filter-tree-${props.column.id}`);

function countedOptionToTreeNode(option: CountedOption): ITreeNode {
  return {
    ...option,
    label: option.label
      ? `${option.label} (${option.count})`
      : `${option.name} (${option.count})`,
    children: option.children?.map(countedOptionToTreeNode) ?? [],
  };
}

const treeNodes = computed<ITreeNode[]>(() =>
  props.options.map(countedOptionToTreeNode)
);

function filterValueToTreeSelection(
  filterValue: IFilterValue | undefined
): string[] {
  if (!filterValue || filterValue.operator !== "equals") return [];
  const val = filterValue.value;
  if (!Array.isArray(val)) {
    if (typeof val === "string") return [val];
    return [];
  }
  return val
    .filter((value) => value !== null && value !== undefined)
    .map((value) => {
      if (typeof value === "object" && value !== null) {
        const values = Object.values(value as Record<string, unknown>);
        return values.length === 1
          ? String(values[0])
          : values.map(String).join(", ");
      }
      return String(value);
    });
}

function treeSelectionToFilterValue(
  selected: string[],
  column: IColumn,
  options: CountedOption[]
): IFilterValue | undefined {
  if (selected.length === 0) return undefined;

  if (
    isRefFilterType(column.columnType) &&
    options.length > 0 &&
    options[0]?.keyObject !== undefined
  ) {
    const firstKey = options[0].keyObject!;
    const isComposite = Object.keys(firstKey).length > 1;
    if (isComposite) {
      const optionsByName = new Map(
        options.map((option) => [option.name, option])
      );
      const values = selected.map((name) => {
        const option = optionsByName.get(name);
        return (option?.keyObject ?? { name }) as Record<string, unknown>;
      });
      return { operator: "equals", value: values as columnValue };
    }
  }

  return { operator: "equals", value: selected };
}

const treeSelection = computed(() =>
  filterValueToTreeSelection(props.modelValue)
);

function onTreeSelectionChange(selected: string[]) {
  emit(
    "update:modelValue",
    treeSelectionToFilterValue(selected, props.column, props.options)
  );
}

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

const textValue = computed<string>(() => {
  if (!props.modelValue || props.modelValue.operator !== "like") return "";
  return typeof props.modelValue.value === "string"
    ? props.modelValue.value
    : "";
});

function onSearchInput(val: string | number) {
  const str = String(val);
  if (!str) {
    emit("update:modelValue", undefined);
  } else {
    emit("update:modelValue", { operator: "like", value: str });
  }
}
</script>

<template>
  <div>
    <Skeleton v-if="loading && options.length === 0" :lines="4" />

    <template v-else-if="isCountable">
      <Tree
        :id="treeId"
        :nodes="treeNodes"
        :modelValue="treeSelection"
        :isMultiSelect="true"
        @update:modelValue="onTreeSelectionChange"
      />
    </template>

    <template v-else-if="isRange">
      <FilterRange
        :id="`filter-range-${column.id}`"
        :legend="column.label || column.id"
        :input-type="rangeInputType"
        :modelValue="rangeValue"
        @update:modelValue="onRangeChange"
      />
    </template>

    <template v-else>
      <InputSearch
        :id="`filter-text-${column.id}`"
        :model-value="textValue"
        @update:model-value="onSearchInput"
        :placeholder="`Search ${column.label || column.id}...`"
        :aria-label="column.label || column.id"
        size="tiny"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type {
  IColumn,
  CellValueType,
} from "../../../../metadata-utils/src/types";
import type { CountedOption } from "../../utils/fetchCounts";
import type { IFilterValue } from "../../../types/filters";
import type { ITreeNode } from "../../../types/types";
import Tree from "../input/Tree.vue";
import FilterRange from "./Range.vue";
import Input from "../Input.vue";

const props = defineProps<{
  column: IColumn;
  options: CountedOption[];
  modelValue: IFilterValue | undefined;
  loading: boolean;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: IFilterValue | undefined];
}>();

const COUNTABLE_TYPES = new Set([
  "ONTOLOGY",
  "ONTOLOGY_ARRAY",
  "BOOL",
  "RADIO",
  "CHECKBOX",
]);

const RANGE_TYPES = new Set([
  "INT",
  "INT_ARRAY",
  "DECIMAL",
  "DECIMAL_ARRAY",
  "LONG",
  "LONG_ARRAY",
  "NON_NEGATIVE_INT",
  "NON_NEGATIVE_INT_ARRAY",
  "DATE",
  "DATE_ARRAY",
  "DATETIME",
  "DATETIME_ARRAY",
]);

const isCountable = computed(() =>
  COUNTABLE_TYPES.has(props.column.columnType)
);
const isRange = computed(() => RANGE_TYPES.has(props.column.columnType));
const rangeInputType = computed(
  () => props.column.columnType.replace(/_ARRAY$/, "") as CellValueType
);

function countedOptionToTreeNode(opt: CountedOption): ITreeNode {
  return {
    ...opt,
    label: opt.label
      ? `${opt.label} (${opt.count})`
      : `${opt.name} (${opt.count})`,
    children: opt.children?.map(countedOptionToTreeNode) ?? [],
  };
}

const treeNodes = computed<ITreeNode[]>(() =>
  props.options.map(countedOptionToTreeNode)
);

const treeSelection = computed<string[]>(() => {
  if (!props.modelValue || props.modelValue.operator !== "equals") return [];
  const val = props.modelValue.value;
  if (Array.isArray(val)) {
    return val.filter((v): v is string | number => v !== null).map(String);
  }
  if (typeof val === "string") return [val];
  return [];
});

function onTreeSelectionChange(selected: string[]) {
  if (selected.length === 0) {
    emit("update:modelValue", undefined);
  } else {
    emit("update:modelValue", { operator: "equals", value: selected });
  }
}

const rangeValue = computed<[any, any]>(() => {
  if (!props.modelValue || props.modelValue.operator !== "between")
    return [null, null];
  const val = props.modelValue.value;
  if (Array.isArray(val) && val.length === 2) {
    return [val[0] ?? null, val[1] ?? null];
  }
  return [null, null];
});

function onRangeChange(val: [any, any]) {
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

let textDebounceTimer: ReturnType<typeof setTimeout> | undefined;

function onTextInput(event: Event) {
  const val = (event.target as HTMLInputElement).value;
  clearTimeout(textDebounceTimer);
  textDebounceTimer = setTimeout(() => {
    if (!val) {
      emit("update:modelValue", undefined);
    } else {
      emit("update:modelValue", { operator: "like", value: val });
    }
  }, 300);
}

const treeId = computed(() => `filter-tree-${props.column.id}`);
</script>

<template>
  <div>
    <div v-if="loading && options.length === 0" class="space-y-2 py-1">
      <div
        v-for="n in 4"
        :key="n"
        class="h-4 rounded bg-gray-200 animate-pulse"
        :style="{ width: `${60 + n * 8}%` }"
        role="status"
        aria-label="Loading options"
      />
    </div>

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
        :modelValue="rangeValue"
        @update:modelValue="onRangeChange"
      >
        <template #min="{ value, update, id: minId }">
          <Input
            :id="minId"
            :type="rangeInputType"
            :model-value="value"
            @update:model-value="update"
          />
        </template>
        <template #max="{ value, update, id: maxId }">
          <Input
            :id="maxId"
            :type="rangeInputType"
            :model-value="value"
            @update:model-value="update"
          />
        </template>
      </FilterRange>
    </template>

    <template v-else>
      <label :for="`filter-text-${column.id}`" class="sr-only">
        {{ column.label || column.id }}
      </label>
      <input
        :id="`filter-text-${column.id}`"
        type="text"
        :value="textValue"
        @input="onTextInput"
        class="w-full rounded border border-gray-300 px-2 py-1 text-sm"
        :placeholder="`Search ${column.label || column.id}...`"
        :aria-label="column.label || column.id"
      />
    </template>
  </div>
</template>

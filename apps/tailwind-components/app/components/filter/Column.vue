<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useDebounceFn } from "@vueuse/core";
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
  filterValueToTreeSelection,
  treeSelectionToFilterValue,
} from "../../utils/filterTypes";
import Tree from "../input/Tree.vue";
import FilterRange from "./Range.vue";
import InputSearch from "../input/Search.vue";
import Skeleton from "../Skeleton.vue";
import TextNoResultsMessage from "../text/NoResultsMessage.vue";

function hasNonZeroDescendant(node: CountedOption): boolean {
  if (!node.children || node.children.length === 0) return false;
  return node.children.some(
    (child) => child.count > 0 || hasNonZeroDescendant(child)
  );
}

function pruneZeros(nodes: CountedOption[]): CountedOption[] {
  return nodes
    .filter((node) => node.count > 0 || hasNonZeroDescendant(node))
    .map((node) => ({
      ...node,
      children:
        node.children && node.children.length > 0
          ? pruneZeros(node.children)
          : node.children,
    }));
}

function countAllNodes(nodes: CountedOption[]): number {
  let total = 0;
  for (const node of nodes) {
    total += 1;
    if (node.children && node.children.length > 0) {
      total += countAllNodes(node.children);
    }
  }
  return total;
}

function applyCollapseView(
  options: CountedOption[],
  { hideZero, limit }: { hideZero: boolean; limit: number | null }
): CountedOption[] {
  const afterZeroFilter = hideZero ? pruneZeros(options) : options;
  if (limit === null) return afterZeroFilter;
  if (afterZeroFilter.length <= limit) return afterZeroFilter;
  return afterZeroFilter.slice(0, limit);
}

function nodeMatchesQuery(node: CountedOption, query: string): boolean {
  const lower = query.toLowerCase();
  const label = (node.label ?? node.name).toLowerCase();
  return label.includes(lower);
}

function filterNode(node: CountedOption, query: string): CountedOption | null {
  const selfMatches = nodeMatchesQuery(node, query);
  const filteredChildren = (node.children ?? [])
    .map((child) => filterNode(child, query))
    .filter((child): child is CountedOption => child !== null);

  if (selfMatches) {
    return { ...node, children: node.children ? node.children : undefined };
  }
  if (filteredChildren.length > 0) {
    return { ...node, children: filteredChildren };
  }
  return null;
}

function filterOptionsBySearch(
  options: CountedOption[],
  query: string
): CountedOption[] {
  if (!query) return options;
  return options
    .map((node) => filterNode(node, query))
    .filter((node): node is CountedOption => node !== null);
}

const SHOW_MORE_THRESHOLD = 25;
const SHOW_MORE_STEP = 50;

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
const treeId = computed(() => `filter-tree-${props.column.id}`);

const visibleRootCount = ref(SHOW_MORE_THRESHOLD);
const localSearch = ref("");

const totalOptionCount = computed(() => countAllNodes(props.options));
const rootOptionCount = computed(() => props.options.length);

const showSearchInput = computed(
  () => isCountable.value && totalOptionCount.value > SHOW_MORE_THRESHOLD
);

const showMoreButton = computed(
  () => rootOptionCount.value > SHOW_MORE_THRESHOLD && !localSearch.value
);

const isFullyExpanded = computed(
  () => visibleRootCount.value >= rootOptionCount.value
);

const showMoreLabel = computed(() => {
  if (isFullyExpanded.value) return "Show less";
  const remaining = rootOptionCount.value - visibleRootCount.value;
  if (remaining >= SHOW_MORE_STEP) return `Show more (+${SHOW_MORE_STEP})`;
  return `Show ${remaining} more`;
});

const visibleOptions = computed<CountedOption[]>(() => {
  if (!isCountable.value) return props.options;
  if (localSearch.value) {
    return filterOptionsBySearch(props.options, localSearch.value);
  }
  return applyCollapseView(props.options, {
    hideZero: !isFullyExpanded.value,
    limit: isFullyExpanded.value ? null : visibleRootCount.value,
  });
});

function onShowMoreClick() {
  if (isFullyExpanded.value) {
    visibleRootCount.value = SHOW_MORE_THRESHOLD;
  } else {
    visibleRootCount.value = Math.min(
      visibleRootCount.value + SHOW_MORE_STEP,
      rootOptionCount.value
    );
  }
}

watch(localSearch, (newVal, oldVal) => {
  if (oldVal && !newVal) {
    visibleRootCount.value = SHOW_MORE_THRESHOLD;
  }
});

function displayCount(option: CountedOption, selected: string[]): number {
  if (selected.includes(option.name)) return option.count;
  return Math.max(0, option.count - (option.overlap ?? 0));
}

function countedOptionToTreeNode(
  option: CountedOption,
  selected: string[]
): ITreeNode {
  const count = displayCount(option, selected);
  return {
    ...option,
    label: option.label
      ? `${option.label} (${count})`
      : `${option.name} (${count})`,
    children:
      option.children?.map((child) =>
        countedOptionToTreeNode(child, selected)
      ) ?? [],
  };
}

const treeSelection = computed(() =>
  filterValueToTreeSelection(props.modelValue)
);

const treeNodes = computed<ITreeNode[]>(() =>
  visibleOptions.value.map((opt) =>
    countedOptionToTreeNode(opt, treeSelection.value)
  )
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

function textValueFromModelValue(mv: IFilterValue | undefined): string {
  if (!mv || mv.operator !== "like") return "";
  return typeof mv.value === "string" ? mv.value : "";
}

const inputText = ref<string>(textValueFromModelValue(props.modelValue));

watch(
  () => props.modelValue,
  (mv) => {
    const next = textValueFromModelValue(mv);
    if (inputText.value !== next) inputText.value = next;
  }
);

const debouncedEmitText = useDebounceFn((val: string) => {
  if (!val) {
    emit("update:modelValue", undefined);
  } else {
    emit("update:modelValue", { operator: "like", value: val });
  }
}, 500);

watch(inputText, (val) => debouncedEmitText(val));
</script>

<template>
  <div>
    <Skeleton v-if="loading && options.length === 0" :lines="4" />

    <TextNoResultsMessage
      v-else-if="isCountable && options.length === 0"
      label="No options available given current filters"
      class="!text-search-filter-group-title"
    />

    <template v-else-if="isCountable">
      <span
        v-if="saturated"
        class="block text-body-sm text-search-filter-group-title italic mb-2"
      >
        too many options, please search
      </span>

      <InputSearch
        v-if="showSearchInput"
        v-model="localSearch"
        :id="`filter-search-${column.id}`"
        :placeholder="`Search ${column.label || column.id}...`"
        :aria-label="`Search within ${column.label || column.id}`"
        size="tiny"
        class="mb-2"
      />

      <TextNoResultsMessage
        v-if="localSearch && visibleOptions.length === 0"
        label="No options available given current filters"
        class="!text-search-filter-group-title"
      />

      <Tree
        v-else
        :id="treeId"
        :nodes="treeNodes"
        :modelValue="treeSelection"
        :isMultiSelect="true"
        :disableInternalSearch="true"
        @update:modelValue="onTreeSelectionChange"
      />

      <button
        v-if="showMoreButton"
        type="button"
        class="text-body-sm text-search-filter-expand hover:underline cursor-pointer mt-1"
        @click="onShowMoreClick"
      >
        {{ showMoreLabel }}
      </button>
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
        v-model="inputText"
        :placeholder="`Search ${column.label || column.id}...`"
        :aria-label="column.label || column.id"
        size="tiny"
      />
    </template>
  </div>
</template>

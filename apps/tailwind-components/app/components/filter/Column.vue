<script setup lang="ts">
import { computed, ref, watch } from "vue";
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
import {
  applyCollapseView,
  countAllNodes,
  filterOptionsBySearch,
} from "../../utils/applyCollapseView";
import Tree from "../input/Tree.vue";
import FilterRange from "./Range.vue";
import InputSearch from "../input/Search.vue";
import Skeleton from "../Skeleton.vue";
import TextNoResultsMessage from "../text/NoResultsMessage.vue";

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
  visibleOptions.value.map(countedOptionToTreeNode)
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

    <TextNoResultsMessage
      v-else-if="isCountable && options.length === 0"
      label="No matching values for this filter"
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
        label="No matching values for this filter"
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
        :model-value="textValue"
        @update:model-value="onSearchInput"
        :placeholder="`Search ${column.label || column.id}...`"
        :aria-label="column.label || column.id"
        size="tiny"
      />
    </template>
  </div>
</template>

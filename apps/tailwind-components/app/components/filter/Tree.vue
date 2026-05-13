<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { CountedOption } from "../../utils/fetchCounts";
import type { IFilterValue } from "../../../types/filters";
import type {
  ITreeNode,
  ITreeNodeState,
  SelectionState,
} from "../../../types/types";
import {
  filterValueToTreeSelection,
  treeSelectionToFilterValue,
} from "../../utils/filterTypes";
import TreeNode from "../input/TreeNode.vue";
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

/* --- Main reactive flow --- */

const visibleRootCount = ref(SHOW_MORE_THRESHOLD);
const hasUserExpanded = ref(false);
const localSearch = ref("");

const totalOptionCount = computed(() => countAllNodes(props.options));
const rootOptionCount = computed(() => props.options.length);

const showSearchInput = computed(
  () => totalOptionCount.value > SHOW_MORE_THRESHOLD
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
  if (localSearch.value) {
    return filterOptionsBySearch(props.options, localSearch.value);
  }
  return applyCollapseView(props.options, {
    hideZero: !hasUserExpanded.value,
    limit: isFullyExpanded.value ? null : visibleRootCount.value,
  });
});

const treeId = computed(() => `filter-tree-${props.column.id}`);

const treeSelection = computed(() =>
  filterValueToTreeSelection(props.modelValue)
);

const treeNodes = computed<ITreeNode[]>(() =>
  visibleOptions.value.map((opt) =>
    countedOptionToTreeNode(opt, treeSelection.value)
  )
);

/* --- nodeMap state machine --- */

const nodeMap = ref({} as Record<string, ITreeNodeState>);

function cloneToState(node: ITreeNode): ITreeNodeState {
  const result: ITreeNodeState = {
    name: node.name,
    label: node.label,
    description: node.description,
    visible: true,
    children: [] as ITreeNodeState[],
    selected: "unselected" as SelectionState,
    expanded: false,
    selectable: true,
  };
  node.children?.forEach((child) => {
    const copy = cloneToState(child);
    copy.parent = node.name;
    nodeMap.value[child.name] = copy;
    result.children.push(copy);
  });
  return result;
}

function buildNodeMap(nodes: ITreeNode[]) {
  nodes.forEach((node) => {
    nodeMap.value[node.name] = cloneToState(node);
  });
  autoExpandSmallTree();
}

function autoExpandSmallTree() {
  const totalNodes = Object.keys(nodeMap.value).length;
  if (totalNodes <= SHOW_MORE_THRESHOLD) {
    for (const node of Object.values(nodeMap.value)) {
      if (node.children && node.children.length > 0) {
        node.expanded = true;
      }
    }
  }
}

buildNodeMap(treeNodes.value);

watch(treeNodes, (newNodes) => {
  const expandedState: Record<string, boolean> = {};
  for (const [key, node] of Object.entries(nodeMap.value)) {
    expandedState[key] = node.expanded === true;
  }
  nodeMap.value = {};
  buildNodeMap(newNodes);
  for (const [key, wasExpanded] of Object.entries(expandedState)) {
    if (nodeMap.value[key]) nodeMap.value[key].expanded = wasExpanded;
  }
  applySelectionToNodeMap(treeSelection.value);
});

watch(treeSelection, (newSelection) => {
  applySelectionToNodeMap(newSelection);
});

applySelectionToNodeMap(treeSelection.value);

function applySelectionToNodeMap(selection: string[]) {
  Object.values(nodeMap.value).forEach(
    (node) => (node.selected = "unselected")
  );
  selection.forEach((name) => {
    const node = nodeMap.value[name];
    if (node) {
      node.selected = "selected";
      propagateSelectionToRelatives(node);
    }
  });
}

function propagateSelectionToRelatives(node: ITreeNodeState) {
  getAllDescendants(node).forEach((child) => {
    child.selected = node.selected;
    child.visible = true;
  });

  getAllAncestors(node).forEach((parent) => {
    const allSelected = parent.children?.every(
      (child) => child.selected === "selected"
    );
    const allUnselected = parent.children?.every(
      (child) => child.selected === "unselected"
    );
    if (allUnselected) {
      parent.selected = "unselected";
    } else if (allSelected) {
      parent.selected = "selected";
    } else {
      parent.selected = "intermediate";
    }
  });
}

function getAllDescendants(node: ITreeNodeState): ITreeNodeState[] {
  return [node, ...(node.children || []).flatMap(getAllDescendants)];
}

function getAllAncestors(node: ITreeNodeState): ITreeNodeState[] {
  if (!node.parent) return [];
  const parentNode = nodeMap.value[node.parent];
  if (!parentNode) return [];
  return [parentNode, ...getAllAncestors(parentNode)];
}

function toggleSelect(node: ITreeNodeState) {
  const previousState = node.selected;
  if (previousState !== "selected") {
    node.selected = "selected";
  } else {
    node.selected = "unselected";
  }

  propagateSelectionToRelatives(node);
  emitSelection();
}

function toggleExpand(node: ITreeNodeState) {
  const targetNode = nodeMap.value[node.name];
  if (targetNode) {
    targetNode.expanded = targetNode.expanded !== true;
  }
}

function emitSelection() {
  const selectedNames = Object.values(nodeMap.value)
    .filter((node) => node.selected === "selected")
    .map((node) => node.name);

  emit(
    "update:modelValue",
    treeSelectionToFilterValue(selectedNames, props.column, props.options)
  );
}

const rootNodes = computed(() =>
  Object.values(nodeMap.value).filter((node) => !node.parent)
);

const virtualRootNode = computed<ITreeNodeState>(() => ({
  name: "__root__",
  label: "Root",
  visible: true,
  children: rootNodes.value,
  selected: "unselected",
  expanded: true,
  selectable: false,
}));

/* --- UI handlers --- */

function onShowMoreClick() {
  if (isFullyExpanded.value) {
    visibleRootCount.value = SHOW_MORE_THRESHOLD;
    hasUserExpanded.value = false;
  } else {
    visibleRootCount.value = Math.min(
      visibleRootCount.value + SHOW_MORE_STEP,
      rootOptionCount.value
    );
    hasUserExpanded.value = true;
  }
}

watch(localSearch, (newVal, oldVal) => {
  if (oldVal && !newVal) {
    visibleRootCount.value = SHOW_MORE_THRESHOLD;
    hasUserExpanded.value = false;
  }
});

/* --- Low-level pure helpers --- */

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
</script>

<template>
  <div>
    <Skeleton v-if="loading && options.length === 0" :lines="4" />

    <TextNoResultsMessage
      v-else-if="options.length === 0"
      label="No options available given current filters"
      class="!text-search-filter-group-title"
    />

    <template v-else>
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
        v-if="visibleOptions.length === 0"
        label="No options available given current filters"
        class="!text-search-filter-group-title"
      />

      <fieldset v-else :id="treeId">
        <legend class="sr-only">
          {{ column.label || column.id }} filter options
        </legend>
        <TreeNode
          :id="treeId"
          :parentNode="virtualRootNode"
          :isRoot="true"
          :multiselect="true"
          @toggleSelect="toggleSelect"
          @toggleExpand="toggleExpand"
        />
      </fieldset>

      <button
        v-if="showMoreButton"
        type="button"
        class="text-body-sm text-search-filter-action hover:underline cursor-pointer mt-1"
        @click="onShowMoreClick"
      >
        {{ showMoreLabel }}
      </button>
    </template>
  </div>
</template>

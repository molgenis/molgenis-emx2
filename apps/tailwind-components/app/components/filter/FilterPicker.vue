<script setup lang="ts">
import { ref, useId, watch, computed } from "vue";
import type { UseFilters } from "../../../types/filters";
import type { ITreeNodeState } from "../../../types/types";
import Modal from "../Modal.vue";
import BaseIcon from "../BaseIcon.vue";
import InputSearch from "../input/Search.vue";
import Button from "../Button.vue";
import Tree from "../input/Tree.vue";
import { computeDefaultFilters } from "../../utils/computeDefaultFilters";
import {
  isRefExpandable,
  isSelectableFilterType,
  isStringFilterType,
  isExcludedColumn,
  shouldExcludeSelfRef,
} from "../../utils/filterTreeUtils";
import {
  pruneTree,
  pruneStringNodes,
  setAllNodesVisible,
} from "../../utils/pruneFilterTree";
import type { IColumn } from "../../../../metadata-utils/src/types";

const props = defineProps<{
  filters: UseFilters;
}>();

const open = ref(false);
const searchQuery = ref("");
const showAllFilters = ref(false);
const treeId = useId();
const localSelection = ref<string[]>([]);
const isBuilding = ref(false);

const allTreeNodes = ref<ITreeNodeState[]>([]);

function buildNodes(
  cols: IColumn[],
  parentTableId: string,
  parentPath: string
): ITreeNodeState[] {
  const nodes: ITreeNodeState[] = [];
  for (const col of cols) {
    if (isExcludedColumn(col)) continue;
    if (shouldExcludeSelfRef(col, parentTableId)) continue;

    const path = parentPath ? `${parentPath}.${col.id}` : col.id;
    const label = col.label || col.id;

    if (isRefExpandable(col.columnType) && col.refTableId) {
      const childCols = props.filters.getRefColumns(path);
      const children = buildNodes(childCols, col.refTableId, path);
      nodes.push({
        name: path,
        label,
        description: col.description,
        children,
        selectable: false,
        visible: true,
        expanded: false,
      });
    } else if (
      isSelectableFilterType(col.columnType) ||
      isStringFilterType(col.columnType)
    ) {
      nodes.push({
        name: path,
        label,
        description: col.description,
        children: [],
        selectable: true,
        visible: !isStringFilterType(col.columnType),
        expanded: false,
      });
    }
  }
  return nodes;
}

async function buildTree() {
  if (isBuilding.value) return;
  isBuilding.value = true;
  try {
    const rootCols = props.filters.columns.value;
    const parentTableId = rootCols[0]?.table ?? "";

    const refRootCols = rootCols.filter(
      (c) => isRefExpandable(c.columnType) && c.refTableId
    );
    await Promise.all(
      refRootCols.map((c) => props.filters.loadRefColumns(c.id, c))
    );

    const layer2Loads: Promise<void>[] = [];
    for (const rootCol of refRootCols) {
      const childCols = props.filters.getRefColumns(rootCol.id);
      for (const c of childCols.filter(
        (c) => isRefExpandable(c.columnType) && c.refTableId
      )) {
        layer2Loads.push(
          props.filters.loadRefColumns(`${rootCol.id}.${c.id}`, c)
        );
      }
    }
    await Promise.all(layer2Loads);

    allTreeNodes.value = buildNodes(rootCols, parentTableId, "");
  } finally {
    isBuilding.value = false;
  }
}

watch(open, (isOpen) => {
  if (isOpen) {
    localSelection.value = [...props.filters.visibleFilterIds.value];
    buildTree();
  } else {
    searchQuery.value = "";
    showAllFilters.value = false;
  }
});

const displayedNodes = computed<ITreeNodeState[]>(() => {
  if (searchQuery.value.trim()) {
    return pruneTree(allTreeNodes.value, searchQuery.value);
  }

  if (showAllFilters.value) {
    return allTreeNodes.value.map(setAllNodesVisible);
  }

  return pruneStringNodes(allTreeNodes.value);
});

const hasNoResults = computed(
  () =>
    searchQuery.value.trim().length > 0 &&
    allTreeNodes.value.length > 0 &&
    displayedNodes.value.length === 0
);

function save(hide: () => void) {
  const currentIds = new Set(props.filters.visibleFilterIds.value);
  const newIds = new Set(localSelection.value);

  for (const id of currentIds) {
    if (!newIds.has(id)) {
      props.filters.toggleFilter(id);
    }
  }
  for (const id of newIds) {
    if (!currentIds.has(id)) {
      props.filters.toggleFilter(id);
    }
  }
  hide();
}

function cancel(hide: () => void) {
  hide();
}

function clearSelection() {
  localSelection.value = [];
}

function resetToDefaults() {
  localSelection.value = computeDefaultFilters(props.filters.columns.value);
}
</script>

<template>
  <button
    class="flex items-center gap-1.5 h-8 px-2 text-heading-sm text-search-filter-expand hover:underline cursor-pointer"
    @click="open = true"
    aria-haspopup="dialog"
  >
    <BaseIcon name="filter" :width="16" />
    <span>Customize</span>
  </button>

  <Modal v-model:visible="open" title="Customize filters" max-width="max-w-2xl">
    <div class="flex flex-col gap-4 p-6">
      <InputSearch
        :id="`${treeId}-search`"
        v-model="searchQuery"
        placeholder="Search columns..."
      />

      <label
        class="flex items-center gap-2 text-body-sm text-title-contrast cursor-pointer"
      >
        <input
          :id="`${treeId}-show-all`"
          type="checkbox"
          v-model="showAllFilters"
          class="rounded"
        />
        Show all column types
      </label>

      <div class="overflow-y-auto px-4">
        <Tree
          :id="treeId"
          v-model="localSelection"
          :nodes="displayedNodes"
          is-multi-select
          :show-search="false"
          :emit-selected-children="false"
        />

        <div
          v-if="hasNoResults"
          class="py-4 text-center text-body-sm text-disabled"
        >
          No columns match your search
        </div>
      </div>
    </div>

    <template #footer="{ hide }">
      <div class="py-3 flex justify-between items-center">
        <div class="flex items-center gap-2">
          <Button type="text" size="tiny" @click="clearSelection">
            Clear
          </Button>
          <Button
            type="text"
            size="tiny"
            icon="restart-alt"
            @click="resetToDefaults"
          >
            Reset to defaults
          </Button>
        </div>
        <div class="flex items-center gap-2">
          <Button type="secondary" size="small" @click="cancel(hide)">
            Cancel
          </Button>
          <Button type="primary" size="small" @click="save(hide)">
            Save
          </Button>
        </div>
      </div>
    </template>
  </Modal>
</template>

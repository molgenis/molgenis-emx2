<script setup lang="ts">
import { ref, watch, computed } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import Modal from "../Modal.vue";
import InputSearch from "../input/Search.vue";
import Button from "../Button.vue";
import InputCheckboxIcon from "../input/CheckboxIcon.vue";
import BaseIcon from "../BaseIcon.vue";
import Well from "../Well.vue";
import {
  computeDefaultFilters,
  isRefExpandable,
  isExcludedColumn,
  shouldExcludeSelfRef,
  navDepth,
} from "../../utils/filterTypes";
import fetchTableMetadata from "../../composables/fetchTableMetadata";
import type { NestedColumnMeta } from "../../../types/filters";

interface PickerNode {
  id: string;
  label: string;
  description?: string;
  selectable: boolean;
  depth: number;
  column: IColumn;
}

const props = defineProps<{
  modelValue: boolean;
  columns: IColumn[];
  visibleFilterIds: Set<string>;
  schemaId: string;
  tableId: string;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  apply: [selectedIds: Set<string>, nestedMeta: Map<string, NestedColumnMeta>];
  cancel: [];
  reset: [];
}>();

const searchQuery = ref("");
const localSelection = ref<Set<string>>(new Set());
const refColumnsCache = ref<Map<string, IColumn[]>>(new Map());
const expandedRefs = ref<Set<string>>(new Set());
const loadingRefs = ref<Set<string>>(new Set());

function resetLocalState() {
  localSelection.value = new Set(props.visibleFilterIds);
  searchQuery.value = "";
  expandedRefs.value = new Set();
}

watch(
  () => props.modelValue,
  (isOpen) => {
    if (isOpen) {
      resetLocalState();
    }
  },
  { immediate: true }
);

async function loadRefColumns(col: IColumn, path: string): Promise<void> {
  if (refColumnsCache.value.has(path)) return;
  if (!col.refTableId) return;

  const targetSchemaId = col.refSchemaId || props.schemaId;
  const targetTableId = col.refTableId;

  loadingRefs.value = new Set([...loadingRefs.value, path]);
  try {
    const meta = await fetchTableMetadata(targetSchemaId, targetTableId);
    const cols = meta.columns ?? [];

    const newCache = new Map(refColumnsCache.value);
    newCache.set(path, cols);
    refColumnsCache.value = newCache;
  } catch (e) {
    console.error("Failed to load ref columns:", e);
    const newCache = new Map(refColumnsCache.value);
    newCache.set(path, []);
    refColumnsCache.value = newCache;
  } finally {
    const newLoading = new Set(loadingRefs.value);
    newLoading.delete(path);
    loadingRefs.value = newLoading;
  }
}

function toggleExpand(node: PickerNode) {
  const newExpanded = new Set(expandedRefs.value);
  if (newExpanded.has(node.id)) {
    newExpanded.delete(node.id);
  } else {
    newExpanded.add(node.id);
    loadRefColumns(node.column, node.id);
  }
  expandedRefs.value = newExpanded;
}

function buildRefPickerNode(
  col: IColumn,
  path: string,
  depth: number
): PickerNode {
  return {
    id: path,
    label: col.label || col.id,
    description: col.description,
    selectable: false,
    depth,
    column: col,
  };
}

function buildNodes(
  cols: IColumn[],
  parentTableId: string,
  parentPath: string,
  depth: number
): PickerNode[] {
  return cols
    .filter(
      (col) =>
        !isExcludedColumn(col) && !shouldExcludeSelfRef(col, parentTableId)
    )
    .flatMap((col) => {
      const path = parentPath ? `${parentPath}.${col.id}` : col.id;

      if (isRefExpandable(col.columnType) && col.refTableId) {
        const maxDepth = navDepth(col.columnType);
        if (depth >= maxDepth) return [];

        const refNode = buildRefPickerNode(col, path, depth);
        const childNodes = expandedRefs.value.has(path)
          ? buildNodes(
              refColumnsCache.value.get(path) ?? [],
              col.refTableId,
              path,
              depth + 1
            )
          : [];

        return [refNode, ...childNodes];
      }

      return [
        {
          id: path,
          label: col.label || col.id,
          description: col.description,
          selectable: true,
          depth,
          column: col,
        },
      ];
    });
}

const parentTableId = computed(() => props.columns[0]?.table ?? "");

const allNodes = computed<PickerNode[]>(() => {
  return buildNodes(props.columns, parentTableId.value, "", 0);
});

function matchesSearchQuery(node: PickerNode, query: string): boolean {
  return (
    node.label.toLowerCase().includes(query) ||
    (node.description ?? "").toLowerCase().includes(query)
  );
}

function isInExpandedParent(node: PickerNode): boolean {
  const parentPath = node.id.substring(0, node.id.lastIndexOf("."));
  return Boolean(parentPath && expandedRefs.value.has(parentPath));
}

const displayedNodes = computed<PickerNode[]>(() => {
  const query = searchQuery.value.trim().toLowerCase();
  if (query) {
    const matchingIds = new Set(
      allNodes.value
        .filter((node) => matchesSearchQuery(node, query))
        .map((node) => node.id)
    );
    return allNodes.value.filter(
      (node) => matchingIds.has(node.id) || isInExpandedParent(node)
    );
  }
  return allNodes.value.filter(
    (node) => !node.id.split(".")[0]!.startsWith("mg_")
  );
});

function toggleSelection(id: string) {
  const next = new Set(localSelection.value);
  if (next.has(id)) {
    next.delete(id);
  } else {
    next.add(id);
  }
  localSelection.value = next;
}

function selectAll() {
  const selectableIds = allNodes.value
    .filter((node) => node.selectable)
    .map((node) => node.id);
  localSelection.value = new Set(selectableIds);
}

function clearSelection() {
  localSelection.value = new Set();
}

function buildNestedLabelForId(
  id: string,
  nodeById: Map<string, PickerNode>
): string {
  const segments = id.split(".");
  const labelParts = segments.map((_, idx) => {
    const pathUpTo = segments.slice(0, idx + 1).join(".");
    return nodeById.get(pathUpTo)?.label ?? segments[idx]!;
  });
  return labelParts.join(" → ");
}

function buildNestedMeta(): Map<string, NestedColumnMeta> {
  const nodeById = new Map(allNodes.value.map((node) => [node.id, node]));
  const nestedIds = [...localSelection.value].filter((id) => id.includes("."));
  const entries = nestedIds
    .map((id) => {
      const node = nodeById.get(id);
      if (!node) return null;
      const entry: [string, NestedColumnMeta] = [
        id,
        {
          label: buildNestedLabelForId(id, nodeById),
          columnType: node.column.columnType,
          refTableId: node.column.refTableId,
          refSchemaId: node.column.refSchemaId ?? null,
        },
      ];
      return entry;
    })
    .filter((entry): entry is [string, NestedColumnMeta] => entry !== null);
  return new Map(entries);
}

function applyAndClose() {
  emit("apply", new Set(localSelection.value), buildNestedMeta());
  emit("update:modelValue", false);
}

function cancelAndClose() {
  emit("cancel");
  emit("update:modelValue", false);
}

function resetToDefaults() {
  localSelection.value = new Set(computeDefaultFilters(props.columns));
  emit("reset");
  emit("update:modelValue", false);
}

function updateVisibility(value: boolean) {
  emit("update:modelValue", value);
}
</script>

<template>
  <Modal
    :visible="modelValue"
    @update:visible="updateVisibility"
    title="Customize filters"
    max-width="w-full max-w-full sm:w-[90vw] sm:max-w-[90vw]"
  >
    <div class="flex flex-col gap-4 p-6 min-h-0 flex-1">
      <InputSearch
        id="filter-picker-search"
        v-model="searchQuery"
        placeholder="Search columns..."
      />

      <div class="overflow-y-auto flex-1">
        <ul role="list" class="flex flex-col gap-1">
          <li
            v-for="node in displayedNodes"
            :key="node.id"
            :style="{ paddingLeft: `${node.depth * 1.5}rem` }"
          >
            <div
              v-if="!node.selectable"
              class="flex items-center justify-between gap-2 py-1.5 px-2 text-body-sm text-title-contrast"
            >
              <button
                type="button"
                class="flex items-center gap-1.5 text-left cursor-pointer hover:underline flex-1 min-w-0"
                :aria-expanded="expandedRefs.has(node.id)"
                @click="toggleExpand(node)"
              >
                <BaseIcon
                  name="caret-right"
                  :width="20"
                  class="shrink-0 min-w-[20px] transition-transform"
                  :class="{ 'rotate-90': expandedRefs.has(node.id) }"
                />
                <span class="font-medium">{{ node.label }}</span>
                <span
                  v-if="!loadingRefs.has(node.id) && node.column.refTableId"
                  class="text-xs text-disabled"
                  >&rarr; {{ node.column.refTableId }}</span
                >
                <span
                  v-if="loadingRefs.has(node.id)"
                  class="text-xs text-disabled"
                  >Loading...</span
                >
              </button>
              <Well class="shrink-0 text-xs">{{ node.column.columnType }}</Well>
            </div>

            <label
              v-else
              class="flex items-center justify-between gap-2 w-full py-1.5 px-2 text-body-sm text-title-contrast cursor-pointer hover:bg-hover rounded-input"
            >
              <input
                type="checkbox"
                :checked="localSelection.has(node.id)"
                :aria-label="node.label"
                class="sr-only"
                @change="toggleSelection(node.id)"
              />
              <div class="flex items-center gap-2 flex-1 min-w-0">
                <InputCheckboxIcon
                  :checked="localSelection.has(node.id)"
                  class="shrink-0"
                />
                <div class="flex-1 min-w-0 text-left">
                  <span class="font-medium text-body-sm text-title-contrast">{{
                    node.label
                  }}</span>
                  <span
                    v-if="node.description"
                    class="text-xs text-disabled truncate block"
                    :title="node.description"
                  >
                    {{ node.description }}
                  </span>
                </div>
              </div>
              <Well class="shrink-0 text-xs">{{ node.column.columnType }}</Well>
            </label>
          </li>
        </ul>

        <div
          v-if="displayedNodes.length === 0"
          class="py-4 text-center text-body-sm text-disabled"
        >
          No columns match your search
        </div>
      </div>
    </div>

    <template #footer>
      <div class="py-3 flex justify-between items-center">
        <div class="flex items-center gap-2">
          <Button type="text" size="tiny" icon="checklist" @click="selectAll">
            Select all
          </Button>
          <Button type="text" size="tiny" icon="trash" @click="clearSelection">
            Clear
          </Button>
          <Button
            type="text"
            size="tiny"
            icon="restart-alt"
            @click="resetToDefaults"
          >
            Reset
          </Button>
        </div>
        <div class="flex items-center gap-2">
          <Button type="secondary" size="small" @click="cancelAndClose">
            Cancel
          </Button>
          <Button type="primary" size="small" @click="applyAndClose">
            Apply
          </Button>
        </div>
      </div>
    </template>
  </Modal>
</template>

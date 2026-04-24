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

interface PickerNode {
  id: string;
  label: string;
  description?: string | null;
  selectable: boolean;
  depth: number;
  column: IColumn;
}

interface NestedColumnMeta {
  label: string;
  columnType: string;
  refTableId?: string | null;
  refSchemaId?: string | null;
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

        const refNode: PickerNode = {
          id: path,
          label: col.label || col.id,
          description: col.description,
          selectable: false,
          depth,
          column: col,
        };

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
        } satisfies PickerNode,
      ];
    });
}

const parentTableId = computed(() => props.columns[0]?.table ?? "");

const allNodes = computed<PickerNode[]>(() => {
  return buildNodes(props.columns, parentTableId.value, "", 0);
});

const isMgCol = (id: string) => id.startsWith("mg_");

const displayedNodes = computed<PickerNode[]>(() => {
  const query = searchQuery.value.trim().toLowerCase();
  const expandedSet = expandedRefs.value;
  if (query) {
    const matchingIds = new Set<string>();
    for (const node of allNodes.value) {
      const labelMatch = node.label.toLowerCase().includes(query);
      const descMatch = (node.description ?? "").toLowerCase().includes(query);
      if (labelMatch || descMatch) matchingIds.add(node.id);
    }
    return allNodes.value.filter((node) => {
      if (matchingIds.has(node.id)) return true;
      const parentPath = node.id.substring(0, node.id.lastIndexOf("."));
      return parentPath && expandedSet.has(parentPath);
    });
  }
  return allNodes.value.filter(
    (node) => !isMgCol(node.id.split(".")[0] ?? node.id)
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

function buildNestedMeta(): Map<string, NestedColumnMeta> {
  const nodeById = new Map(allNodes.value.map((n) => [n.id, n]));
  const meta = new Map<string, NestedColumnMeta>();
  for (const id of localSelection.value) {
    if (!id.includes(".")) continue;
    const node = nodeById.get(id);
    if (!node) continue;
    const segments = id.split(".");
    const labelParts = segments.map((_, idx) => {
      const pathUpTo = segments.slice(0, idx + 1).join(".");
      return nodeById.get(pathUpTo)?.label ?? segments[idx]!;
    });
    meta.set(id, {
      label: labelParts.join(" → "),
      columnType: node.column.columnType,
      refTableId: node.column.refTableId,
      refSchemaId: node.column.refSchemaId ?? null,
    });
  }
  return meta;
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
}

function clearSelection() {
  localSelection.value = new Set();
}

function updateVisibility(value: boolean) {
  emit("update:modelValue", value);
}

function selectAll() {
  const selectableIds = allNodes.value
    .filter((node) => node.selectable)
    .map((node) => node.id);
  localSelection.value = new Set(selectableIds);
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

<script setup lang="ts">
import { ref, watch, computed, onMounted } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import Modal from "../Modal.vue";
import InputSearch from "../input/Search.vue";
import Button from "../Button.vue";
import InputCheckboxIcon from "../input/CheckboxIcon.vue";
import BaseIcon from "../BaseIcon.vue";
import { computeDefaultFilters } from "../../utils/computeDefaultFilters";
import {
  isRefExpandable,
  isExcludedColumn,
  shouldExcludeSelfRef,
  navDepth,
} from "../../utils/filterTreeUtils";

interface PickerNode {
  id: string;
  label: string;
  description?: string | null;
  selectable: boolean;
  depth: number;
  refTableId?: string | null;
  refSchemaId?: string | null;
  columnType: string;
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
  apply: [
    selectedIds: Set<string>,
    nestedMeta: Map<
      string,
      {
        label: string;
        columnType: string;
        refTableId?: string | null;
        refSchemaId?: string | null;
      }
    >
  ];
  cancel: [];
}>();

const searchQuery = ref("");
const localSelection = ref<Set<string>>(new Set());
const refColumnsCache = ref<Map<string, IColumn[]>>(new Map());
const expandedRefs = ref<Set<string>>(new Set());
const loadingRefs = ref<Set<string>>(new Set());

function syncFromProps() {
  localSelection.value = new Set(props.visibleFilterIds);
  searchQuery.value = "";
  expandedRefs.value = new Set();
}

onMounted(() => {
  if (props.modelValue) {
    syncFromProps();
  }
});

watch(
  () => props.modelValue,
  (isOpen) => {
    if (isOpen) {
      syncFromProps();
    }
  }
);

async function loadRefColumns(col: IColumn, path: string): Promise<void> {
  if (refColumnsCache.value.has(path)) return;
  if (!col.refTableId) return;

  const targetSchemaId = col.refSchemaId || props.schemaId;
  const targetTableId = col.refTableId;

  loadingRefs.value = new Set([...loadingRefs.value, path]);
  try {
    const response = await $fetch<{
      data: {
        _schema: { tables: Array<{ name: string; columns: IColumn[] }> };
      };
    }>(`/${encodeURIComponent(targetSchemaId)}/graphql`, {
      method: "POST",
      body: {
        query: `{ _schema { tables { name columns { id name columnType description refTableId refSchemaId refLinkId } } } }`,
      },
    });

    const tables = response?.data?._schema?.tables ?? [];
    const table = tables.find(
      (t: { name: string }) => t.name === targetTableId
    );
    const cols = table?.columns ?? [];

    const newCache = new Map(refColumnsCache.value);
    newCache.set(path, cols);
    refColumnsCache.value = newCache;
  } catch {
    const newCache = new Map(refColumnsCache.value);
    newCache.set(path, []);
    refColumnsCache.value = newCache;
  } finally {
    const newLoading = new Set(loadingRefs.value);
    newLoading.delete(path);
    loadingRefs.value = newLoading;
  }
}

function toggleExpand(node: PickerNode, col: IColumn) {
  const newExpanded = new Set(expandedRefs.value);
  if (newExpanded.has(node.id)) {
    newExpanded.delete(node.id);
  } else {
    newExpanded.add(node.id);
    loadRefColumns(col, node.id);
  }
  expandedRefs.value = newExpanded;
}

function buildNodes(
  cols: IColumn[],
  parentTableId: string,
  parentPath: string,
  depth: number
): PickerNode[] {
  const nodes: PickerNode[] = [];
  for (const col of cols) {
    if (isExcludedColumn(col)) continue;
    if (shouldExcludeSelfRef(col, parentTableId)) continue;

    const path = parentPath ? `${parentPath}.${col.id}` : col.id;

    if (isRefExpandable(col.columnType) && col.refTableId) {
      const maxDepth = navDepth(col.columnType);
      if (depth < maxDepth) {
        nodes.push({
          id: path,
          label: col.label || col.id,
          description: col.description,
          selectable: false,
          depth,
          refTableId: col.refTableId,
          refSchemaId: col.refSchemaId ?? null,
          columnType: col.columnType,
        });

        if (expandedRefs.value.has(path)) {
          const childCols = refColumnsCache.value.get(path) ?? [];
          const childNodes = buildNodes(
            childCols,
            col.refTableId,
            path,
            depth + 1
          );
          nodes.push(...childNodes);
        }
      }
    } else {
      nodes.push({
        id: path,
        label: col.label || col.id,
        description: col.description,
        selectable: true,
        depth,
        refTableId: col.refTableId ?? null,
        refSchemaId: col.refSchemaId ?? null,
        columnType: col.columnType,
      });
    }
  }
  return nodes;
}

const rootColsMap = computed<Map<string, IColumn>>(() => {
  const map = new Map<string, IColumn>();
  for (const col of props.columns) {
    if (isRefExpandable(col.columnType) && col.refTableId) {
      map.set(col.id, col);
    }
  }
  return map;
});

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

function buildNestedMeta(): Map<
  string,
  {
    label: string;
    columnType: string;
    refTableId?: string | null;
    refSchemaId?: string | null;
  }
> {
  const nodeById = new Map(allNodes.value.map((n) => [n.id, n]));
  const meta = new Map<
    string,
    {
      label: string;
      columnType: string;
      refTableId?: string | null;
      refSchemaId?: string | null;
    }
  >();
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
      columnType: node.columnType,
      refTableId: node.refTableId,
      refSchemaId: node.refSchemaId,
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

function selectAll() {
  const selectableIds = allNodes.value
    .filter((node) => node.selectable)
    .map((node) => node.id);
  localSelection.value = new Set(selectableIds);
}

function getRefColForNode(node: PickerNode): IColumn | undefined {
  const topId = node.id.split(".")[0];
  if (!topId) return undefined;

  if (node.depth === 0) {
    return rootColsMap.value.get(topId);
  }

  const segments = node.id.split(".");
  let currentCols: IColumn[] = props.columns;
  for (let i = 0; i < segments.length - 1; i++) {
    const seg = segments[i];
    const found = currentCols.find((c) => c.id === seg);
    if (!found) return undefined;
    const parentPath = segments.slice(0, i + 1).join(".");
    currentCols = refColumnsCache.value.get(parentPath) ?? [];
  }
  const lastSeg = segments[segments.length - 1];
  return currentCols.find((c) => c.id === lastSeg);
}
</script>

<template>
  <Modal
    :visible="modelValue"
    @update:visible="emit('update:modelValue', $event)"
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
                @click="toggleExpand(node, getRefColForNode(node)!)"
              >
                <BaseIcon
                  name="caret-right"
                  :width="20"
                  class="shrink-0 min-w-[20px] transition-transform"
                  :class="{ 'rotate-90': expandedRefs.has(node.id) }"
                />
                <span class="font-medium">{{ node.label }}</span>
                <span
                  v-if="!loadingRefs.has(node.id) && node.refTableId"
                  class="text-xs text-disabled"
                  >&rarr; {{ node.refTableId }}</span
                >
                <span
                  v-if="loadingRefs.has(node.id)"
                  class="text-xs text-disabled"
                  >Loading...</span
                >
              </button>
              <span
                class="text-xs text-disabled bg-gray-100 px-1.5 py-0.5 rounded shrink-0"
                >{{ node.columnType }}</span
              >
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
              <span
                class="text-xs text-disabled bg-gray-100 px-1.5 py-0.5 rounded shrink-0"
                >{{ node.columnType }}</span
              >
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
          <Button type="text" size="tiny" icon="check-box" @click="selectAll">
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

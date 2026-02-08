<script setup lang="ts">
import { computed, ref, useId, watch } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import Button from "../Button.vue";
import BaseIcon from "../BaseIcon.vue";
import InputSearch from "../input/Search.vue";
import InputCheckboxIcon from "../input/CheckboxIcon.vue";
import fetchTableMetadata from "../../composables/fetchTableMetadata";
import {
  MAX_NESTING_DEPTH,
  REF_EXPANDABLE_TYPES,
} from "../../utils/filterConstants";

function columnTooltip(col: IColumn): string {
  const parts = [`${col.label} (${col.id})`, col.columnType];
  if (col.refTableId) parts.push(`→ ${col.refTableId}`);
  if (col.description) parts.push(col.description);
  return parts.join("\n");
}

function headingTooltip(heading: string): string {
  const headingCol = props.columns.find(
    (c) =>
      (c.columnType === "HEADING" || c.columnType === "SECTION") &&
      (c.label === heading || c.id === heading)
  );
  if (!headingCol?.description) return heading;
  return `${heading}\n${headingCol.description}`;
}

const props = defineProps<{
  columns: IColumn[];
  visibleFilterIds: string[];
  defaultFilterIds: string[];
  schemaId: string;
}>();

const emit = defineEmits<{
  toggle: [columnId: string];
  reset: [];
}>();

const searchQuery = ref("");
const searchInputId = useId();
const expandedGroups = ref<Set<string>>(new Set());
const expandedPaths = ref<Set<string>>(new Set());
const refColumnsCache = ref<Map<string, IColumn[]>>(new Map());
const refLoadingKeys = ref<Set<string>>(new Set());

const TYPE_ICONS: Record<string, string> = {
  ONTOLOGY: "database",
  ONTOLOGY_ARRAY: "database",
  REF: "hub",
  REF_ARRAY: "hub",
  SELECT: "hub",
  RADIO: "hub",
  CHECKBOX: "hub",
  MULTISELECT: "hub",
  REFBACK: "hub",
  INT: "percent",
  DECIMAL: "percent",
  LONG: "percent",
  DATE: "schedule",
  DATETIME: "schedule",
  STRING: "format-align-left",
  TEXT: "format-align-left",
  EMAIL: "format-align-left",
  BOOL: "check",
};

const TYPE_PRIORITY: Record<string, number> = {
  ONTOLOGY: 1,
  ONTOLOGY_ARRAY: 1,
  REF: 2,
  REF_ARRAY: 2,
  SELECT: 2,
  RADIO: 2,
  CHECKBOX: 2,
  MULTISELECT: 2,
  REFBACK: 2,
  INT: 3,
  DECIMAL: 3,
  LONG: 3,
  DATE: 3,
  DATETIME: 3,
  STRING: 4,
  TEXT: 4,
  EMAIL: 4,
  BOOL: 5,
};

const EXCLUDED_TYPES = ["HEADING", "SECTION"];

const filteredColumns = computed(() => {
  return props.columns.filter(
    (col) =>
      !EXCLUDED_TYPES.includes(col.columnType) && !col.id.startsWith("mg_")
  );
});

const searchedColumns = computed(() => {
  if (!searchQuery.value) return filteredColumns.value;
  const searchLower = searchQuery.value.toLowerCase();
  return filteredColumns.value.filter((col) =>
    col.label.toLowerCase().includes(searchLower)
  );
});

const groupedColumns = computed(() => {
  const groups = new Map<string, IColumn[]>();

  for (const col of searchedColumns.value) {
    const groupKey = col.heading || "";
    if (!groups.has(groupKey)) {
      groups.set(groupKey, []);
    }
    groups.get(groupKey)!.push(col);
  }

  const result = Array.from(groups.entries()).map(([heading, columns]) => ({
    heading,
    columns: [...columns].sort((a, b) => {
      const priorityA = TYPE_PRIORITY[a.columnType] || 999;
      const priorityB = TYPE_PRIORITY[b.columnType] || 999;
      return priorityA - priorityB;
    }),
  }));

  return result.sort((a, b) => {
    if (!a.heading && b.heading) return 1;
    if (a.heading && !b.heading) return -1;
    return 0;
  });
});

watch(searchQuery, (newValue) => {
  if (newValue) {
    const allHeadings = groupedColumns.value
      .map((g) => g.heading)
      .filter((h) => h);
    expandedGroups.value = new Set(allHeadings);
  }
});

watch(
  groupedColumns,
  (newGroups) => {
    const allHeadings = newGroups.map((g) => g.heading).filter((h) => h);
    expandedGroups.value = new Set(allHeadings);
  },
  { immediate: true }
);

function getTypeIcon(column: IColumn): string {
  return TYPE_ICONS[column.columnType] || "filter-alt";
}

function isRefType(col: IColumn): boolean {
  return REF_EXPANDABLE_TYPES.includes(col.columnType);
}

function isVisible(columnId: string): boolean {
  return props.visibleFilterIds.includes(columnId);
}

function handleToggle(columnId: string) {
  emit("toggle", columnId);
}

function handleReset() {
  emit("reset");
}

function toggleGroup(heading: string) {
  const newSet = new Set(expandedGroups.value);
  if (newSet.has(heading)) {
    newSet.delete(heading);
  } else {
    newSet.add(heading);
  }
  expandedGroups.value = newSet;
}

function toggleRefExpand(parentPath: string, column: IColumn) {
  const newSet = new Set(expandedPaths.value);
  if (newSet.has(parentPath)) {
    newSet.delete(parentPath);
    for (const path of newSet) {
      if (path.startsWith(parentPath + ".")) {
        newSet.delete(path);
      }
    }
  } else {
    newSet.add(parentPath);
    loadRefColumns(parentPath, column);
  }
  expandedPaths.value = newSet;
}

async function loadRefColumns(parentPath: string, column: IColumn) {
  if (
    refColumnsCache.value.has(parentPath) ||
    refLoadingKeys.value.has(parentPath)
  )
    return;
  if (!column.refTableId) return;

  refLoadingKeys.value.add(parentPath);
  const schemaId = column.refSchemaId || props.schemaId;
  try {
    const meta = await fetchTableMetadata(schemaId, column.refTableId);
    const unfilterable = ["HEADING", "SECTION", "REFBACK"];
    refColumnsCache.value.set(
      parentPath,
      meta.columns.filter(
        (c) => !c.id.startsWith("mg_") && !unfilterable.includes(c.columnType)
      )
    );
  } catch {
  } finally {
    refLoadingKeys.value.delete(parentPath);
  }
}

interface FlatPickerRow {
  path: string;
  column: IColumn;
  depth: number;
  isRefExpandable: boolean;
}

function flattenGroup(columns: IColumn[]): FlatPickerRow[] {
  const rows: FlatPickerRow[] = [];

  function walk(
    cols: IColumn[],
    parentPath: string,
    depth: number,
    parentSchemaId: string
  ) {
    for (const col of cols) {
      const path = parentPath ? `${parentPath}.${col.id}` : col.id;
      const isRef =
        REF_EXPANDABLE_TYPES.includes(col.columnType) && !!col.refTableId;
      rows.push({
        path,
        column: col,
        depth,
        isRefExpandable: isRef && depth < MAX_NESTING_DEPTH,
      });
      if (isRef && expandedPaths.value.has(path) && depth < MAX_NESTING_DEPTH) {
        const children = refColumnsCache.value.get(path);
        if (children) {
          walk(children, path, depth + 1, col.refSchemaId || parentSchemaId);
        }
      }
    }
  }

  walk(columns, "", 0, props.schemaId);
  return rows;
}
</script>

<template>
  <VDropdown placement="bottom-start" :distance="4">
    <Button type="text" size="tiny" icon="plus">Add filter</Button>
    <template #popper="{ hide }">
      <div
        class="bg-modal border border-black/10 rounded-lg shadow-lg w-96"
        @keydown.escape="hide"
      >
        <div class="p-3 border-b border-black/10">
          <InputSearch
            :id="searchInputId"
            v-model="searchQuery"
            placeholder="Search filters..."
            size="tiny"
          />
        </div>

        <div class="max-h-80 overflow-y-auto">
          <div v-for="group in groupedColumns" :key="group.heading">
            <button
              v-if="group.heading"
              @click="toggleGroup(group.heading)"
              v-tooltip.right="headingTooltip(group.heading)"
              class="w-full px-4 py-2 flex items-center gap-2 bg-modal-footer hover:bg-tab-hover transition-colors text-left"
            >
              <span
                class="rounded-full h-6 w-6 flex items-center justify-center shrink-0 text-button-tree-node-toggle hover:bg-button-tree-node-toggle hover:text-button-tree-node-toggle-hover"
              >
                <BaseIcon
                  :name="
                    expandedGroups.has(group.heading)
                      ? 'caret-down'
                      : 'caret-right'
                  "
                  :width="20"
                />
              </span>
              <span
                class="flex-1 uppercase font-display text-heading-xs text-title"
                >{{ group.heading }}</span
              >
              <span class="text-body-xs opacity-40">{{
                group.columns.length
              }}</span>
            </button>
            <template
              v-if="!group.heading || expandedGroups.has(group.heading)"
            >
              <div v-for="row in flattenGroup(group.columns)" :key="row.path">
                <button
                  @click="handleToggle(row.path)"
                  v-tooltip.right="columnTooltip(row.column)"
                  class="w-full py-1.5 flex items-center gap-2 hover:bg-tab-hover text-left transition-colors"
                  :style="{
                    paddingLeft: `${16 + row.depth * 24}px`,
                    paddingRight: '16px',
                  }"
                >
                  <InputCheckboxIcon
                    :checked="isVisible(row.path)"
                    class="min-w-[20px] shrink-0"
                  />
                  <BaseIcon
                    :name="getTypeIcon(row.column)"
                    class="w-4 h-4 opacity-40 shrink-0"
                  />
                  <span
                    class="text-body-sm leading-normal flex-1 truncate text-title"
                  >
                    {{ row.column.label }}
                  </span>
                  <span
                    v-if="row.isRefExpandable"
                    @click.stop="toggleRefExpand(row.path, row.column)"
                    class="rounded-full h-5 w-5 flex items-center justify-center shrink-0 text-button-tree-node-toggle hover:bg-button-tree-node-toggle hover:text-button-tree-node-toggle-hover"
                  >
                    <BaseIcon
                      :name="
                        expandedPaths.has(row.path) ? 'caret-up' : 'caret-down'
                      "
                      :width="16"
                    />
                  </span>
                </button>
              </div>
            </template>
          </div>
        </div>

        <div class="border-t border-black/10 p-2">
          <Button
            type="text"
            size="tiny"
            icon="restart-alt"
            @click="handleReset"
            class="w-full justify-start"
            >Reset to defaults</Button
          >
        </div>
      </div>
    </template>
  </VDropdown>
</template>

<script setup lang="ts">
import { computed, ref, useId } from "vue";
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
const expandedPaths = ref<Set<string>>(new Set());
const refColumnsCache = ref<Map<string, IColumn[]>>(new Map());
const refLoadingKeys = ref<Set<string>>(new Set());

const EXCLUDED_TYPES = ["HEADING", "SECTION"];

const TYPE_LABELS: Record<string, string> = {
  ONTOLOGY: "ontology",
  ONTOLOGY_ARRAY: "ontology",
  REF: "ref",
  REF_ARRAY: "ref",
  REFBACK: "ref",
  SELECT: "ref",
  MULTISELECT: "ref",
  RADIO: "ref",
  CHECKBOX: "ref",
  INT: "number",
  DECIMAL: "number",
  LONG: "number",
  NON_NEGATIVE_INT: "number",
  DATE: "date",
  DATETIME: "date",
  STRING: "text",
  TEXT: "text",
  EMAIL: "text",
  HYPERLINK: "text",
  AUTO_ID: "text",
  UUID: "text",
  JSON: "text",
  STRING_ARRAY: "text",
  TEXT_ARRAY: "text",
  EMAIL_ARRAY: "text",
  HYPERLINK_ARRAY: "text",
  UUID_ARRAY: "text",
  BOOL: "bool",
  BOOL_ARRAY: "bool",
  FILE: "text",
};

const TYPE_ORDER = ["ontology", "ref", "text", "number", "date", "bool"];

function typeLabel(col: IColumn): string {
  return TYPE_LABELS[col.columnType] || col.columnType.toLowerCase();
}

const groupedByType = computed(() => {
  let cols = props.columns.filter(
    (col) =>
      !EXCLUDED_TYPES.includes(col.columnType) && !col.id.startsWith("mg_")
  );
  if (searchQuery.value) {
    const searchLower = searchQuery.value.toLowerCase();
    cols = cols.filter(
      (col) =>
        col.label.toLowerCase().includes(searchLower) ||
        typeLabel(col).includes(searchLower)
    );
  }

  const groups = new Map<string, IColumn[]>();
  for (const col of cols) {
    const group = typeLabel(col);
    if (!groups.has(group)) groups.set(group, []);
    groups.get(group)!.push(col);
  }

  return TYPE_ORDER
    .filter((type) => groups.has(type))
    .map((type) => ({
      type,
      columns: groups.get(type)!.sort((a, b) =>
        a.label.localeCompare(b.label, undefined, { sensitivity: "base" })
      ),
    }))
    .concat(
      Array.from(groups.entries())
        .filter(([type]) => !TYPE_ORDER.includes(type))
        .map(([type, columns]) => ({
          type,
          columns: columns.sort((a, b) =>
            a.label.localeCompare(b.label, undefined, { sensitivity: "base" })
          ),
        }))
    );
});

function isVisible(columnId: string): boolean {
  return props.visibleFilterIds.includes(columnId);
}

function handleToggle(columnId: string) {
  emit("toggle", columnId);
}

function handleReset() {
  emit("reset");
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

function flattenColumns(columns: IColumn[]): FlatPickerRow[] {
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
          <div v-for="group in groupedByType" :key="group.type">
            <div
              class="px-4 py-1.5 text-body-xs uppercase tracking-wide opacity-40 bg-modal-footer"
            >
              {{ group.type }}
            </div>
            <div
              v-for="row in flattenColumns(group.columns)"
              :key="row.path"
            >
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

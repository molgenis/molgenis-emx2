<script lang="ts">
export const MG_COLLAPSED_PARAM = "mg_collapsed";
</script>

<script setup lang="ts">
import { ref, computed, onMounted, useId } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { UseFilters } from "../../../types/filters";
import BaseIcon from "../BaseIcon.vue";
import InputSearch from "../input/Search.vue";
import Column from "./Column.vue";
import Picker from "./Picker.vue";
import fetchTableMetadata from "../../composables/fetchTableMetadata";
type RouteQuery = Record<
  string,
  string | string[] | (string | null)[] | null | undefined
>;

const props = defineProps<{
  filters: UseFilters;
  columns: IColumn[];
  schemaId: string;
  tableId: string;
  defaultCollapsed?: string[];
  route?: { query: RouteQuery };
  router?: { replace: (opts: Record<string, unknown>) => void };
}>();

const route = props.route ?? null;
const router = props.router ?? null;

const searchInputId = useId();
const collapsed = ref(new Set<string>());
const pickerOpen = ref(false);

async function fetchTableColumns(
  schemaId: string,
  tableId: string
): Promise<IColumn[]> {
  try {
    const meta = await fetchTableMetadata(schemaId, tableId);
    return meta.columns ?? [];
  } catch {
    return [];
  }
}

async function hydrateNestedFiltersFromUrl() {
  const visibleIds = props.filters.visibleFilterIds.value;
  const alreadyKnown = props.filters.nestedColumnMeta.value;

  const dottedIds = visibleIds.filter(
    (id) => id.includes(".") && !alreadyKnown.has(id)
  );
  if (dottedIds.length === 0) return;

  for (const id of dottedIds) {
    const segments = id.split(".");
    let currentCols: IColumn[] = props.columns;
    let currentSchemaId = props.schemaId;
    const labelParts: string[] = [];
    let resolved = true;

    for (let i = 0; i < segments.length; i++) {
      const seg = segments[i]!;
      const col = currentCols.find((c) => c.id === seg);
      if (!col) {
        resolved = false;
        break;
      }
      labelParts.push(col.label || col.id);

      if (i < segments.length - 1) {
        if (!col.refTableId) {
          resolved = false;
          break;
        }
        const nextSchemaId = col.refSchemaId || currentSchemaId;
        currentCols = await fetchTableColumns(nextSchemaId, col.refTableId);
        currentSchemaId = nextSchemaId;
      } else if (resolved) {
        props.filters.registerNestedColumn(id, {
          label: labelParts.join(" → "),
          columnType: col.columnType,
          refTableId: col.refTableId ?? null,
          refSchemaId: col.refSchemaId ?? null,
        });
      }
    }
  }
}

function applyDefaultCollapse() {
  if (props.defaultCollapsed) {
    collapsed.value = new Set(props.defaultCollapsed);
    return;
  }
  const visibleIds = [...props.filters.visibleFilterIds.value];
  const next = new Set<string>();
  visibleIds.forEach((id, index) => {
    if (index >= 5 && !props.filters.filterStates.value.has(id)) {
      next.add(id);
    }
  });
  collapsed.value = next;
}

function persistCollapsed(next: Set<string>) {
  if (!route || !router) return;
  const currentQuery = { ...(route.query as Record<string, unknown>) };
  if (next.size === 0) {
    delete currentQuery[MG_COLLAPSED_PARAM];
  } else {
    currentQuery[MG_COLLAPSED_PARAM] = [...next].join(",");
  }
  router.replace({ query: currentQuery });
}

onMounted(async () => {
  const urlParam = route?.query[MG_COLLAPSED_PARAM];
  if (typeof urlParam === "string" && urlParam.trim()) {
    const ids = urlParam
      .split(",")
      .map((id) => id.trim())
      .filter(Boolean);
    collapsed.value = new Set(ids);
  } else {
    applyDefaultCollapse();
  }
  await hydrateNestedFiltersFromUrl();
});

function toggleSection(columnId: string) {
  const next = new Set(collapsed.value);
  if (next.has(columnId)) {
    next.delete(columnId);
  } else {
    next.add(columnId);
  }
  collapsed.value = next;
  persistCollapsed(next);
}

function isCollapsed(columnId: string): boolean {
  return collapsed.value.has(columnId);
}

const searchValue = computed({
  get: () => props.filters.searchValue.value,
  set: (val: string) => props.filters.setSearch(val),
});

function pathLabel(id: string): string {
  return id.split(".").join(" → ");
}

const visibleColumns = computed(() =>
  props.filters.visibleFilterIds.value.map((id) => {
    const found = props.columns.find((col) => col.id === id);
    if (found) return found;
    const nested = props.filters.nestedColumnMeta.value.get(id);
    return {
      id,
      label: nested?.label ?? pathLabel(id),
      columnType: nested?.columnType ?? "STRING",
      table: props.tableId,
      position: 0,
    } as IColumn;
  })
);

const visibleFilterIdsSet = computed(
  () => new Set(props.filters.visibleFilterIds.value)
);

function handlePickerApply(
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
) {
  for (const [id, meta] of nestedMeta) {
    props.filters.registerNestedColumn(id, meta);
  }
  const current = new Set(props.filters.visibleFilterIds.value);
  for (const id of current) {
    if (!selectedIds.has(id)) props.filters.toggleFilter(id);
  }
  for (const id of selectedIds) {
    if (!current.has(id)) props.filters.toggleFilter(id);
  }
}
</script>

<template>
  <div>
    <div
      id="filter-sidebar-content"
      class="rounded-t-[3px] rounded-b-[50px] bg-sidebar-gradient pb-8"
    >
      <div class="px-5 pt-5 pb-3 flex items-center justify-between">
        <h2
          class="font-display text-heading-3xl text-search-filter-title font-bold uppercase"
        >
          Filters
        </h2>
        <button
          type="button"
          class="flex items-center gap-1 text-sm text-link hover:underline"
          aria-label="Customize filters"
          @click="pickerOpen = true"
        >
          <BaseIcon name="filter" :width="16" />
          Customize
        </button>
      </div>

      <div class="px-5 pb-4">
        <InputSearch
          :id="searchInputId"
          v-model="searchValue"
          placeholder="Search..."
        />
      </div>

      <template v-for="column in visibleColumns" :key="column.id">
        <hr class="border-black opacity-10 mx-5" />
        <div
          class="p-5 flex items-center justify-between cursor-pointer group"
          role="button"
          tabindex="0"
          :aria-expanded="!isCollapsed(column.id)"
          :aria-controls="`filter-section-${column.id}`"
          @click="toggleSection(column.id)"
          @keydown.enter.space.prevent="toggleSection(column.id)"
        >
          <h3
            class="font-sans text-body-base font-bold text-search-filter-group-title group-hover:underline"
          >
            {{ column.label || column.id }}
          </h3>
          <span
            class="flex items-center justify-center w-8 h-8 rounded-full text-search-filter-group-toggle hover:bg-search-filter-group-toggle transition-transform"
            :class="{ 'rotate-180': isCollapsed(column.id) }"
          >
            <BaseIcon name="caret-up" :width="26" />
          </span>
        </div>

        <div
          v-if="!isCollapsed(column.id)"
          :id="`filter-section-${column.id}`"
          class="mx-5 mb-5"
        >
          <Column
            :column="column"
            :options="filters.getCountedOptions(column.id).value"
            :loading="filters.isCountLoading(column.id).value"
            :model-value="filters.filterStates.value.get(column.id)"
            @update:model-value="filters.setFilter(column.id, $event ?? null)"
          />
        </div>
      </template>
    </div>

    <Picker
      v-model="pickerOpen"
      :columns="columns"
      :visible-filter-ids="visibleFilterIdsSet"
      :schema-id="schemaId"
      :table-id="tableId"
      @apply="handlePickerApply"
      @cancel="pickerOpen = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, useId } from "vue";
import { useDebounceFn } from "@vueuse/core";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { UseFilters, NestedColumnMeta } from "../../../types/filters";
import { FILTER_DEBOUNCE } from "../../composables/useFilters";
import BaseIcon from "../BaseIcon.vue";
import InputSearch from "../input/Search.vue";
import Skeleton from "../Skeleton.vue";
import Column from "./Column.vue";
import Picker from "./Picker.vue";
import ColumnLabel from "./ColumnLabel.vue";

const props = defineProps<{
  filters: UseFilters;
  columns: IColumn[];
  schemaId: string;
  tableId: string;
}>();

const searchInputId = useId();
const pickerOpen = ref(false);

const localSearchInput = ref(props.filters.searchValue.value ?? "");

watch(
  () => props.filters.searchValue.value,
  (val) => {
    localSearchInput.value = val ?? "";
  }
);

const debouncedSetSearch = useDebounceFn((val: string) => {
  props.filters.setSearch(val);
}, FILTER_DEBOUNCE);

const searchValue = computed({
  get: () => localSearchInput.value,
  set: (val: string) => {
    localSearchInput.value = val;
    debouncedSetSearch(val);
  },
});

function isNestedPending(id: string): boolean {
  return (
    id.includes(".") &&
    !props.columns.find((col) => col.id === id) &&
    !props.filters.nestedColumnMeta.value.has(id)
  );
}

const visibleFilterIdsSet = computed(
  () => new Set(props.filters.visibleFilterIds.value)
);

function getFilterSelectionCount(columnId: string): number {
  const filterValue = props.filters.filterStates.value.get(columnId);
  if (!filterValue) return 0;
  if (Array.isArray(filterValue.value)) return filterValue.value.length;
  return filterValue.value ? 1 : 0;
}

function isSelectionFilter(columnId: string): boolean {
  const filterValue = props.filters.filterStates.value.get(columnId);
  return (
    filterValue !== undefined &&
    filterValue.operator === "equals" &&
    Array.isArray(filterValue.value)
  );
}

function handlePickerApply(
  selectedIds: Set<string>,
  nestedMeta: Map<string, NestedColumnMeta>
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
  <div class="px-5 pt-3 pb-3 flex items-center gap-2">
    <div class="flex items-center justify-between grow">
      <h2
        class="font-display text-heading-3xl text-search-filter-title font-bold uppercase"
      >
        Filters
      </h2>
      <button
        type="button"
        class="flex items-center gap-1 text-body-sm text-search-filter-action hover:underline cursor-pointer"
        aria-label="Customize filters"
        @click="pickerOpen = true"
      >
        <BaseIcon name="filter" :width="16" />
        Customize
      </button>
    </div>
  </div>

  <div class="px-5 pb-4">
    <InputSearch
      :id="searchInputId"
      v-model="searchValue"
      placeholder="Search..."
    />
  </div>

  <template
    v-for="column in props.filters.visibleColumns.value"
    :key="column.id"
  >
    <hr class="border-t border-filter-divider mx-5" />
    <div
      class="p-5 flex items-center gap-1 cursor-pointer group"
      role="button"
      tabindex="0"
      :aria-expanded="!filters.isCollapsed(column.id)"
      :aria-controls="`filter-section-${column.id}`"
      @click="filters.toggleCollapse(column.id)"
      @keydown.enter.space.prevent="filters.toggleCollapse(column.id)"
    >
      <h3
        class="font-sans text-body-base font-bold text-search-filter-group-title group-hover:underline min-w-0 break-words"
      >
        <ColumnLabel
          :label-parts="
            filters.nestedColumnMeta.value.get(column.id)?.labelParts ?? [
              column.label || column.id,
            ]
          "
        />
      </h3>
      <span
        v-if="filters.filterStates.value.has(column.id)"
        class="text-body-sm text-search-filter-action hover:underline cursor-pointer grow text-right"
        @click.stop="filters.removeFilter(column.id)"
      >
        <template v-if="isSelectionFilter(column.id)">
          Remove {{ getFilterSelectionCount(column.id) }} selected
        </template>
        <template v-else>Clear</template>
      </span>
      <span
        class="flex items-center justify-center w-8 h-8 rounded-full text-search-filter-group-toggle group-hover:bg-search-filter-group-toggle transition-transform shrink-0"
        :class="{ 'rotate-180': filters.isCollapsed(column.id) }"
      >
        <BaseIcon name="caret-up" :width="26" />
      </span>
    </div>

    <div
      v-if="!filters.isCollapsed(column.id)"
      :id="`filter-section-${column.id}`"
      class="mx-5 mb-5"
    >
      <Skeleton v-if="isNestedPending(column.id)" :lines="3" />
      <Column
        v-else
        :column="column"
        :options="filters.getCountedOptions(column.id).value"
        :loading="filters.isCountLoading(column.id).value"
        :saturated="filters.isSaturated(column.id).value"
        :has-count-error="filters.hasCountError(column.id).value"
        :model-value="filters.filterStates.value.get(column.id)"
        @update:model-value="filters.setFilter(column.id, $event ?? null)"
      />
    </div>
  </template>

  <Picker
    v-model="pickerOpen"
    :columns="columns"
    :visible-filter-ids="visibleFilterIdsSet"
    :schema-id="schemaId"
    :table-id="tableId"
    @apply="handlePickerApply"
    @cancel="pickerOpen = false"
    @reset="filters.resetFilters()"
  />
</template>

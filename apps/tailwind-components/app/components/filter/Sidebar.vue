<script setup lang="ts">
import { ref, computed, watch, useId } from "vue";
import { useDebounceFn } from "@vueuse/core";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { UseFilters } from "../../../types/filters";
import { FILTER_DEBOUNCE } from "../../composables/useFilters";
import BaseIcon from "../BaseIcon.vue";
import Button from "../Button.vue";
import InputSearch from "../input/Search.vue";
import Skeleton from "../Skeleton.vue";
import Column from "./Column.vue";
import Picker from "./Picker.vue";

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

const visibleColumns = computed(() =>
  props.filters.visibleFilterIds.value.map((id) => {
    const found = props.columns.find((col) => col.id === id);
    if (found) return found;
    const nested = props.filters.nestedColumnMeta.value.get(id);
    return {
      id,
      label: nested?.label ?? "",
      columnType: nested?.columnType ?? "STRING",
      table: props.tableId,
      position: 0,
    } as IColumn;
  })
);

const visibleFilterIdsSet = computed(
  () => new Set(props.filters.visibleFilterIds.value)
);

function getFilterSelectionCount(columnId: string): number {
  const filterValue = props.filters.filterStates.value.get(columnId);
  if (!filterValue) return 0;
  if (Array.isArray(filterValue.value)) return filterValue.value.length;
  return filterValue.value ? 1 : 0;
}

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
      class="rounded-t-3px rounded-b-50px bg-sidebar-gradient pb-8"
    >
      <div class="px-5 pt-5 pb-3 flex items-center justify-between">
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
          class="p-5 flex items-center gap-1 cursor-pointer group"
          role="button"
          tabindex="0"
          :aria-expanded="!filters.isCollapsed(column.id)"
          :aria-controls="`filter-section-${column.id}`"
          @click="filters.toggleCollapse(column.id)"
          @keydown.enter.space.prevent="filters.toggleCollapse(column.id)"
        >
          <h3
            class="font-sans text-body-base font-bold text-search-filter-group-title group-hover:underline"
            :aria-label="column.label || column.id.split('.').join(' → ')"
          >
            <template v-if="column.label">{{ column.label }}</template>
            <template v-else>
              <template v-for="(part, i) in column.id.split('.')" :key="i">
                <span>{{ part }}</span
                ><span
                  v-if="i < column.id.split('.').length - 1"
                  class="text-gray-400"
                  aria-hidden="true"
                  >&nbsp;→&nbsp;</span
                >
              </template>
            </template>
          </h3>
          <span
            v-if="filters.filterStates.value.has(column.id)"
            class="text-body-sm text-search-filter-action hover:underline cursor-pointer grow text-right"
            @click.stop="filters.removeFilter(column.id)"
          >
            <template v-if="getFilterSelectionCount(column.id) > 1">
              Remove {{ getFilterSelectionCount(column.id) }} selected
            </template>
            <template v-else>Clear</template>
          </span>
          <span
            class="flex items-center justify-center w-8 h-8 rounded-full text-search-filter-group-toggle hover:bg-search-filter-group-toggle transition-transform shrink-0"
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
      @reset="filters.resetFilters()"
    />
  </div>
</template>

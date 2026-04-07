<script setup lang="ts">
import { ref, computed, onMounted, useId } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { UseFilters } from "../../../types/filters";
import BaseIcon from "../BaseIcon.vue";
import InputSearch from "../input/Search.vue";
import FilterOptions from "./FilterOptions.vue";
import FilterPicker from "./FilterPicker.vue";

const props = defineProps<{
  filters: UseFilters;
  columns: IColumn[];
  schemaId: string;
  tableId: string;
}>();

const searchInputId = useId();
const collapsed = ref(new Set<string>());
const pickerOpen = ref(false);

onMounted(() => {
  const visibleIds = [...props.filters.visibleFilterIds.value];
  visibleIds.forEach((id, index) => {
    if (index >= 5 && !props.filters.filterStates.value.has(id)) {
      collapsed.value.add(id);
    }
  });
});

function toggleSection(columnId: string) {
  const next = new Set(collapsed.value);
  if (next.has(columnId)) {
    next.delete(columnId);
  } else {
    next.add(columnId);
  }
  collapsed.value = next;
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
  nestedMeta: Map<string, { label: string; columnType: string }>
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
      class="rounded-t-[3px] rounded-b-[50px] bg-sidebar-gradient"
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

      <template v-for="(column, index) in visibleColumns" :key="column.id">
        <hr class="border-black opacity-10 mx-5" />
        <div class="px-5 pt-3 pb-1">
          <button
            type="button"
            class="flex items-center justify-between w-full text-left group"
            :aria-expanded="!isCollapsed(column.id)"
            :aria-controls="`filter-section-${column.id}`"
            @click="toggleSection(column.id)"
          >
            <span
              class="font-sans text-body-base font-bold text-search-filter-group-title group-hover:underline"
            >
              {{ column.label || column.id }}
            </span>
            <span
              class="flex items-center justify-center w-8 h-8 rounded-full text-search-filter-group-toggle hover:bg-search-filter-group-toggle transition-transform"
              :class="{ 'rotate-180': !isCollapsed(column.id) }"
            >
              <BaseIcon name="caret-down" :width="26" />
            </span>
          </button>
        </div>

        <div
          v-if="!isCollapsed(column.id)"
          :id="`filter-section-${column.id}`"
          class="mx-5 mb-5"
        >
          <FilterOptions
            :column="column"
            :options="filters.getCountedOptions(column.id).value"
            :loading="filters.isCountLoading(column.id).value"
            :model-value="filters.filterStates.value.get(column.id)"
            @update:model-value="filters.setFilter(column.id, $event ?? null)"
          />
        </div>
      </template>
    </div>

    <FilterPicker
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

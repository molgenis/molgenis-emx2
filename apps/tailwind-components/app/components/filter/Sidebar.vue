<script setup lang="ts">
import { computed, useId } from "vue";
import type { UseFilters } from "../../../types/filters";
import FilterColumn from "./Column.vue";
import InputSearch from "../input/Search.vue";
import FilterPicker from "./FilterPicker.vue";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    tableId: string;
    filters: UseFilters;
    title?: string;
    showSearch?: boolean;
  }>(),
  {
    title: "Filters",
    showSearch: false,
  }
);

const searchInputId = useId();

const searchTerms = computed({
  get: () => props.filters.searchValue.value,
  set: (val: string) => {
    props.filters.setSearch(val);
  },
});

function getFilterValue(columnId: string) {
  return props.filters.filterStates.value.get(columnId) || null;
}
</script>

<template>
  <div
    class="filter-sidebar-context rounded-t-3px rounded-b-50px pb-8 bg-sidebar-gradient"
  >
    <div class="p-5">
      <h2
        class="uppercase font-display text-heading-3xl text-search-filter-title"
      >
        {{ title }}
      </h2>
    </div>
    <div class="px-5 pb-3 flex justify-end">
      <FilterPicker :filters="filters" />
    </div>

    <div v-if="showSearch" class="px-5 pb-5">
      <InputSearch
        :id="searchInputId"
        v-model="searchTerms"
        placeholder="Search..."
      />
    </div>

    <FilterColumn
      v-for="filter in filters.resolvedFilters.value"
      :key="filter.fullPath"
      :column="filter.column"
      :label="filter.label"
      :model-value="getFilterValue(filter.fullPath)"
      @update:model-value="filters.setFilterValue(filter.fullPath, $event)"
      :removable="true"
      @remove="filters.toggleFilter(filter.fullPath)"
      :count-fetcher="filters.getCountFetcher(filter.fullPath)"
    />
  </div>
</template>

<style scoped>
.filter-sidebar-context {
  --text-color-title-contrast: var(--text-color-search-filter-group-title);
  --text-color-input-description: var(--text-color-search-filter-group-title);
  --text-color-input: var(--text-color-search-filter-group-title);
}
</style>

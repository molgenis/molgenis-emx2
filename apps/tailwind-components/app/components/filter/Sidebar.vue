<script setup lang="ts">
import { computed, ref, useId } from "vue";
import type { UseFilters } from "../../../types/filters";
import FilterColumn from "./Column.vue";
import InputSearch from "../input/Search.vue";
import FilterPicker from "./FilterPicker.vue";
import BaseIcon from "../BaseIcon.vue";

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

const collapsed = ref<Set<string>>(
  new Set(
    props.filters.resolvedFilters.value
      .filter((f) => !props.filters.filterStates.value.has(f.fullPath))
      .map((f) => f.fullPath)
  )
);

function toggleSection(fullPath: string) {
  const next = new Set(collapsed.value);
  if (next.has(fullPath)) {
    next.delete(fullPath);
  } else {
    next.add(fullPath);
  }
  collapsed.value = next;
}

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
    <div class="px-5 pt-5 pb-3 flex items-center justify-between">
      <h2
        class="uppercase font-display text-heading-3xl text-search-filter-title"
      >
        {{ title }}
      </h2>
      <FilterPicker :filters="filters" />
    </div>

    <div v-if="showSearch" class="px-5 pb-5">
      <InputSearch
        :id="searchInputId"
        v-model="searchTerms"
        placeholder="Search..."
      />
    </div>

    <div v-for="filter in filters.resolvedFilters.value" :key="filter.fullPath">
      <hr class="mx-5 border-black opacity-10" />
      <div class="px-5 pt-4 pb-2">
        <div
          class="inline-flex gap-1 group"
          :aria-expanded="!collapsed.has(filter.fullPath)"
          :aria-controls="`filter-section-${filter.fullPath}`"
          @click="toggleSection(filter.fullPath)"
        >
          <h3
            class="font-sans text-body-base font-bold mr-[5px] text-search-filter-group-title group-hover:underline group-hover:cursor-pointer"
          >
            {{ filter.label }}
          </h3>
          <span
            class="flex items-center justify-center w-8 h-8 rounded-full text-search-filter-group-toggle group-hover:bg-search-filter-group-toggle group-hover:cursor-pointer"
          >
            <BaseIcon
              :name="
                collapsed.has(filter.fullPath) ? 'caret-right' : 'caret-up'
              "
              :width="26"
            />
          </span>
        </div>
      </div>
      <div
        v-if="!collapsed.has(filter.fullPath)"
        :id="`filter-section-${filter.fullPath}`"
        class="mb-5 mx-5 text-search-filter-group-title"
      >
        <FilterColumn
          :column="filter.column"
          :label="filter.label"
          :model-value="getFilterValue(filter.fullPath)"
          @update:model-value="filters.setFilterValue(filter.fullPath, $event)"
          :removable="false"
          :show-label="false"
          :count-fetcher="filters.getCountFetcher(filter.fullPath)"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.filter-sidebar-context {
  --text-color-title-contrast: var(--text-color-search-filter-group-title);
  --text-color-input-description: var(--text-color-search-filter-group-title);
  --text-color-input: var(--text-color-search-filter-group-title);
}
</style>

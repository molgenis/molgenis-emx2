<script setup lang="ts">
import { useId } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";
import FilterColumn from "./Column.vue";
import InputSearch from "../input/Search.vue";

const props = withDefaults(
  defineProps<{
    columns: IColumn[];
    title?: string;
    mobileDisplay?: boolean;
    showSearch?: boolean;
  }>(),
  {
    title: "Filters",
    mobileDisplay: false,
    showSearch: false,
  }
);

const searchInputId = useId();

const filterStates = defineModel<Map<string, IFilterValue>>("filterStates", {
  default: () => new Map(),
});

const searchTerms = defineModel<string>("searchTerms", {
  default: "",
});

function getFilterValue(columnId: string): IFilterValue | null {
  return filterStates.value.get(columnId) || null;
}

function setFilterValue(
  columnId: string,
  value: IFilterValue | null | undefined
) {
  const newMap = new Map(filterStates.value);
  if (value === null || value === undefined) {
    newMap.delete(columnId);
  } else {
    newMap.set(columnId, value);
  }
  filterStates.value = newMap;
}
</script>

<template>
  <div
    class="mt-7.5 rounded-t-3px rounded-b-50px"
    :class="{ 'bg-sidebar-gradient': !mobileDisplay }"
  >
    <h2
      v-if="!mobileDisplay"
      class="p-5 uppercase font-display text-heading-3xl text-search-filter-title"
    >
      {{ title }}
    </h2>

    <div v-if="showSearch" class="px-5 pb-5">
      <InputSearch
        :id="searchInputId"
        v-model="searchTerms"
        placeholder="Search..."
        size="small"
      />
    </div>

    <FilterColumn
      v-for="column in columns"
      :key="column.id"
      :column="column"
      :model-value="getFilterValue(column.id)"
      @update:model-value="setFilterValue(column.id, $event)"
      :collapsed="true"
      :mobile-display="mobileDisplay"
    />

    <hr class="mx-5 border-black opacity-10" />
  </div>
</template>

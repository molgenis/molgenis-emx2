<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";
import FilterColumn from "./Column.vue";

const props = withDefaults(
  defineProps<{
    columns: IColumn[];
    title?: string;
    mobileDisplay?: boolean;
  }>(),
  {
    title: "Filters",
    mobileDisplay: false,
  }
);

const filterStates = defineModel<Map<string, IFilterValue>>("filterStates", {
  default: () => new Map(),
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

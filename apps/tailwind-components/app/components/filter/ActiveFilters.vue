<script setup lang="ts">
import { computed } from "vue";
import Button from "../Button.vue";
import ColumnLabel from "./ColumnLabel.vue";
import type { ActiveFilter } from "../../../types/filters";

const props = defineProps<{
  filters: ActiveFilter[];
  searchValue?: string;
}>();

const emit = defineEmits<{
  remove: [columnId: string];
  clearAll: [];
  clearSearch: [];
}>();

const hasSearchChip = computed(() => (props.searchValue ?? "").length > 0);
const hasAnyContent = computed(
  () => props.filters.length > 0 || hasSearchChip.value
);
</script>

<template>
  <div
    v-if="hasAnyContent"
    class="flex flex-wrap items-center gap-3 bg-sidebar-gradient rounded-input px-3 py-2 mb-2"
  >
    <span
      class="text-search-filter-group-title text-body-sm whitespace-nowrap mr-1"
      >Active filters</span
    >

    <Button
      v-if="hasSearchChip"
      @click="emit('clearSearch')"
      type="filterWell"
      size="tiny"
      icon="cross"
      icon-position="right"
      :aria-label="`Remove search: ${props.searchValue}`"
    >
      <span class="inline-block font-bold max-w-48 truncate align-bottom"
        >Search</span
      >
      <span class="inline-block max-w-32 truncate align-bottom ml-1.5">{{
        props.searchValue
      }}</span>
    </Button>

    <VDropdown
      v-for="filter in props.filters"
      :key="filter.columnId"
      :triggers="['hover', 'focus']"
      :distance="12"
      theme="tooltip"
      class="!max-w-none shrink-0"
    >
      <Button
        @click="emit('remove', filter.columnId)"
        type="filterWell"
        size="tiny"
        icon="cross"
        icon-position="right"
        :aria-label="`Remove filter: ${filter.labelParts.join(' / ')}`"
      >
        <span class="inline-block font-bold max-w-48 truncate align-bottom">
          <ColumnLabel :label-parts="filter.labelParts" />
        </span>
        <span
          v-if="filter.values.length <= 1"
          class="inline-block max-w-32 truncate align-bottom ml-1.5"
          >{{ filter.displayValue }}</span
        >
        <span
          v-else
          class="inline-block opacity-70 max-w-32 truncate align-bottom ml-1.5"
          >{{ filter.displayValue }}</span
        >
      </Button>
      <template #popper>
        <div class="px-1 py-0.5">
          <div class="font-bold">
            <ColumnLabel :label-parts="filter.labelParts" />
          </div>
          <ul v-if="filter.values.length > 1" class="list-disc pl-3">
            <li v-for="item in filter.values" :key="item">
              {{ item }}
            </li>
          </ul>
          <div v-else>{{ filter.displayValue }}</div>
        </div>
      </template>
    </VDropdown>

    <Button type="text" size="tiny" @click="emit('clearAll')">
      Clear all filters
    </Button>
  </div>
</template>

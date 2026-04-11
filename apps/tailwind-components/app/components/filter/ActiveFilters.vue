<script setup lang="ts">
import Button from "../Button.vue";
import type { ActiveFilter } from "../../../types/filters";

const props = defineProps<{
  filters: ActiveFilter[];
}>();

const emit = defineEmits<{
  remove: [columnId: string];
  clearAll: [];
}>();
</script>

<template>
  <div
    v-if="props.filters.length > 0"
    class="flex flex-wrap items-center gap-3 bg-button-primary rounded-input px-3 py-2 mb-2"
  >
    <span class="text-button-primary text-xs whitespace-nowrap mr-1"
      >Active filters</span
    >

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
        :aria-label="`Remove filter: ${filter.label}`"
      >
        <span class="inline-block font-bold max-w-48 truncate align-bottom">{{
          filter.label
        }}</span>
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
          <div class="font-bold">{{ filter.label }}</div>
          <ul
            v-if="filter.values.length > 1"
            style="list-style-type: disc"
            class="pl-3"
          >
            <li v-for="item in filter.values" :key="item">
              {{ item }}
            </li>
          </ul>
          <div v-else>{{ filter.displayValue }}</div>
        </div>
      </template>
    </VDropdown>

    <button
      @click="emit('clearAll')"
      class="whitespace-nowrap text-button-primary text-xs underline hover:no-underline cursor-pointer"
    >
      Clear all
    </button>
  </div>
</template>

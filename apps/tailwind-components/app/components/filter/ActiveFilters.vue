<script setup lang="ts">
import { computed, useId } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";
import BaseIcon from "../BaseIcon.vue";

const ariaId = useId();

const props = defineProps<{
  filters: Map<string, IFilterValue>;
  columns: IColumn[];
}>();

const emit = defineEmits<{
  remove: [columnId: string];
  clearAll: [];
}>();

interface ActiveFilter {
  columnId: string;
  label: string;
  displayValue: string;
  isMultiValue: boolean;
  values: string[];
}

const activeFilters = computed<ActiveFilter[]>(() => {
  const result: ActiveFilter[] = [];

  for (const [columnId, filterValue] of props.filters) {
    const column = props.columns.find((c) => c.id === columnId);
    if (!column) continue;

    const label = column.displayConfig?.label || column.label || column.id;
    const { displayValue, isMultiValue, values } =
      formatFilterValue(filterValue);

    result.push({ columnId, label, displayValue, isMultiValue, values });
  }

  return result;
});

function formatFilterValue(filterValue: IFilterValue): {
  displayValue: string;
  isMultiValue: boolean;
  values: string[];
} {
  const { operator, value } = filterValue;

  switch (operator) {
    case "between":
      if (Array.isArray(value) && value.length === 2) {
        const [min, max] = value;
        if (min && max)
          return {
            displayValue: `${min} - ${max}`,
            isMultiValue: false,
            values: [],
          };
        if (min)
          return { displayValue: `≥ ${min}`, isMultiValue: false, values: [] };
        if (max)
          return { displayValue: `≤ ${max}`, isMultiValue: false, values: [] };
      }
      return { displayValue: "", isMultiValue: false, values: [] };

    case "in":
      if (Array.isArray(value)) {
        if (value.length === 0)
          return { displayValue: "", isMultiValue: false, values: [] };
        const formatted = value.map((v) => {
          if (typeof v === "object" && v !== null) {
            return extractDisplayValue(v);
          }
          return String(v);
        });
        if (value.length > 1) {
          return {
            displayValue: `${value.length}`,
            isMultiValue: true,
            values: formatted,
          };
        }
        return {
          displayValue: formatted[0] || "",
          isMultiValue: false,
          values: [],
        };
      }
      if (typeof value === "object" && value !== null) {
        const displayValue = extractDisplayValue(value);
        return { displayValue, isMultiValue: false, values: [] };
      }
      return { displayValue: String(value), isMultiValue: false, values: [] };

    case "notNull":
      return { displayValue: "has value", isMultiValue: false, values: [] };

    case "isNull":
      return { displayValue: "is empty", isMultiValue: false, values: [] };

    case "like":
    case "equals":
    default:
      if (Array.isArray(value)) {
        const formatted = value.map((v) => {
          if (typeof v === "object" && v !== null) {
            return extractDisplayValue(v);
          }
          return String(v);
        });
        if (value.length > 1) {
          return {
            displayValue: `${value.length}`,
            isMultiValue: true,
            values: formatted,
          };
        }
        return {
          displayValue: formatted[0] || "",
          isMultiValue: false,
          values: [],
        };
      }
      if (typeof value === "object" && value !== null) {
        const displayValue = extractDisplayValue(value);
        return { displayValue, isMultiValue: false, values: [] };
      }
      return { displayValue: String(value), isMultiValue: false, values: [] };
  }
}

function extractDisplayValue(obj: Record<string, unknown>): string {
  if (obj.name) return String(obj.name);
  if (obj.label) return String(obj.label);
  const firstValue = Object.values(obj)[0];
  return String(firstValue);
}

function handleRemove(columnId: string) {
  emit("remove", columnId);
}

function handleClearAll() {
  emit("clearAll");
}
</script>

<template>
  <div
    v-if="activeFilters.length > 0"
    class="flex flex-wrap gap-2 items-center"
  >
    <VDropdown
      v-for="(filter, index) in activeFilters"
      :key="filter.columnId"
      :aria-id="ariaId + '_' + index"
      :triggers="filter.isMultiValue ? ['hover', 'focus'] : []"
      :distance="12"
      theme="tooltip"
    >
      <button
        @click="handleRemove(filter.columnId)"
        class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-sidebar-gradient text-body-sm text-search-filter-group-title hover:opacity-80 transition-opacity"
        :aria-label="`Remove filter: ${filter.label}`"
      >
        <span class="font-bold">{{ filter.label }}</span>
        <span v-if="!filter.isMultiValue">- {{ filter.displayValue }}</span>
        <span v-else class="text-gray-600">- {{ filter.displayValue }}</span>
        <BaseIcon name="cross" :width="12" class="flex-shrink-0" />
      </button>
      <template #popper v-if="filter.isMultiValue">
        <ul style="list-style-type: disc" class="pl-3 min-w-95">
          <li v-for="item in filter.values" :key="item">
            {{ item }}
          </li>
        </ul>
      </template>
    </VDropdown>

    <button
      v-if="activeFilters.length > 1"
      @click="handleClearAll"
      class="text-body-sm text-button-text hover:underline"
    >
      Clear all
    </button>
  </div>
</template>

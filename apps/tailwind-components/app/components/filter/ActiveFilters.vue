<script setup lang="ts">
import { computed, ref, watch, useId } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";
import Button from "../Button.vue";
import fetchTableMetadata from "../../composables/fetchTableMetadata";

const ariaId = useId();

const props = defineProps<{
  filters: Map<string, IFilterValue>;
  columns: IColumn[];
  schemaId: string;
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

const nestedColumnLabels = ref<Map<string, string>>(new Map());

watch(
  () => props.filters,
  async (filters) => {
    for (const columnId of filters.keys()) {
      if (!columnId.includes(".")) continue;
      if (nestedColumnLabels.value.has(columnId)) continue;

      const segments = columnId.split(".");
      let currentColumns: IColumn[] = props.columns;
      let currentSchemaId = props.schemaId;
      const labels: string[] = [];

      for (let depth = 0; depth < segments.length; depth++) {
        const segment = segments[depth]!;
        const column = currentColumns.find((c) => c.id === segment);
        if (!column) break;

        labels.push(column.displayConfig?.label || column.label || segment);

        if (depth < segments.length - 1 && column.refTableId) {
          const refSchemaId = column.refSchemaId || currentSchemaId;
          try {
            const metadata = await fetchTableMetadata(
              refSchemaId,
              column.refTableId
            );
            currentColumns = metadata.columns;
            currentSchemaId = refSchemaId;
          } catch {
            break;
          }
        }
      }

      if (labels.length === segments.length) {
        nestedColumnLabels.value = new Map(nestedColumnLabels.value).set(
          columnId,
          labels.join(" → ")
        );
      }
    }
  },
  { immediate: true, deep: true }
);

const activeFilters = computed<ActiveFilter[]>(() => {
  const result: ActiveFilter[] = [];

  for (const [columnId, filterValue] of props.filters) {
    const label = getColumnLabel(columnId);
    const { displayValue, isMultiValue, values } =
      formatFilterValue(filterValue);
    result.push({ columnId, label, displayValue, isMultiValue, values });
  }

  return result;
});

function getColumnLabel(columnId: string): string {
  if (columnId.includes(".")) {
    const cached = nestedColumnLabels.value.get(columnId);
    if (cached) return cached;

    const segments = columnId.split(".");
    const rootColumn = props.columns.find((c) => c.id === segments[0]);
    const rootLabel = rootColumn
      ? rootColumn.displayConfig?.label || rootColumn.label || segments[0]!
      : segments[0]!;
    return [rootLabel, ...segments.slice(1)].join(" → ");
  }

  const column = props.columns.find((c) => c.id === columnId);
  if (!column) return columnId;
  return column.displayConfig?.label || column.label || column.id;
}

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
  <div v-if="activeFilters.length > 0" class="flex flex-wrap gap-2 items-start">
    <VDropdown
      v-for="(filter, index) in activeFilters"
      :key="filter.columnId"
      :aria-id="ariaId + '_' + index"
      :triggers="['hover', 'focus']"
      :distance="12"
      theme="tooltip"
      class="shrink-0"
    >
      <Button
        @click="handleRemove(filter.columnId)"
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
          v-if="!filter.isMultiValue"
          class="inline-block max-w-32 truncate align-bottom"
          >- {{ filter.displayValue }}</span
        >
        <span
          v-else
          class="inline-block text-gray-600 max-w-32 truncate align-bottom"
          >- {{ filter.displayValue }}</span
        >
      </Button>
      <template #popper>
        <div class="px-1 py-0.5">
          <div class="font-bold">{{ filter.label }}</div>
          <ul
            v-if="filter.isMultiValue"
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

    <Button
      v-if="activeFilters.length > 1"
      @click="handleClearAll"
      type="text"
      size="tiny"
    >
      Clear all
    </Button>
  </div>
</template>

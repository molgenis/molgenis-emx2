<template>
  <div class="p-6">
    <h2 class="text-2xl font-bold text-title-contrast mb-6">
      Filter System Demo
    </h2>

    <div class="flex gap-6">
      <div class="w-80 shrink-0">
        <FilterSidebar
          :filters="mockFilters"
          :columns="columns"
          schema-id="demo"
          table-id="Samples"
        />
      </div>

      <div class="flex-1 min-w-0">
        <FilterActiveFilters
          :filters="mockFilters.activeFilters.value"
          @remove="mockFilters.removeFilter"
          @clear-all="mockFilters.clearFilters"
        />

        <div
          class="rounded border border-input bg-content p-4 min-h-48 flex items-center justify-center text-disabled text-sm"
        >
          Table content area — apply filters to update the query below
        </div>

        <div class="mt-4 rounded border border-input bg-content p-4">
          <div class="text-xs font-semibold text-record-label mb-2 uppercase">
            gqlFilter
          </div>
          <pre class="text-xs text-record-value overflow-auto max-h-64">{{
            JSON.stringify(mockFilters.gqlFilter.value, null, 2)
          }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type {
  UseFilters,
  IFilterValue,
  ActiveFilter,
  IGraphQLFilter,
} from "../../../types/filters";
import type { CountedOption } from "../../utils/fetchCounts";
import FilterSidebar from "../../components/filter/Sidebar.vue";
import FilterActiveFilters from "../../components/filter/ActiveFilters.vue";
import { buildGraphQLFilter } from "../../utils/buildFilter";
import { formatFilterValue } from "../../utils/formatFilterValue";
import { buildLabelMap } from "../../composables/useFilters";

const columns: IColumn[] = [
  {
    id: "species",
    name: "species",
    label: "Species",
    columnType: "ONTOLOGY",
    table: "Samples",
    position: 1,
  } as IColumn,
  {
    id: "available",
    name: "available",
    label: "Available",
    columnType: "BOOL",
    table: "Samples",
    position: 2,
  } as IColumn,
  {
    id: "status",
    name: "status",
    label: "Status",
    columnType: "CHECKBOX",
    table: "Samples",
    position: 3,
  } as IColumn,
  {
    id: "age",
    name: "age",
    label: "Age (years)",
    columnType: "INT",
    table: "Samples",
    position: 4,
  } as IColumn,
  {
    id: "name",
    name: "name",
    label: "Name",
    columnType: "STRING",
    table: "Samples",
    position: 5,
  } as IColumn,
  {
    id: "owner",
    name: "owner",
    label: "Owner",
    columnType: "REF",
    refTableId: "Persons",
    table: "Samples",
    position: 6,
  } as IColumn,
];

const mockCounts: Record<string, CountedOption[]> = {
  species: [
    {
      name: "Mammalia",
      label: "Mammals",
      count: 142,
      children: [
        { name: "Homo sapiens", label: "Human", count: 98, children: [] },
        { name: "Mus musculus", label: "Mouse", count: 44, children: [] },
      ],
    },
    {
      name: "Aves",
      label: "Birds",
      count: 27,
      children: [
        { name: "Gallus gallus", label: "Chicken", count: 27, children: [] },
      ],
    },
  ],
  available: [
    { name: "true", count: 113 },
    { name: "false", count: 56 },
  ],
  status: [
    { name: "active", label: "Active", count: 89 },
    { name: "archived", label: "Archived", count: 52 },
    { name: "pending", label: "Pending", count: 28 },
  ],
};

const filterStatesRef = ref<Map<string, IFilterValue>>(new Map());
const searchValueRef = ref("");
const visibleFilterIdsRef = ref<string[]>([
  "species",
  "available",
  "status",
  "age",
]);
const columnsRef = ref<IColumn[]>(columns);

const gqlFilter = computed<IGraphQLFilter>(() =>
  buildGraphQLFilter(filterStatesRef.value, columns, searchValueRef.value)
);

const activeFilters = computed<ActiveFilter[]>(() => {
  const result: ActiveFilter[] = [];
  for (const [columnId, filterValue] of filterStatesRef.value) {
    const column = columns.find((col) => col.id === columnId);
    const label = column?.label || column?.id || columnId;
    const effectiveColumn =
      column ?? ({ id: columnId, columnType: "STRING" } as IColumn);
    const optionLabels = buildLabelMap(effectiveColumn, null);
    const { displayValue, values } = formatFilterValue(
      filterValue,
      optionLabels
    );
    if (displayValue) {
      result.push({ columnId, label, displayValue, values });
    }
  }
  return result;
});

function setFilter(columnId: string, value: IFilterValue | null) {
  const next = new Map(filterStatesRef.value);
  if (value === null) {
    next.delete(columnId);
  } else {
    next.set(columnId, value);
  }
  filterStatesRef.value = next;
}

function removeFilter(columnId: string) {
  setFilter(columnId, null);
}

function clearFilters() {
  filterStatesRef.value = new Map();
  searchValueRef.value = "";
}

function toggleFilter(columnId: string) {
  if (visibleFilterIdsRef.value.includes(columnId)) {
    visibleFilterIdsRef.value = visibleFilterIdsRef.value.filter(
      (id) => id !== columnId
    );
    removeFilter(columnId);
  } else {
    visibleFilterIdsRef.value = [columnId, ...visibleFilterIdsRef.value];
  }
}

function resetFilters() {
  clearFilters();
  visibleFilterIdsRef.value = ["species", "available", "status", "age"];
}

function getCountedOptions(columnId: string) {
  return computed<CountedOption[]>(() => mockCounts[columnId] ?? []);
}

function isCountLoading(columnId: string) {
  return computed(() => false);
}

const collapsedRef = ref(new Set<string>());

const mockFilters: UseFilters = {
  filterStates: filterStatesRef,
  searchValue: searchValueRef,
  gqlFilter,
  activeFilters,
  setFilter,
  setSearch: (val: string) => {
    searchValueRef.value = val;
  },
  clearFilters,
  removeFilter,
  columns: columnsRef,
  visibleFilterIds: visibleFilterIdsRef,
  toggleFilter,
  resetFilters,
  getCountedOptions,
  isCountLoading,
  nestedColumnMeta: ref(new Map()),
  registerNestedColumn: () => {},
  schemaId: "demo",
  tableId: "Samples",
  collapsedIds: computed(() => collapsedRef.value),
  toggleCollapse: (id: string) => {
    const next = new Set(collapsedRef.value);
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    collapsedRef.value = next;
  },
  isCollapsed: (id: string) => collapsedRef.value.has(id),
  hydrateNestedFilters: async () => {},
};
</script>

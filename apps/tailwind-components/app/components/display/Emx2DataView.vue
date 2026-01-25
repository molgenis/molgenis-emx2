<script setup lang="ts">
import { ref, computed, watch, useId } from "vue";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";
import { useTableData } from "../../composables/useTableData";
import { useFilters } from "../../composables/useFilters";
import { rowToString } from "../../utils/rowToString";
import InputSearch from "../input/Search.vue";
import InlinePagination from "./InlinePagination.vue";
import LoadingContent from "../LoadingContent.vue";
import CardList from "../CardList.vue";
import CardListItem from "../CardListItem.vue";
import FilterSidebar from "../filter/Sidebar.vue";

export interface IColumnDisplayOptions {
  href?: (row: IRow) => string;
  onClick?: (row: IRow) => void;
}

const props = withDefaults(
  defineProps<{
    schemaId: string;
    tableId: string;
    layout?: "table" | "list" | "cards";
    showFilters?: boolean;
    filterPosition?: "sidebar" | "topbar";
    filterableColumns?: string[];
    showSearch?: boolean;
    pagingLimit?: number;
    rowLabel?: string;
    displayOptions?: Record<string, IColumnDisplayOptions>;
    visibleColumns?: string[];
  }>(),
  {
    layout: "list",
    showFilters: false,
    filterPosition: "sidebar",
    showSearch: true,
    pagingLimit: 10,
  }
);

defineSlots<{
  default?: (props: { row: IRow; label: string }) => any;
  card?: (props: { row: IRow; label: string }) => any;
}>();

const searchInputId = useId();
const page = ref(1);
const searchTerms = ref("");

// compute filterable columns from metadata (initial empty state)
const metadataRef = ref<IColumn[]>([]);

const filterableColumnsComputed = computed<IColumn[]>(() => {
  const filterableTypes = [
    "STRING",
    "TEXT",
    "EMAIL",
    "INT",
    "DECIMAL",
    "LONG",
    "NON_NEGATIVE_INT",
    "DATE",
    "DATETIME",
    "BOOL",
    "REF",
    "REF_ARRAY",
    "ONTOLOGY",
    "ONTOLOGY_ARRAY",
  ];

  let cols = metadataRef.value.filter(
    (col) =>
      filterableTypes.includes(col.columnType) && !col.id.startsWith("mg_")
  );

  if (props.filterableColumns && props.filterableColumns.length > 0) {
    cols = cols.filter((col) => props.filterableColumns!.includes(col.id));
  }

  return cols;
});

// use filters composable
const columnsRef = computed(() => filterableColumnsComputed.value);
const { filterStates, gqlFilter } = useFilters(columnsRef, {
  debounceMs: 300,
});

// fetch data using useTableData with filter
const { metadata, rows, status, totalPages, showPagination, errorMessage } =
  useTableData(props.schemaId, props.tableId, {
    pageSize: props.pagingLimit,
    page,
    searchTerms,
    filter: gqlFilter,
  });

// update metadataRef when metadata loads
watch(
  metadata,
  (newMeta) => {
    if (newMeta?.columns) {
      metadataRef.value = newMeta.columns;
    }
  },
  { immediate: true }
);

const errorText = computed(
  () =>
    errorMessage.value ||
    (status.value === "error" ? "Failed to load data" : undefined)
);

// row label template from props or metadata
const rowLabelTemplate = computed(() => {
  if (props.rowLabel) return props.rowLabel;
  const keyCol = metadata.value?.columns?.find((c) => c.key === 1);
  return keyCol?.refLabel || keyCol?.refLabelDefault || "${name}";
});

// visible columns for table layout
const visibleColumnsComputed = computed(() => {
  const cols = props.visibleColumns || [];
  if (!metadata.value?.columns) return cols;
  return cols.filter((colId) =>
    metadata.value!.columns!.some((c) => c.id === colId)
  );
});

const tableColumns = computed<IColumn[]>(() => {
  if (!metadata.value?.columns) return [];
  return visibleColumnsComputed.value
    .map((colId) => metadata.value!.columns!.find((c) => c.id === colId))
    .filter((col): col is IColumn => col !== undefined);
});

function getCellValue(row: IRow, colId: string): string {
  const val = row[colId];
  if (val === null || val === undefined) return "";
  if (Array.isArray(val)) {
    return val
      .map((v) => (typeof v === "object" ? v.label || v.name || "" : String(v)))
      .join(", ");
  }
  if (typeof val === "object") {
    const obj = val as Record<string, unknown>;
    return String(obj.label || obj.name || JSON.stringify(val));
  }
  return String(val);
}

watch(searchTerms, () => {
  page.value = 1;
});

function getLabel(row: IRow): string {
  return rowToString(row, rowLabelTemplate.value) || "";
}

function getColumnOptions(colId: string): IColumnDisplayOptions | undefined {
  return props.displayOptions?.[colId];
}

const firstColumnId = computed(() => tableColumns.value[0]?.id);
const firstColumnOptions = computed(() =>
  firstColumnId.value ? getColumnOptions(firstColumnId.value) : undefined
);
</script>

<template>
  <div class="emx2-data-view">
    <div class="flex gap-6">
      <!-- Filter Sidebar -->
      <div
        v-if="showFilters && filterPosition === 'sidebar'"
        class="w-80 flex-shrink-0"
      >
        <FilterSidebar
          v-if="filterableColumnsComputed.length"
          v-model:filter-states="filterStates"
          :columns="filterableColumnsComputed"
        />
      </div>

      <!-- Content Area -->
      <div class="flex-1 min-w-0">
        <InputSearch
          v-if="showSearch"
          :id="searchInputId"
          v-model="searchTerms"
          placeholder="Search..."
          size="small"
          class="mb-4"
        />

        <LoadingContent
          :id="`emx2-data-view-${schemaId}-${tableId}`"
          :status="status"
          loading-text="Loading..."
          :error-text="errorText"
          :show-slot-on-error="false"
        >
          <!-- Table layout -->
          <div v-if="rows.length && layout === 'table'" class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="border-b border-gray-200">
                  <th
                    v-for="col in tableColumns"
                    :key="col.id"
                    class="px-3 py-2 text-body-sm font-semibold text-gray-600"
                  >
                    {{ col.label || col.id }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(row, index) in rows"
                  :key="index"
                  class="border-b border-gray-100 hover:bg-gray-50"
                >
                  <td
                    v-for="(col, colIndex) in tableColumns"
                    :key="col.id"
                    class="px-3 py-2 text-body-base"
                  >
                    <NuxtLink
                      v-if="colIndex === 0 && firstColumnOptions?.href"
                      :to="firstColumnOptions.href(row)"
                      class="text-link hover:underline"
                    >
                      {{ getCellValue(row, col.id) }}
                    </NuxtLink>
                    <span
                      v-else-if="colIndex === 0 && firstColumnOptions?.onClick"
                      class="text-link hover:underline cursor-pointer"
                      @click="firstColumnOptions.onClick!(row)"
                    >
                      {{ getCellValue(row, col.id) }}
                    </span>
                    <span v-else>{{ getCellValue(row, col.id) }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Card layout -->
          <CardList v-else-if="rows.length && layout === 'cards'">
            <CardListItem v-for="(row, index) in rows" :key="index">
              <slot name="card" :row="row" :label="getLabel(row)">
                <span>{{ getLabel(row) }}</span>
              </slot>
            </CardListItem>
          </CardList>

          <!-- List layout (default) -->
          <ul
            v-else-if="rows.length"
            class="grid gap-1 pl-4 list-disc list-outside"
          >
            <li v-for="(row, index) in rows" :key="index">
              <slot :row="row" :label="getLabel(row)">
                <span>{{ getLabel(row) }}</span>
              </slot>
            </li>
          </ul>
          <p v-else class="text-gray-400 dark:text-gray-500 italic">No items</p>

          <InlinePagination
            v-if="showPagination"
            :current-page="page"
            :total-pages="totalPages"
            @update:page="page = $event"
          />
        </LoadingContent>
      </div>
    </div>
  </div>
</template>

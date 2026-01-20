<script setup lang="ts">
import { ref, computed, watch, useId, type Component } from "vue";
import type {
  IColumn,
  IRow,
  IRefColumn,
  IDisplayConfig,
} from "../../../../metadata-utils/src/types";
import { useTableData } from "../../composables/useTableData";
import { rowToString } from "../../utils/rowToString";
import InputSearch from "../input/Search.vue";
import InlinePagination from "./InlinePagination.vue";
import LoadingContent from "../LoadingContent.vue";
import CardList from "../CardList.vue";
import CardListItem from "../CardListItem.vue";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    tableId: string;
    filter?: object;
    viewColumns?: string[];
    showSearch?: boolean;
    pagingLimit?: number;
    refLabel?: string;
    clickAction?: (col: IColumn, row: IRow) => void;
    getHref?: (col: IColumn, row: IRow) => string;
    displayConfig?: IDisplayConfig;
    componentProps?: Record<string, unknown>;
  }>(),
  {
    showSearch: true,
    pagingLimit: 10,
  }
);

const searchInputId = useId();
const page = ref(1);
const searchTerms = ref("");

// wrap filter in computed so useTableData can watch for changes
const filterComputed = computed(() => props.filter);

const {
  metadata,
  rows,
  count,
  status,
  totalPages,
  showPagination,
  errorMessage,
} = useTableData(props.schemaId, props.tableId, {
  pageSize: props.pagingLimit,
  page,
  filter: filterComputed,
  searchTerms,
});

const errorText = computed(
  () =>
    errorMessage.value ||
    (status.value === "error" ? "Failed to load data" : undefined)
);

// construct a ref column for label rendering and slot
const refColumn = computed<IRefColumn | undefined>(() => {
  if (!metadata.value) return undefined;
  const keyCol = metadata.value.columns?.find((c) => c.key === 1);
  return {
    id: metadata.value.id,
    label: metadata.value.label,
    columnType: "REF",
    refTableId: metadata.value.id,
    refSchemaId: metadata.value.schemaId,
    refLabel: props.refLabel || keyCol?.refLabel || "",
    refLabelDefault: keyCol?.refLabelDefault || "${name}",
    refLinkId: keyCol?.id || "name",
  };
});

// check if displayConfig.component is "table" string
const isTableMode = computed(() => props.displayConfig?.component === "table");

// check if displayConfig.component is a Vue component (not a string)
const isCardMode = computed(
  () =>
    props.displayConfig?.component &&
    typeof props.displayConfig.component !== "string"
);

// get visible columns for table mode
const visibleColumns = computed(() => {
  const cols = props.displayConfig?.visibleColumns || props.viewColumns || [];
  if (!metadata.value?.columns) return cols;
  // filter to only columns that exist in metadata
  return cols.filter((colId) =>
    metadata.value!.columns!.some((c) => c.id === colId)
  );
});

// get column metadata for table headers
const tableColumns = computed<IColumn[]>(() => {
  if (!metadata.value?.columns) return [];
  return visibleColumns.value
    .map((colId) => metadata.value!.columns!.find((c) => c.id === colId))
    .filter((col): col is IColumn => col !== undefined);
});

// get cell value for a row/column
function getCellValue(row: IRow, colId: string): string {
  const val = row[colId];
  if (val === null || val === undefined) return "";
  if (Array.isArray(val)) {
    // handle arrays (ref_array, ontology_array)
    return val
      .map((v) => (typeof v === "object" ? v.label || v.name || "" : String(v)))
      .join(", ");
  }
  if (typeof val === "object") {
    // handle ref/ontology objects - cast to access properties
    const obj = val as Record<string, unknown>;
    return String(obj.label || obj.name || JSON.stringify(val));
  }
  return String(val);
}

watch(searchTerms, () => {
  page.value = 1;
});

function getLabel(row: IRow): string {
  const template =
    refColumn.value?.refLabel || refColumn.value?.refLabelDefault;
  return template ? rowToString(row, template) || "" : "";
}
</script>

<template>
  <div class="emx2-list-view">
    <InputSearch
      v-if="showSearch"
      :id="searchInputId"
      v-model="searchTerms"
      placeholder="Search..."
      size="small"
      class="mb-4"
    />

    <LoadingContent
      :id="`emx2-list-view-${schemaId}-${tableId}`"
      :status="status"
      loading-text="Loading..."
      :error-text="errorText"
      :show-slot-on-error="false"
    >
      <!-- Table layout when displayConfig.component === "table" -->
      <div v-if="rows.length && isTableMode" class="overflow-x-auto">
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
                  v-if="colIndex === 0 && props.getHref && refColumn"
                  :to="props.getHref(refColumn, row)"
                  class="text-link hover:underline"
                >
                  {{ getCellValue(row, col.id) }}
                </NuxtLink>
                <span v-else>{{ getCellValue(row, col.id) }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <!-- Card layout when displayConfig.component is a Vue component -->
      <CardList v-else-if="rows.length && isCardMode">
        <CardListItem v-for="(row, index) in rows" :key="index">
          <component
            :is="displayConfig!.component"
            :data="row"
            v-bind="componentProps"
          />
        </CardListItem>
      </CardList>
      <!-- Default list layout -->
      <ul
        v-else-if="rows.length"
        class="grid gap-1 pl-4 list-disc list-outside"
      >
        <li v-for="(row, index) in rows" :key="index">
          <slot :row="row" :column="refColumn" :label="getLabel(row)">
            <NuxtLink
              v-if="props.getHref && refColumn"
              :to="props.getHref(refColumn, row)"
              class="underline text-link"
            >
              {{ getLabel(row) }}
            </NuxtLink>
            <span
              v-else-if="props.clickAction && refColumn"
              class="underline hover:cursor-pointer text-link"
              @click="props.clickAction(refColumn, row)"
            >
              {{ getLabel(row) }}
            </span>
            <span v-else>{{ getLabel(row) }}</span>
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
</template>

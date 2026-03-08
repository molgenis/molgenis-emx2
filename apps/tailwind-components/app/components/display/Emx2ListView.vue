<script setup lang="ts">
import { ref, computed, watch, useId, type Component } from "vue";
import type {
  IColumn,
  IRow,
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
    config?: IDisplayConfig;
  }>(),
  {}
);

const showSearch = computed(() => props.config?.showSearch !== false);
const pagingLimit = computed(() => props.config?.pageSize || 10);
const refLabel = computed(() => props.config?.rowLabel);
const clickAction = computed(() => props.config?.clickAction);
const getHref = computed(() => props.config?.getHref);
const displayConfig = computed(() => props.config);
const viewColumns = computed(() => props.config?.visibleColumns);

const searchInputId = useId();
const page = ref(1);
const searchTerms = ref("");

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
  pageSize: pagingLimit.value,
  page,
  filter: filterComputed,
  searchTerms,
});

const errorText = computed(
  () =>
    errorMessage.value ||
    (status.value === "error" ? "Failed to load data" : undefined)
);

const refColumn = computed<IColumn | undefined>(() => {
  if (!metadata.value) return undefined;
  const keyCol = metadata.value.columns?.find((c) => c.key === 1);
  return {
    id: metadata.value.id,
    label: metadata.value.label,
    columnType: "REF",
    refTableId: metadata.value.id,
    refSchemaId: metadata.value.schemaId,
    refLabel: refLabel.value || keyCol?.refLabel || "",
    refLabelDefault: keyCol?.refLabelDefault || "${name}",
    refLinkId: keyCol?.id || "name",
  };
});

const isTableMode = computed(() => displayConfig.value?.component === "table");

const isCardMode = computed(
  () =>
    displayConfig.value?.component &&
    typeof displayConfig.value.component !== "string"
);

const visibleColumns = computed(() => {
  const cols = displayConfig.value?.visibleColumns || viewColumns.value || [];
  if (!metadata.value?.columns) return cols;
  const showMg = displayConfig.value?.showMgColumns;
  const availableColumns = showMg
    ? metadata.value.columns
    : metadata.value.columns.filter((c) => !c.id.startsWith("mg_"));
  return cols.filter((colId) => availableColumns.some((c) => c.id === colId));
});

const tableColumns = computed<IColumn[]>(() => {
  if (!metadata.value?.columns) return [];
  const showMg = displayConfig.value?.showMgColumns;
  const availableColumns = showMg
    ? metadata.value.columns
    : metadata.value.columns.filter((c) => !c.id.startsWith("mg_"));
  return visibleColumns.value
    .map((colId) => availableColumns.find((c) => c.id === colId))
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
                  v-if="colIndex === 0 && getHref && refColumn"
                  :to="getHref(refColumn, row)"
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
      <div
        v-else-if="rows.length && isCardMode"
        class="bg-content rounded-t-3px rounded-b-50px shadow-primary"
      >
        <CardList>
          <CardListItem v-for="(row, index) in rows" :key="index">
            <component :is="displayConfig!.component" :data="row" />
          </CardListItem>
        </CardList>
      </div>
      <!-- Default list layout -->
      <ul
        v-else-if="rows.length"
        class="grid gap-1 pl-4 list-disc list-outside"
      >
        <li v-for="(row, index) in rows" :key="index">
          <slot :row="row" :column="refColumn" :label="getLabel(row)">
            <NuxtLink
              v-if="getHref && refColumn"
              :to="getHref(refColumn, row)"
              class="underline text-link"
            >
              {{ getLabel(row) }}
            </NuxtLink>
            <span
              v-else-if="clickAction && refColumn"
              class="underline hover:cursor-pointer text-link"
              @click="clickAction(refColumn, row)"
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

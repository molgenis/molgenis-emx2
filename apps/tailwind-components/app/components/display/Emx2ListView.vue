<script setup lang="ts">
import { ref, computed, useId } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IColumnDisplay } from "../../../types/types";
import { useTableData } from "../../composables/useTableData";
import { getRowLabel } from "../../utils/displayUtils";
import InputSearch from "../input/Search.vue";
import LoadingContent from "../LoadingContent.vue";
import ListView from "./ListView.vue";

const props = defineProps<{
  schemaId: string;
  tableId: string;
  filter?: object;
  column?: IColumnDisplay;
}>();

const searchInputId = useId();
const page = ref(1);
const searchTerms = ref("");

const layout = computed((): "TABLE" | "CARDS" | "LIST" => {
  const override = props.column?.listConfig?.layout;
  const raw = override || props.column?.display || "TABLE";
  if (raw === "CARDS" || raw === "LIST") return raw;
  return "TABLE";
});

const showSearch = computed(() => layout.value === "TABLE");

const pageSize = computed(() => {
  return props.column?.listConfig?.pageSize || 10;
});

const { metadata, rows, status, totalPages, showPagination, errorMessage } =
  useTableData(props.schemaId, props.tableId, {
    pageSize: pageSize.value,
    page,
    filter: computed(() => props.filter),
    searchTerms,
  });

const refTableColumns = computed(() => metadata.value?.columns || []);

const visibleColumns = computed(() => {
  const override = props.column?.listConfig?.visibleColumns;
  if (override?.length) return override;

  if (layout.value === "TABLE") return undefined;

  const roleCols = refTableColumns.value.filter((c) => c.role).map((c) => c.id);
  if (roleCols.length > 0) return roleCols;

  const keyCols = refTableColumns.value
    .filter((c) => c.key && c.key > 0)
    .map((c) => c.id);
  const otherCols = refTableColumns.value
    .filter(
      (c) =>
        (!c.key || c.key === 0) &&
        c.columnType !== "HEADING" &&
        c.columnType !== "SECTION" &&
        !c.id.startsWith("mg_")
    )
    .slice(0, 5 - keyCols.length)
    .map((c) => c.id);
  return [...keyCols, ...otherCols];
});

const hideColumns = computed(() => {
  const override = props.column?.listConfig?.hideColumns;
  if (override?.length) return override;
  return props.column?.refBackId ? [props.column.refBackId] : [];
});

const rowLabel = computed(() => {
  const override = props.column?.listConfig?.rowLabel;
  return override || props.column?.refLabelDefault || undefined;
});

function getHref(_col: IColumn, row: Record<string, any>): string {
  return `/${props.schemaId}/${props.tableId}/${encodeURIComponent(
    getRowLabel(row, rowLabel.value)
  )}`;
}

const errorText = computed(
  () =>
    errorMessage.value ||
    (status.value === "error" ? "Failed to load data" : undefined)
);
</script>

<template>
  <div>
    <InputSearch
      v-if="showSearch && showPagination"
      :id="searchInputId"
      v-model="searchTerms"
      placeholder="Search..."
      size="small"
      class="mb-4"
    />
    <LoadingContent
      :id="`list-${schemaId}-${tableId}`"
      :status="status"
      loading-text="Loading..."
      :error-text="errorText"
      :show-slot-on-error="false"
    >
      <ListView
        :rows="rows"
        :columns="refTableColumns"
        :layout="layout"
        :visible-columns="visibleColumns"
        :hide-columns="hideColumns"
        :row-label="rowLabel"
        :get-href="getHref"
        :total-pages="totalPages"
        :current-page="page"
        :show-pagination="showPagination"
        :schema-id="schemaId"
        :table-id="tableId"
        @update:page="page = $event"
      />
    </LoadingContent>
  </div>
</template>

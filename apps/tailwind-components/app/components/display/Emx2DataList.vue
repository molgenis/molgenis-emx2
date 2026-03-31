<script setup lang="ts">
import { ref, computed, useId } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IColumnDisplay } from "../../../types/types";
import { useTableData } from "../../composables/useTableData";
import { getRowLabel, getListColumns } from "../../utils/displayUtils";
import InputSearch from "../input/Search.vue";
import LoadingContent from "../LoadingContent.vue";
import InlinePagination from "./InlinePagination.vue";
import ListView from "./DataList.vue";

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
  const raw = props.column?.display || "TABLE";
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

const listColumns = computed(() =>
  getListColumns(metadata.value?.columns || [], {
    layout: layout.value,
    hideColumns: props.column?.refBackId ? [props.column.refBackId] : [],
    rows: rows.value,
  })
);

const rowLabel = computed(() => {
  const override = props.column?.listConfig?.rowLabel;
  return override || props.column?.refLabelDefault || undefined;
});

function getHref(row: Record<string, any>): string {
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
        :columns="listColumns"
        :layout="layout"
        :row-label="rowLabel"
        :get-href="getHref"
        :schema-id="schemaId"
        :table-id="tableId"
      />
    </LoadingContent>
    <InlinePagination
      v-if="showPagination"
      :current-page="page"
      :total-pages="totalPages"
      class="mt-4"
      @update:page="page = $event"
    />
  </div>
</template>

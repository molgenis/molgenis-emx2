<script setup lang="ts">
import { ref, computed, useId } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import { useTableData } from "../../composables/useTableData";
import { getListColumns, getRowLabel } from "../../utils/displayUtils";
import InputSearch from "../input/Search.vue";
import LoadingContent from "../LoadingContent.vue";
import InlinePagination from "./InlinePagination.vue";
import DataTable from "./DataTable.vue";
import DataCards from "./DataCards.vue";
import DataLinks from "./DataLinks.vue";

const props = withDefaults(
  defineProps<{
    schemaId?: string;
    tableId?: string;
    filter?: object;
    rows?: Record<string, any>[];
    columns?: IColumn[];
    layout?: "TABLE" | "CARDS" | "LIST" | "LINKS";
    pageSize?: number;
    hideColumns?: string[];
    rowLabelTemplate?: string;
  }>(),
  {
    layout: "TABLE",
    pageSize: 10,
  }
);

const isSmartMode = computed(
  () => !!(props.schemaId && props.tableId && !props.rows)
);

const searchInputId = useId();
const page = ref(1);
const searchTerms = ref("");

const showSearch = computed(
  () => isSmartMode.value && props.layout === "TABLE"
);

const {
  metadata,
  rows: fetchedRows,
  status,
  totalPages,
  showPagination,
  errorMessage,
} = useTableData(props.schemaId || "", props.tableId || "", {
  pageSize: props.pageSize,
  page,
  filter: computed(() => props.filter),
  searchTerms,
});

const smartListColumns = computed(() =>
  getListColumns(metadata.value?.columns || [], {
    layout: props.layout as "TABLE" | "CARDS" | "LIST" | undefined,
    hideColumns: props.hideColumns,
    rows: fetchedRows.value,
  })
);

const errorText = computed(
  () =>
    errorMessage.value ||
    (status.value === "error" ? "Failed to load data" : undefined)
);

const dumbColumns = computed(() =>
  getListColumns(props.columns || [], {
    rows: props.rows || [],
  })
);

const effectiveRows = computed(() =>
  isSmartMode.value ? fetchedRows.value : props.rows || []
);

const effectiveColumns = computed(() =>
  isSmartMode.value ? smartListColumns.value : dumbColumns.value
);

function buildRowHref(row: Record<string, any>): string | undefined {
  if (!props.schemaId || !props.tableId) return undefined;
  return `/${props.schemaId}/${props.tableId}/${encodeURIComponent(
    getRowLabel(row, props.rowLabelTemplate)
  )}`;
}
</script>

<template>
  <div v-if="isSmartMode">
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
      <div>
        <DataTable
          v-if="layout === 'TABLE'"
          :columns="effectiveColumns"
          :rows="effectiveRows"
          :schema-id="schemaId"
          :table-id="tableId"
        />
        <DataCards
          v-else-if="layout === 'CARDS'"
          :rows="effectiveRows"
          :columns="effectiveColumns"
          :grid-columns="2"
          :row-label-template="rowLabelTemplate"
          :get-href="buildRowHref"
        />
        <DataCards
          v-else-if="layout === 'LIST'"
          :rows="effectiveRows"
          :columns="effectiveColumns"
          :grid-columns="1"
          :row-label-template="rowLabelTemplate"
          :get-href="buildRowHref"
        />
        <DataLinks
          v-else-if="layout === 'LINKS'"
          :rows="effectiveRows"
          :row-label-template="rowLabelTemplate"
          :get-href="buildRowHref"
        />
      </div>
    </LoadingContent>
    <InlinePagination
      v-if="showPagination"
      :current-page="page"
      :total-pages="totalPages"
      class="mt-4"
      @update:page="page = $event"
    />
  </div>
  <div v-else>
    <DataTable
      v-if="layout === 'TABLE'"
      :columns="effectiveColumns"
      :rows="effectiveRows"
      :schema-id="schemaId"
      :table-id="tableId"
    />
    <DataCards
      v-else-if="layout === 'CARDS'"
      :rows="effectiveRows"
      :columns="effectiveColumns"
      :grid-columns="2"
      :row-label-template="rowLabelTemplate"
      :get-href="buildRowHref"
    />
    <DataCards
      v-else-if="layout === 'LIST'"
      :rows="effectiveRows"
      :columns="effectiveColumns"
      :grid-columns="1"
      :row-label-template="rowLabelTemplate"
      :get-href="buildRowHref"
    />
    <DataLinks
      v-else-if="layout === 'LINKS'"
      :rows="effectiveRows"
      :row-label-template="rowLabelTemplate"
      :get-href="buildRowHref"
    />
  </div>
</template>

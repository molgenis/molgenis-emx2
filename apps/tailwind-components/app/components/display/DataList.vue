<script setup lang="ts">
import { ref, computed, watch, useId } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import { useTableData } from "../../composables/useTableData";
import { getListColumns } from "../../utils/displayUtils";
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
    totalCount?: number;
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

const hasSwitchedToSmart = ref(false);

const hasTruncatedData = computed(
  () =>
    props.totalCount !== undefined &&
    props.rows !== undefined &&
    props.totalCount > props.rows.length
);

const isPrefetchMode = computed(
  () => !!props.rows && !hasSwitchedToSmart.value
);

const effectiveSmartMode = computed(
  () =>
    !!(props.schemaId && props.tableId) &&
    (!isPrefetchMode.value || !props.rows)
);

const effectiveSchemaId = computed(() =>
  effectiveSmartMode.value ? props.schemaId ?? "" : ""
);

const effectiveTableId = computed(() =>
  effectiveSmartMode.value ? props.tableId ?? "" : ""
);

const searchInputId = useId();
const page = ref(1);
const searchTerms = ref("");

const showSearch = computed(
  () =>
    (effectiveSmartMode.value || hasTruncatedData.value) &&
    props.layout === "TABLE"
);

watch(searchTerms, (val) => {
  if (val && hasTruncatedData.value && !hasSwitchedToSmart.value) {
    hasSwitchedToSmart.value = true;
    page.value = 1;
  }
});

watch(page, (val) => {
  if (!hasSwitchedToSmart.value && props.rows) {
    const prefetchedPages = Math.ceil(props.rows.length / props.pageSize);
    if (val > prefetchedPages && hasTruncatedData.value) {
      hasSwitchedToSmart.value = true;
    }
  }
});

const {
  metadata,
  rows: fetchedRows,
  status,
  totalPages: fetchedTotalPages,
  showPagination: fetchedShowPagination,
  errorMessage,
} = useTableData(effectiveSchemaId, effectiveTableId, {
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
    layout: props.layout as "TABLE" | "CARDS" | "LIST" | undefined,
    hideColumns: props.hideColumns,
    rows: props.rows || [],
  })
);

const dumbDisplayCount = computed(
  () => props.totalCount ?? props.rows?.length ?? 0
);

const dumbTotalPages = computed(() =>
  Math.ceil(dumbDisplayCount.value / props.pageSize)
);

const dumbShowPagination = computed(
  () => dumbDisplayCount.value > props.pageSize
);

const effectiveRows = computed(() =>
  effectiveSmartMode.value ? fetchedRows.value : props.rows || []
);

const effectiveColumns = computed(() =>
  effectiveSmartMode.value ? smartListColumns.value : dumbColumns.value
);

const effectiveTotalPages = computed(() =>
  effectiveSmartMode.value ? fetchedTotalPages.value : dumbTotalPages.value
);

const effectiveShowPagination = computed(() =>
  effectiveSmartMode.value
    ? fetchedShowPagination.value
    : dumbShowPagination.value
);
</script>

<template>
  <div v-if="effectiveSmartMode">
    <InputSearch
      v-if="showSearch && effectiveShowPagination"
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
          :schema-id="schemaId"
          :table-id="tableId"
        />
        <DataCards
          v-else-if="layout === 'LIST'"
          :rows="effectiveRows"
          :columns="effectiveColumns"
          :grid-columns="1"
          :row-label-template="rowLabelTemplate"
          :schema-id="schemaId"
          :table-id="tableId"
        />
        <DataLinks
          v-else-if="layout === 'LINKS'"
          :rows="effectiveRows"
          :row-label-template="rowLabelTemplate"
          :schema-id="schemaId"
          :table-id="tableId"
        />
      </div>
    </LoadingContent>
    <InlinePagination
      v-if="effectiveShowPagination"
      :current-page="page"
      :total-pages="effectiveTotalPages"
      class="mt-4"
      @update:page="page = $event"
    />
  </div>
  <div v-else>
    <InputSearch
      v-if="showSearch && effectiveShowPagination"
      :id="searchInputId"
      v-model="searchTerms"
      placeholder="Search..."
      size="small"
      class="mb-4"
    />
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
      :schema-id="schemaId"
      :table-id="tableId"
    />
    <DataCards
      v-else-if="layout === 'LIST'"
      :rows="effectiveRows"
      :columns="effectiveColumns"
      :grid-columns="1"
      :row-label-template="rowLabelTemplate"
      :schema-id="schemaId"
      :table-id="tableId"
    />
    <DataLinks
      v-else-if="layout === 'LINKS'"
      :rows="effectiveRows"
      :row-label-template="rowLabelTemplate"
      :schema-id="schemaId"
      :table-id="tableId"
    />
    <InlinePagination
      v-if="effectiveShowPagination"
      :current-page="page"
      :total-pages="effectiveTotalPages"
      class="mt-4"
      @update:page="page = $event"
    />
  </div>
</template>

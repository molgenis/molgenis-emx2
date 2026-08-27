<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";
import type { DisplayConfig } from "../../types/display";
import { resolveDisplay } from "../../utils/displayUtils";
import fetchTableData from "../../composables/fetchTableData";
import fetchTableMetadata from "../../composables/fetchTableMetadata";
import Pagination from "../Pagination.vue";
import DataTable from "./DataTable.vue";
import DataCards from "./DataCards.vue";
import DataLinks from "./DataLinks.vue";

const props = withDefaults(
  defineProps<{
    display?: DisplayConfig;
    rows?: IRow[];
    columns?: IColumn[];
    schemaId?: string;
    tableId?: string;
    filter?: Record<string, unknown>;
    pageSize?: number;
    linkTo?: (row: IRow) => string;
  }>(),
  {
    pageSize: 10,
  }
);

const isFetchMode = computed(
  () => !props.rows && !!props.schemaId && !!props.tableId
);

const currentPage = ref(1);
const fetchedColumns = ref<IColumn[]>([]);
const fetchedRows = ref<IRow[]>([]);
const fetchedCount = ref(0);

// A newer request racing past a slower one must win. Capture a request id
// before the await and drop the write if a later request already landed.
let latestRequestId = 0;

// Any input that changes what the result set IS goes back to page 1.
// Registered before the data-fetch watcher below so, when both fire in the
// same flush (e.g. filter changes), this one runs first.
watch(
  () => [
    props.schemaId,
    props.tableId,
    props.filter,
    props.pageSize,
    props.rows,
  ],
  () => {
    currentPage.value = 1;
  }
);

watch(
  () => [props.schemaId, props.tableId],
  async () => {
    if (!isFetchMode.value || !props.schemaId || !props.tableId) {
      return;
    }
    const metadata = await fetchTableMetadata(props.schemaId, props.tableId);
    fetchedColumns.value = metadata.columns;
  },
  { immediate: true }
);

watch(
  () =>
    [
      props.schemaId,
      props.tableId,
      props.filter,
      props.pageSize,
      currentPage.value,
    ] as const,
  async ([schemaId, tableId, filter, pageSize, page]) => {
    if (!isFetchMode.value || !schemaId || !tableId) {
      return;
    }
    const requestId = ++latestRequestId;
    const offset = (page - 1) * pageSize;
    const response = await fetchTableData(schemaId, tableId, {
      limit: pageSize,
      offset,
      filter,
      expandLevel: 1,
    });
    if (requestId !== latestRequestId) {
      return;
    }
    fetchedRows.value = response.rows;
    fetchedCount.value = response.count;
  },
  { immediate: true }
);

const columns = computed(() =>
  isFetchMode.value ? fetchedColumns.value : props.columns ?? []
);

const resolved = computed(() => resolveDisplay(columns.value, props.display));

const totalRows = computed(() =>
  isFetchMode.value ? fetchedCount.value : props.rows?.length ?? 0
);

const totalPages = computed(() =>
  Math.max(1, Math.ceil(totalRows.value / props.pageSize))
);

const pagedRows = computed(() => {
  if (isFetchMode.value) {
    return fetchedRows.value;
  }
  const start = (currentPage.value - 1) * props.pageSize;
  return (props.rows ?? []).slice(start, start + props.pageSize);
});

const rangeText = computed(() => {
  if (totalRows.value === 0) {
    return "0";
  }
  const start = (currentPage.value - 1) * props.pageSize + 1;
  const end = Math.min(currentPage.value * props.pageSize, totalRows.value);
  return `${start}-${end}`;
});

function onPageUpdate(page: number) {
  currentPage.value = page;
}
</script>

<template>
  <div>
    <DataTable
      v-if="resolved.layout === 'TABLE'"
      :rows="pagedRows"
      :resolved="resolved"
      :link-to="linkTo"
    />
    <DataCards
      v-else-if="resolved.layout === 'CARDS'"
      :rows="pagedRows"
      :resolved="resolved"
      :column-count="2"
      :link-to="linkTo"
    />
    <DataCards
      v-else-if="resolved.layout === 'LIST'"
      :rows="pagedRows"
      :resolved="resolved"
      :column-count="1"
      :link-to="linkTo"
    />
    <DataLinks
      v-else
      :rows="pagedRows"
      :resolved="resolved"
      :link-to="linkTo"
    />

    <p class="text-center text-pagination">
      {{ rangeText }} of {{ totalRows }}
    </p>
    <Pagination
      :current-page="currentPage"
      :total-pages="totalPages"
      :show-page-selector="false"
      @update="onPageUpdate"
    />
  </div>
</template>

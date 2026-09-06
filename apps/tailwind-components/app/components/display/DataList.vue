<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";
import type { DisplayConfig, Layout } from "../../types/display";
import { resolveDisplay } from "../../utils/displayUtils";
import fetchTableData from "../../composables/fetchTableData";
import fetchTableMetadata from "../../composables/fetchTableMetadata";
import Pagination from "../Pagination.vue";
import ShowMore from "../ShowMore.vue";
import DataTable from "./DataTable.vue";
import DataCards from "./DataCards.vue";
import DataRows from "./DataRows.vue";
import DataLinks from "./DataLinks.vue";
import DataBullets from "./DataBullets.vue";

const props = withDefaults(
  defineProps<{
    displayConfig?: DisplayConfig;
    rows?: IRow[];
    columns?: IColumn[];
    schemaId?: string;
    tableId?: string;
    // Watched by identity: pass a computed, not a literal, or every parent
    // render refetches and resets to page 1.
    filter?: Record<string, unknown>;
    pageSize?: number;
    linkTo?: (row: IRow) => string;
    hideListSeparator?: boolean;
    maxLines?: number;
    renderLimit?: number;
    truncate?: boolean;
    hideEmpty?: boolean;
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
let latestMetadataRequestId = 0;

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
    const requestId = ++latestMetadataRequestId;
    const metadata = await fetchTableMetadata(props.schemaId, props.tableId);
    if (requestId !== latestMetadataRequestId) {
      return;
    }
    fetchedColumns.value = metadata.columns;
  },
  { immediate: true }
);

const columns = computed(() =>
  isFetchMode.value ? fetchedColumns.value : props.columns ?? []
);

const resolvedDisplay = computed(() =>
  resolveDisplay(columns.value, props.displayConfig)
);

// The compiler proves every Layout is handled here; a value missing from
// this switch is a typecheck failure, not a silent wrong render.
function assertNever(layout: never): never {
  throw new Error(`DataList: unhandled layout "${layout}"`);
}

// TABLE, CARDS and LIST page through Pagination. LINKS and BULLETS grow on
// demand through their own load-more instead, batchSize rows at a time: a
// bulleted list takes one line each, a comma-separated run is compact.
const layoutView = computed(() => {
  const layout: Layout = resolvedDisplay.value.layout;
  switch (layout) {
    case "TABLE":
      return { component: DataTable, paginated: true, batchSize: undefined };
    case "CARDS":
      return { component: DataCards, paginated: true, batchSize: undefined };
    case "LIST":
      return { component: DataRows, paginated: true, batchSize: undefined };
    case "LINKS":
      return { component: DataLinks, paginated: false, batchSize: 50 };
    case "BULLETS":
      return { component: DataBullets, paginated: false, batchSize: 20 };
    default:
      return assertNever(layout);
  }
});

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
    const paginated = layoutView.value.paginated;
    // pageSize belongs to the paging layouts; a layout that loads on demand
    // has its own batch size and does not inherit a pager's.
    const limit = paginated ? pageSize : layoutView.value.batchSize;
    const offset = paginated ? (page - 1) * pageSize : 0;
    const response = await fetchTableData(schemaId, tableId, {
      limit,
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

// LINKS and BULLETS never move currentPage (they render no Pagination), so
// this watcher only re-fires on schemaId/tableId/filter/pageSize for them,
// which is exactly a fresh first batch. loadMore is the one path that
// appends instead of replacing, for the "show more" control's own click.
const remaining = computed(() =>
  Math.max(fetchedCount.value - fetchedRows.value.length, 0)
);
const hasMore = computed(() => isFetchMode.value && remaining.value > 0);

async function loadMore() {
  if (!isFetchMode.value || !props.schemaId || !props.tableId) {
    return;
  }
  const batchSize = layoutView.value.batchSize;
  if (batchSize === undefined) {
    return;
  }
  const requestId = ++latestRequestId;
  const offset = fetchedRows.value.length;
  const response = await fetchTableData(props.schemaId, props.tableId, {
    limit: batchSize,
    offset,
    filter: props.filter,
    expandLevel: 1,
  });
  if (requestId !== latestRequestId) {
    return;
  }
  fetchedRows.value = [...fetchedRows.value, ...response.rows];
  fetchedCount.value = response.count;
}

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
  if (!layoutView.value.paginated) {
    return props.rows ?? [];
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
  return `${start} - ${end}`;
});

function onPageUpdate(page: number) {
  currentPage.value = page;
}
</script>

<template>
  <div>
    <component
      :is="layoutView.component"
      :rows="pagedRows"
      :columns="columns"
      :display-config="displayConfig"
      :link-to="linkTo"
      :hide-list-separator="hideListSeparator"
      :max-lines="maxLines"
      :render-limit="renderLimit"
      :truncate="truncate"
      :hide-empty="hideEmpty"
    />

    <!-- pt-5: CARDS and LIST end on a card border with a -mb-[1px] overlap,
    so the pager needs a gap above it or it lands right on that line. -->
    <Pagination
      v-if="layoutView.paginated"
      :current-page="currentPage"
      :total-pages="totalPages"
      class="pt-5 pb-[30px]"
      @update="onPageUpdate"
    />
    <ShowMore v-else-if="isFetchMode" :has-more="hasMore" @show-more="loadMore">
      <template #more>Load more ({{ remaining }})</template>
    </ShowMore>

    <p v-if="layoutView.paginated" class="text-center text-pagination">
      {{ rangeText }} of {{ totalRows }}
    </p>
  </div>
</template>

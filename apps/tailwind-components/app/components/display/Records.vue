<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";
import type { DisplayConfig, Layout } from "../../types/display";
import { resolveDisplay } from "../../utils/displayUtils";
import fetchTableData from "../../composables/fetchTableData";
import fetchTableMetadata from "../../composables/fetchTableMetadata";
import Pagination from "../Pagination.vue";
import ShowMore from "../ShowMore.vue";
import RecordsTable from "./records/Table.vue";
import RecordsCards from "./records/Cards.vue";
import RecordsList from "./records/List.vue";
import RecordsLinks from "./records/Links.vue";
import RecordsBullets from "./records/Bullets.vue";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    tableId: string;
    displayConfig?: DisplayConfig;
    // Watched by identity: pass a computed, not a literal, or every parent
    // render refetches and resets to page 1.
    filter?: Record<string, unknown>;
    pageSize?: number;
    linkTo?: (row: IRow) => string;
    hideEmpty?: boolean;
  }>(),
  {
    pageSize: 10,
  }
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
  () => [props.schemaId, props.tableId, props.filter, props.pageSize],
  () => {
    currentPage.value = 1;
  }
);

watch(
  () => [props.schemaId, props.tableId],
  async () => {
    const requestId = ++latestMetadataRequestId;
    const metadata = await fetchTableMetadata(props.schemaId, props.tableId);
    if (requestId !== latestMetadataRequestId) {
      return;
    }
    fetchedColumns.value = metadata.columns;
  },
  { immediate: true }
);

const resolvedDisplay = computed(() =>
  resolveDisplay(fetchedColumns.value, props.displayConfig)
);

// The compiler proves every Layout is handled here; a value missing from
// this switch is a typecheck failure, not a silent wrong render.
function assertNever(layout: never): never {
  throw new Error(`Records: unhandled layout "${layout}"`);
}

// TABLE, CARDS and LIST page through Pagination. LINKS and BULLETS grow on
// demand through their own load-more instead, batchSize rows at a time: a
// bulleted list takes one line each, a comma-separated run is compact.
const layoutMeta = computed(() => {
  const layout: Layout = resolvedDisplay.value.layout;
  switch (layout) {
    case "TABLE":
      return { paginated: true, batchSize: undefined };
    case "CARDS":
      return { paginated: true, batchSize: undefined };
    case "LIST":
      return { paginated: true, batchSize: undefined };
    case "LINKS":
      return { paginated: false, batchSize: 50 };
    case "BULLETS":
      return { paginated: false, batchSize: 20 };
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
    const requestId = ++latestRequestId;
    const paginated = layoutMeta.value.paginated;
    // pageSize belongs to the paging layouts; a layout that loads on demand
    // has its own batch size and does not inherit a pager's.
    const limit = paginated ? pageSize : layoutMeta.value.batchSize;
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
const hasMore = computed(() => remaining.value > 0);

async function loadMore() {
  const batchSize = layoutMeta.value.batchSize;
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

const totalPages = computed(() =>
  Math.max(1, Math.ceil(fetchedCount.value / props.pageSize))
);

const rangeText = computed(() => {
  if (fetchedCount.value === 0) {
    return "0";
  }
  const start = (currentPage.value - 1) * props.pageSize + 1;
  const end = Math.min(currentPage.value * props.pageSize, fetchedCount.value);
  return `${start} - ${end}`;
});

function onPageUpdate(page: number) {
  currentPage.value = page;
}
</script>

<template>
  <div>
    <RecordsTable
      v-if="resolvedDisplay.layout === 'TABLE'"
      :rows="fetchedRows"
      :title-template="resolvedDisplay.titleTemplate"
      :columns="resolvedDisplay.detailColumns"
      :link-to="linkTo"
    />
    <RecordsCards
      v-else-if="resolvedDisplay.layout === 'CARDS'"
      :rows="fetchedRows"
      :title-template="resolvedDisplay.titleTemplate"
      :subtitle-template="resolvedDisplay.subtitleTemplate"
      :description-column="resolvedDisplay.descriptionColumn"
      :detail-columns="resolvedDisplay.detailColumns"
      :logo-column="resolvedDisplay.logoColumn"
      :link-to="linkTo"
    />
    <RecordsList
      v-else-if="resolvedDisplay.layout === 'LIST'"
      :rows="fetchedRows"
      :title-template="resolvedDisplay.titleTemplate"
      :subtitle-template="resolvedDisplay.subtitleTemplate"
      :description-column="resolvedDisplay.descriptionColumn"
      :detail-columns="resolvedDisplay.detailColumns"
      :logo-column="resolvedDisplay.logoColumn"
      :link-to="linkTo"
      :hide-empty="hideEmpty"
    />
    <RecordsLinks
      v-else-if="resolvedDisplay.layout === 'LINKS'"
      :rows="fetchedRows"
      :title-template="resolvedDisplay.titleTemplate"
      :link-to="linkTo"
    />
    <RecordsBullets
      v-else-if="resolvedDisplay.layout === 'BULLETS'"
      :rows="fetchedRows"
      :title-template="resolvedDisplay.titleTemplate"
      :link-to="linkTo"
    />

    <!-- pt-5: CARDS and LIST end on a card border with a -mb-[1px] overlap,
    so the pager needs a gap above it or it lands right on that line. -->
    <Pagination
      v-if="layoutMeta.paginated"
      :current-page="currentPage"
      :total-pages="totalPages"
      class="pt-5 pb-[30px]"
      @update="onPageUpdate"
    />
    <ShowMore v-else :has-more="hasMore" @show-more="loadMore">
      <template #more>Load more ({{ remaining }})</template>
    </ShowMore>

    <p v-if="layoutMeta.paginated" class="text-center text-pagination">
      {{ rangeText }} of {{ fetchedCount }}
    </p>
  </div>
</template>

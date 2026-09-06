<script setup lang="ts">
import { computed, ref, useId, watch } from "vue";
import { useAsyncData } from "#app";
import type { IRow } from "../../../../metadata-utils/src/types";
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

// Any input that changes what the result set IS goes back to page 1. This
// must run before useAsyncData's own key/watch react to the same change, so
// it stays "sync": useAsyncData's key watcher is sync internally, and a
// "pre"-flush watcher here would run after it, one page too late.
watch(
  () => [props.schemaId, props.tableId, props.filter, props.pageSize],
  () => {
    currentPage.value = 1;
  },
  { flush: "sync" }
);

// A newer request racing past a slower one must win. useAsyncData dedupes
// its own keyed fetch; this id is for loadMore below, the one path that is
// still a bare fetch outside that mechanism.
let latestRequestId = 0;

// Unique per instance's actual query: a record page renders several of
// these side by side for different refbacks, each needing its own fetch and
// its own place in the SSR payload. useId() is stable across the server
// render and the client hydration of the SAME instance, unlike a random id.
const instanceId = useId();
const asyncDataKey = computed(
  () =>
    `records-${instanceId}-${props.schemaId}-${props.tableId}-${JSON.stringify(
      props.filter ?? null
    )}-${currentPage.value}`
);

const { data } = useAsyncData(
  asyncDataKey,
  async () => {
    latestRequestId++;
    const metadata = await fetchTableMetadata(props.schemaId, props.tableId);
    const paginated = layoutMeta.value.paginated;
    // pageSize belongs to the paging layouts; a layout that loads on demand
    // has its own batch size and does not inherit a pager's.
    const limit = paginated ? props.pageSize : layoutMeta.value.batchSize;
    const offset = paginated ? (currentPage.value - 1) * props.pageSize : 0;
    const response = await fetchTableData(props.schemaId, props.tableId, {
      limit,
      offset,
      filter: props.filter,
      expandLevel: 1,
    });
    return {
      columns: metadata.columns,
      rows: response.rows,
      count: response.count,
    };
  },
  {
    watch: [
      () => props.schemaId,
      () => props.tableId,
      () => props.filter,
      () => props.pageSize,
      currentPage,
    ],
  }
);

const fetchedColumns = computed(() => data.value?.columns ?? []);

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

// useAsyncData REPLACES data.value on every fetch; LINKS/BULLETS need to
// APPEND across "load more" clicks, so that accumulation lives in its own
// ref, reseeded whenever a fresh keyed fetch replaces the whole result.
const accumulatedRows = ref<IRow[]>(data.value?.rows ?? []);
const accumulatedCount = ref(data.value?.count ?? 0);

watch(data, (value) => {
  accumulatedRows.value = value?.rows ?? [];
  accumulatedCount.value = value?.count ?? 0;
});

const fetchedRows = computed(() => accumulatedRows.value);
const fetchedCount = computed(() => accumulatedCount.value);

// LINKS and BULLETS never move currentPage (they render no Pagination), so
// the keyed fetch above only re-fires for them on schemaId/tableId/filter/
// pageSize, which is exactly a fresh first batch. loadMore is the one path
// that appends instead of replacing, for the "show more" control's own click.
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
  accumulatedRows.value = [...accumulatedRows.value, ...response.rows];
  accumulatedCount.value = response.count;
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

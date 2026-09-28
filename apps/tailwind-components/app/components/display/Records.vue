<script setup lang="ts">
import { computed, ref, useId, watch } from "vue";
import { useAsyncData } from "#app";
import type { IRow } from "../../../../metadata-utils/src/types";
import type { DisplayConfig } from "../../types/display";
import { resolveDisplay, resolveLayout } from "../../utils/displayUtils";
import fetchTableData from "../../composables/fetchTableData";
import fetchTableMetadata from "../../composables/fetchTableMetadata";
import Button from "../Button.vue";
import Message from "../Message.vue";
import Pagination from "../Pagination.vue";
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
    filter?: Record<string, unknown>;
    pageSize?: number;
    linkTo?: (row: IRow) => string;
    showEmpty?: boolean;
  }>(),
  {
    pageSize: 10,
  }
);

const currentPage = ref(1);

const layout = computed(() => resolveLayout(props.displayConfig));

function assertNever(value: never): never {
  throw new Error(`Records: unhandled layout "${value}"`);
}

const layoutMeta = computed(() => {
  switch (layout.value) {
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
      return assertNever(layout.value);
  }
});

const filterKey = computed(() => JSON.stringify(props.filter ?? null));

watch(
  () => [
    props.schemaId,
    props.tableId,
    filterKey.value,
    props.pageSize,
    layout.value,
  ],
  () => {
    currentPage.value = 1;
  },
  // sync: must reset the page before useAsyncData's own key watcher refetches.
  { flush: "sync" }
);

let latestRequestId = 0;

// useId, not a random id: the key must match between server render and hydration.
const instanceId = useId();
const asyncDataKey = computed(
  () =>
    `records-${instanceId}-${props.schemaId}-${props.tableId}-${filterKey.value}-${layout.value}-${currentPage.value}`
);

const { data, error } = useAsyncData(
  asyncDataKey,
  async () => {
    latestRequestId++;
    const metadata = await fetchTableMetadata(props.schemaId, props.tableId);
    const paginated = layoutMeta.value.paginated;
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
      filterKey,
      () => props.pageSize,
      layout,
      currentPage,
    ],
  }
);

const fetchedColumns = computed(() => data.value?.columns ?? []);

const resolvedDisplay = computed(() =>
  resolveDisplay(fetchedColumns.value, props.displayConfig)
);

const accumulatedRows = ref<IRow[]>(data.value?.rows ?? []);
const accumulatedCount = ref(data.value?.count ?? 0);

watch(data, (value) => {
  accumulatedRows.value = value?.rows ?? [];
  accumulatedCount.value = value?.count ?? 0;
});

const fetchedRows = computed(() => accumulatedRows.value);
const fetchedCount = computed(() => accumulatedCount.value);

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
  <Message v-if="error" :id="`${instanceId}-error`" invalid>
    Could not load {{ tableId }}.
  </Message>
  <div v-else>
    <RecordsTable
      v-if="resolvedDisplay.layout === 'TABLE'"
      :rows="fetchedRows"
      :titleTemplate="resolvedDisplay.titleTemplate"
      :columns="resolvedDisplay.detailColumns"
      :linkTo="linkTo"
    />
    <RecordsCards
      v-else-if="resolvedDisplay.layout === 'CARDS'"
      :rows="fetchedRows"
      :titleTemplate="resolvedDisplay.titleTemplate"
      :subtitleTemplate="resolvedDisplay.subtitleTemplate"
      :descriptionColumn="resolvedDisplay.descriptionColumn"
      :detailColumns="resolvedDisplay.detailColumns"
      :logoColumn="resolvedDisplay.logoColumn"
      :linkTo="linkTo"
    />
    <RecordsList
      v-else-if="resolvedDisplay.layout === 'LIST'"
      :rows="fetchedRows"
      :titleTemplate="resolvedDisplay.titleTemplate"
      :subtitleTemplate="resolvedDisplay.subtitleTemplate"
      :descriptionColumn="resolvedDisplay.descriptionColumn"
      :detailColumns="resolvedDisplay.detailColumns"
      :logoColumn="resolvedDisplay.logoColumn"
      :linkTo="linkTo"
      :showEmpty="showEmpty"
    />
    <RecordsLinks
      v-else-if="resolvedDisplay.layout === 'LINKS'"
      :rows="fetchedRows"
      :titleTemplate="resolvedDisplay.titleTemplate"
      :linkTo="linkTo"
      :navLabel="tableId"
    />
    <RecordsBullets
      v-else-if="resolvedDisplay.layout === 'BULLETS'"
      :rows="fetchedRows"
      :titleTemplate="resolvedDisplay.titleTemplate"
      :linkTo="linkTo"
      :navLabel="tableId"
    />

    <Pagination
      v-if="layoutMeta.paginated"
      :currentPage="currentPage"
      :totalPages="totalPages"
      class="pt-5 pb-[30px]"
      @update="onPageUpdate"
    />
    <Button v-else-if="hasMore" type="text" size="small" @click="loadMore">
      Load more ({{ remaining }})
    </Button>

    <p v-if="layoutMeta.paginated" class="text-center text-pagination">
      {{ rangeText }} of {{ fetchedCount }}
    </p>
  </div>
</template>

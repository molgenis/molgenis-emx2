<script setup lang="ts">
import { computed, ref, useId, watch } from "vue";
import { useAsyncData } from "#app";
import type { IRow } from "../../../../metadata-utils/src/types";
import type { ListSeed } from "../../../types/recordConfig";
import type { Layout } from "../../types/display";
import { resolveDisplay } from "../../utils/displayUtils";
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
    layout?: Layout;
    filter?: Record<string, unknown>;
    pageSize?: number;
    linkTo?: (row: IRow) => string;
    showEmpty?: boolean;
    seed?: ListSeed;
  }>(),
  {
    layout: "TABLE",
    pageSize: 10,
  }
);

const currentPage = ref(1);

function assertNever(value: never): never {
  throw new Error(`Records: unhandled layout "${value}"`);
}

const layoutMeta = computed(() => {
  switch (props.layout) {
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
      return assertNever(props.layout);
  }
});

const filterKey = computed(() => JSON.stringify(props.filter ?? null));

watch(
  () => [
    props.schemaId,
    props.tableId,
    filterKey.value,
    props.pageSize,
    props.layout,
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
    `records-${instanceId}-${props.schemaId}-${props.tableId}-${filterKey.value}-${props.layout}-${currentPage.value}`
);

const { data, error } = useAsyncData(
  asyncDataKey,
  async () => {
    latestRequestId++;
    if (props.seed && currentPage.value === 1) {
      return props.seed;
    }
    const columns =
      props.seed?.columns ??
      (await fetchTableMetadata(props.schemaId, props.tableId)).columns;
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
      columns,
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
      () => props.layout,
      currentPage,
    ],
  }
);

const fetchedColumns = computed(() => data.value?.columns ?? []);

const resolvedDisplay = computed(() => resolveDisplay(fetchedColumns.value));

const appendedRows = ref<IRow[]>([]);
const appendedCount = ref<number>();

watch(data, () => {
  appendedRows.value = [];
  appendedCount.value = undefined;
});

// Derived from data, not copied: the server renders after the fetch but runs no watchers.
const fetchedRows = computed(() => [
  ...(data.value?.rows ?? []),
  ...appendedRows.value,
]);
const fetchedCount = computed(
  () => appendedCount.value ?? data.value?.count ?? 0
);

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
  appendedRows.value = [...appendedRows.value, ...response.rows];
  appendedCount.value = response.count;
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
      v-if="layout === 'TABLE'"
      :rows="fetchedRows"
      :titleColumn="resolvedDisplay.titleColumn"
      :columns="resolvedDisplay.detailColumns"
      :linkTo="linkTo"
    />
    <RecordsCards
      v-else-if="layout === 'CARDS'"
      :rows="fetchedRows"
      :titleColumn="resolvedDisplay.titleColumn"
      :subtitleColumn="resolvedDisplay.subtitleColumn"
      :descriptionColumn="resolvedDisplay.descriptionColumn"
      :detailColumns="resolvedDisplay.detailColumns"
      :logoColumn="resolvedDisplay.logoColumn"
      :linkTo="linkTo"
    />
    <RecordsList
      v-else-if="layout === 'LIST'"
      :rows="fetchedRows"
      :titleColumn="resolvedDisplay.titleColumn"
      :subtitleColumn="resolvedDisplay.subtitleColumn"
      :descriptionColumn="resolvedDisplay.descriptionColumn"
      :detailColumns="resolvedDisplay.detailColumns"
      :logoColumn="resolvedDisplay.logoColumn"
      :linkTo="linkTo"
      :showEmpty="showEmpty"
    />
    <RecordsLinks
      v-else-if="layout === 'LINKS'"
      :rows="fetchedRows"
      :titleColumn="resolvedDisplay.titleColumn"
      :linkTo="linkTo"
      :navLabel="tableId"
    />
    <RecordsBullets
      v-else-if="layout === 'BULLETS'"
      :rows="fetchedRows"
      :titleColumn="resolvedDisplay.titleColumn"
      :linkTo="linkTo"
      :navLabel="tableId"
    />

    <Pagination
      v-if="layoutMeta.paginated && totalPages > 1"
      :currentPage="currentPage"
      :totalPages="totalPages"
      class="pt-5 pb-[30px]"
      @update="onPageUpdate"
    />
    <Button v-else-if="hasMore" type="text" size="small" @click="loadMore">
      Load more ({{ remaining }})
    </Button>

    <p
      v-if="layoutMeta.paginated && totalPages > 1"
      class="text-center text-pagination"
    >
      {{ rangeText }} of {{ fetchedCount }}
    </p>
  </div>
</template>

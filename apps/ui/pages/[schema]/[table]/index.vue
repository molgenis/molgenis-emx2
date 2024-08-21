<script setup lang="ts">
import type { ITableSettings } from "../../../../tailwind-components/types/types";

const route = useRoute();
const router = useRouter();
const schemaId = route.params.schema as string;
const tableId = route.params.table as string;

type orderDirection = "ASC" | "DESC";

const orderbyColumn = computed(() => route.query.orderby as string);
const orderbyDirection = computed(() =>
  route.query.order ? (route.query.order as orderDirection) : "ASC"
);

const orderby = computed(() => {
  return orderbyColumn.value
    ? { [orderbyColumn.value]: orderbyDirection.value }
    : {};
});

const search = computed(() => route.query.search as string);

const tableSettings = computed(() => {
  return {
    page: 1,
    orderby: {
      column: orderbyColumn.value,
      direction: orderbyDirection.value,
    },
    search: search.value,
  };
});

const { data, status, error, refresh } = await useLazyAsyncData(
  "table meta data",
  async () => {
    const metaData = fetchTableMetadata(schemaId, tableId);
    const tableData = fetchTableData(schemaId, tableId, {
      limit: 10,
      offset: 0,
      orderby: orderby.value,
      searchTerms: search.value,
    });

    return await Promise.all([metaData, tableData]);
  },
  {
    watch: [tableSettings],
  }
);

const tableMetaData = computed(() => (data.value ? data.value[0] : null));
const tableData = computed(() =>
  data.value ? data.value[1] : { rows: [], count: 0 }
);
const numberOfRows = computed(() => tableData.value.count);
const rows = computed(() => tableData.value.rows);

const dataColumns = computed(() => {
  if (!tableMetaData.value) return [];

  return tableMetaData.value.columns.filter((c) => !c.id.startsWith("mg"));
});

function handleSettingsUpdate(settings: ITableSettings) {
  const query = {
    ...route.query,
    orderby: settings.orderby.column,
    order: !settings.orderby.column ? undefined : settings.orderby.direction,
    search: settings.search === "" ? undefined : settings.search,
  };

  router.push({ query });
}
</script>
<template>
  <Container>
    <PageHeader :title="tableMetaData?.label">
      <template #prefix>
        <BreadCrumbs
          :crumbs="{
            databases: '/',
            tables: `/${schemaId}`,
            [tableMetaData?.label ||
            tableMetaData?.id ||
            '']: `/${schemaId}/${tableId}`,
          }"
        />
      </template>
    </PageHeader>

    <ContentBlock
      :title="tableMetaData?.label || tableMetaData?.id || ''"
      :description="tableMetaData?.description"
    >
      <div v-if="status === 'pending'">Loading...</div>
      <div v-if="error">Error: {{ error }}</div>
      <!-- <div>{{ dataColumns }}</div> -->
      <!-- <pre v-if="data">{{ tableMetaData }}</pre> -->
      <!-- <pre>{{ tableData}}</pre> -->
      <TableEMX2
        :columns="dataColumns"
        :rows="rows"
        :settings="tableSettings"
        @update:settings="handleSettingsUpdate"
      />
    </ContentBlock>
  </Container>
</template>

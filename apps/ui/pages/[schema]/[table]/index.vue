<script setup lang="ts">
const schemaId = useRoute().params.schema as string;
const tableId = useRoute().params.table as string;

const { data, status, error, refresh } = await useLazyAsyncData(
  "table meta data",
  async () => {
    const metaData = fetchTableMetadata(schemaId, tableId);
    const tableData = fetchTableData(schemaId, tableId, {
      limit: 10,
      offset: 0,
    });

    return await Promise.all([metaData, tableData]);
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
      class="mt-1"
      :title="tableMetaData?.label || tableMetaData?.id || ''"
      :description="tableMetaData?.description"
    >
      <div v-if="status === 'pending'">Loading...</div>
      <div v-if="error">Error: {{ error }}</div>
      <!-- <div>{{ dataColumns }}</div> -->
      <!-- <pre v-if="data">{{ tableMetaData }}</pre> -->
      <!-- <pre>{{ tableData}}</pre> -->
      <TableEMX2 :columns="dataColumns" :rows="rows" />
    </ContentBlock>
  </Container>
</template>

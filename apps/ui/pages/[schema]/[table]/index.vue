<script setup lang="ts">
import { computed } from "vue";
import type { ITableSettings } from "../../../../tailwind-components/types/types";
import { useLazyAsyncData } from "#app/composables/asyncData";
import fetchTableMetadata from "../../../../tailwind-components/composables/fetchTableMetadata";
import fetchTableData from "../../../../tailwind-components/composables/fetchTableData";
import { useRoute, useRouter } from "#app/composables/router";

const route = useRoute();
const router = useRouter();
const schemaId = route.params.schema as string;
const tableId = route.params.table as string;

const currentPage = computed(() => {
  const queryPageNumber = Number(route.query?.page);
  return !isNaN(queryPageNumber) && typeof queryPageNumber === "number"
    ? Math.round(queryPageNumber)
    : 1;
});

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
    page: currentPage.value,
    pageSize: 10,
    orderby: {
      column: orderbyColumn.value,
      direction: orderbyDirection.value,
    },
    search: search.value,
  };
});

const offset = computed(
  () => (currentPage.value - 1) * tableSettings.value.pageSize
);

const { data, error } = await useLazyAsyncData(
  "table explorer data",
  async () => {
    const metaData = fetchTableMetadata(schemaId, tableId);
    const tableData = fetchTableData(schemaId, tableId, {
      limit: tableSettings.value.pageSize,
      offset: offset.value,
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

  return tableMetaData.value.columns
    .filter((c) => !c.id.startsWith("mg"))
    .filter((c) => c.columnType !== "HEADING");
});

function handleSettingsUpdate(settings: ITableSettings) {
  const query = {
    ...route.query,
    orderby: settings.orderby.column,
    order: !settings.orderby.column ? undefined : settings.orderby.direction,
    search: settings.search === "" ? undefined : settings.search,
    page: settings.page < 2 ? undefined : settings.page,
  };

  router.push({ query });
}

const crumbs = computed(() => {
  let crumb: { [key: string]: string } = {};
  crumb[schemaId] = `/${schemaId}`;
  return crumb;
});
const current = computed(
  () => tableMetaData.value?.label ?? tableMetaData.value?.id ?? ""
);
</script>
<template>
  <section class="mx-auto lg:px-[30px] px-0">
    <PageHeader :title="tableMetaData?.label ?? ''" align="left">
      <template #prefix>
        <BreadCrumbs :align="'left'" :crumbs="crumbs" :current="current" />
      </template>
    </PageHeader>
    <div v-if="error">Error: {{ error }}</div>
    <!-- <pre v-if="data">{{ tableMetaData }}</pre> -->
    <!-- <pre>{{ tableData}}</pre> -->
    <TableEMX2
      :table-id="tableId"
      :columns="dataColumns"
      :rows="rows"
      :count="numberOfRows"
      :settings="tableSettings"
      @update:settings="handleSettingsUpdate"
    />
  </section>
</template>

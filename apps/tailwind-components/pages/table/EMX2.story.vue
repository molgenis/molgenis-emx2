<script setup lang="ts">
import type { ITableSettings, Resp, Schema } from "~/types/types";

const tableSettings = ref<ITableSettings>({
  page: 1,
  pageSize: 10,
  orderby: { column: "", direction: "ASC" },
  search: "",
});

const { data } = await useFetch<Resp<Schema>>("/graphql", {
  key: "databases",
  method: "POST",
  body: { query: `{ _schemas { id,label,description } }` },
});

const databases = computed(
  () =>
    data.value?.data?._schemas.sort((a, b) => a.label.localeCompare(b.label)) ??
    []
);

const schemaId = ref(
  databases.value.find(
    (database) =>
      database.label === "pet store" || database.id === "catalogue-demo"
  )?.id || ""
);

const tableId = ref(
  schemaId.value === "pet store"
    ? "Pet"
    : schemaId.value === "catalogue-demo"
    ? "Resources"
    : ""
);

const schemaOptions = computed(() =>
  databases.value.map((schema) => schema.id)
);

const { data: metadata, refresh: refetchMetadata } = await useLazyAsyncData(
  "my meta data",
  () => fetchMetadata(schemaId.value)
);

if (metadata.value) {
  tableId.value = metadata.value.tables[0].id;
}

const tableOptions = computed(() => {
  if (metadata.value) {
    return metadata.value.tables.map((table) => table.id);
  } else {
    return [];
  }
});

const {
  data: tableData,
  error,
  refresh: refetchTableData,
} = await useLazyAsyncData("my data", () =>
  fetchTableData(schemaId.value, tableId.value)
);

watch(schemaId, () => {
  refetchMetadata().then(() => {
    if (metadata.value) {
      tableId.value = metadata.value.tables[0].id;
      refetchTableData();
    }
  });
});

watch(tableId, () => refetchTableData);

const tableColumns = computed(() => {
  return (
    metadata.value?.tables
      .find((t) => t.id === tableId.value)
      ?.columns.filter((column) => !column.id.startsWith("mg")) ?? []
  );
});

const dataRows = computed(() => {
  if (!tableData.value) return [];

  const filteredRows = tableData.value.rows.map((row) => {
    return Object.fromEntries(
      Object.entries(row).filter(([key]) => !key.startsWith("mg"))
    );
  });

  if (tableSettings.value.orderby?.column) {
    return filteredRows.sort(sortRow);
  } else {
    return filteredRows;
  }
});

const numberOfRows = computed(() => tableData?.value?.count ?? 0);

function sortRow(row1: Record<string, any>, row2: Record<string, any>) {
  const orderbyColumn = tableSettings.value.orderby.column;
  const orderbyDirection = tableSettings.value.orderby.direction;

  const row1Value =
    typeof row1[orderbyColumn] === "string"
      ? row1[orderbyColumn]
      : row1[orderbyColumn]?.name;
  const row2Value =
    typeof row2[orderbyColumn] === "string"
      ? row2[orderbyColumn]
      : row2[orderbyColumn]?.name;
  if (orderbyDirection === "ASC") {
    return row1Value > row2Value ? 1 : -1;
  } else {
    return row1Value < row2Value ? 1 : -1;
  }
}
</script>

<template>
  <div class="mt-4 mb-16">
    <h3 class="text-heading-lg">Params</h3>
    <div class="m-2">
      <label for="schema-id-input">schema id: </label>
      <select id="schema-id-input" v-model="schemaId">
        <option v-for="option in schemaOptions" :value="option">
          {{ option }}
        </option>
      </select>
    </div>
    <div class="m-2">
      <label for="table-id-select">table id: </label>
      <select id="table-id-select" v-model="tableId">
        <option v-for="option in tableOptions" :value="option">
          {{ option }}
        </option>
      </select>
    </div>
  </div>

  <div>
    <TableEMX2
      v-if="tableId && schemaId"
      :table-id="tableId"
      :columns="tableColumns"
      :rows="dataRows"
      :count="numberOfRows"
      @update:settings="(value: ITableSettings) => tableSettings = value"
      :settings="tableSettings"
    />
  </div>
</template>

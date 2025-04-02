<script setup lang="ts">
import { useFetch, useLazyAsyncData } from "#app";
import { fetchMetadata, fetchTableData } from "#imports";
import { ref, computed, watch } from "vue";
import type { ITableSettings, Resp, Schema } from "../../types/types";

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
    data.value?.data?._schemas.sort((a: any, b: any) =>
      a.label.localeCompare(b.label)
    ) ?? []
);

const schemaId = ref(
  databases.value.find(
    (d: any) => d.label === "pet store" || d.id === "catalogue-demo"
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
  databases.value.map((schema: any) => schema.id)
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

watch(schemaId, async () => {
  refetchMetadata().then(() => {
    if (metadata.value) {
      tableId.value = metadata.value.tables[0].id;
      refetchTableData();
    }
  });
});

watch(tableId, async () => {
  refetchTableData();
});

const tableColumns = computed(() => {
  return (
    metadata.value?.tables
      .find((t) => t.id === tableId.value)
      ?.columns.filter((column) => !column.id.startsWith("mg")) ?? []
  );
});

const dataRows = computed(() => {
  if (!tableData.value) return [];

  return tableData.value.rows.map((row) => {
    return Object.fromEntries(
      Object.entries(row).filter(([key]) => !key.startsWith("mg"))
    );
  });
});

const numberOfRows = computed(() => tableData?.value?.count ?? 0);
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
d

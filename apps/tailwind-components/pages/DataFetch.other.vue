<script setup lang="ts">
import type { Schema } from "~/types/types";

type Resp<T> = {
  data: Record<string, T[]>;
};

const route = useRoute();
const schemaId = ref((route.query.schema as string) ?? "pet store");
const tableId = ref((route.query.table as string) ?? "Pet");

const { data: schemas } = await useFetch<Resp<Schema>>("/graphql", {
  key: "schemas",
  method: "POST",
  body: { query: `{ _schemas { id,label,description } }` },
});

const schemaIds = computed(
  () =>
    schemas.value?.data?._schemas
      .sort((a, b) => a.label.localeCompare(b.label))
      .map((s) => s.id) ?? []
);

const {
  data: metadata,
  error: metadataError,
  refresh: refetchMetadata,
} = await useAsyncData("my meta data", () => fetchMetadata(schemaId.value));

const schemaTablesIds = computed(() =>
  metadata.value?.tables.map((table) => table.id)
);

const {
  data: tableData,
  status,
  error,
  refresh: refetchTableData,
} = await useAsyncData("my data", () =>
  fetchTableData(schemaId.value, tableId.value)
);

watch(
  () => schemaId.value,
  async () => {
    if (metadata.value) {
      await refetchMetadata();
      await refetchTableData();
      tableId.value = metadata.value.tables[0].id;
      useRouter().push({
        query: {
          ...useRoute().query,
          schema: schemaId.value,
          table: tableId.value,
        },
      });
    }
  }
);

watch(
  () => tableId.value,
  async () => {
    console.log("tableID", tableId.value);
    await refetchTableData();
    useRouter().push({
      query: {
        ...useRoute().query,
        schema: schemaId.value,
        table: tableId.value,
      },
    });
  }
);
</script>

<template>
  <h1>Data and Meta data</h1>
  <div class="p-4 border-2 mb-2 flex flex-col gap-4">
    <div class="flex flex-col">
      <label for="table-select" class="text-title font-bold">Schema: </label>
      <select id="table-select" v-model="schemaId" class="border border-black">
        <option v-for="schemaId in schemaIds" :value="schemaId">
          {{ schemaId }}
        </option>
      </select>
    </div>

    <div class="flex flex-col">
      <label for="table-select" class="text-title font-bold">Table: </label>
      <select id="table-select" v-model="tableId" class="border border-black">
        <option v-for="tableId in schemaTablesIds" :value="tableId">
          {{ tableId }}
        </option>
      </select>
    </div>
  </div>

  <div v-if="status === 'pending'">Loading...</div>
  <div v-if="error">Error: {{ error }}</div>
  <div v-if="metadataError">Meta data Error: {{ metadataError }}</div>

  <div v-if="metadata">
    <h2>Data for {{ tableId }}:</h2>
    {{ error }}
    <pre v-if="tableData">{{ tableData }}</pre>

    <h2>Schema: {{ metadata.label }}</h2>
    <h2>Tables:</h2>

    <ul class="pl-6 list-disc">
      <li v-for="table in metadata.tables">
        {{ table.id }} (type: {{ table.tableType }})
        <h3>Columns:</h3>
        <ul class="pl-6 pb-3 list-disc">
          <li v-for="column in table.columns">
            {{ column.id }} (type: {{ column.columnType }})
          </li>
        </ul>
      </li>
    </ul>
  </div>
</template>

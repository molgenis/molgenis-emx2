<script setup lang="ts">
import type { Schema } from "~/types/types";
import type { ISchemaMetaData } from "../../metadata-utils/src/types";

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

const { data: schemaMeta } = await useAsyncData("form sample", () =>
  fetchMetadata(schemaId.value)
);

const schemaTablesIds = computed(() =>
  (schemaMeta.value as ISchemaMetaData)?.tables.map((table) => table.id)
);

const {
  data: metadata,
  error: metadataError,
  refresh: refetchMetadata,
} = await useLazyAsyncData("my meta data", () => fetchMetadata(schemaId.value));

if (metadata.value) {
  tableId.value = metadata.value.tables[0].id;
}

const {
  data: tableData,
  status,
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

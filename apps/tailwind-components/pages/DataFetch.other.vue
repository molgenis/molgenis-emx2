<script setup lang="ts">
type Resp<T> = {
  data: Record<string, T[]>;
};

interface Schema {
  id: string;
  label: string;
  description: string;
}

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
  databases.value.find((d) => d.label === "pet store" || d.id === "catalogue")
    ?.id || ""
);

const schemaOptions = computed(() =>
  databases.value.map((schema) => schema.id)
);

const {
  data: metadata,
  pending: metadataPending,
  error: metadataError,
  refresh: refetchMetadata,
} = await useLazyAsyncData("my meta data", () => fetchMetadata(schemaId.value));

const tableId = ref("");

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
  pending,
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
  <div class="h-12 mt-4 mb-16">
    <h3 class="text-heading-lg">Params</h3>
    <div class="m-2">
      <label for="schema-id-input">schema id: </label>
      <select id="table-id-select" v-model="schemaId">
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

  <div v-if="pending">Loading...</div>
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

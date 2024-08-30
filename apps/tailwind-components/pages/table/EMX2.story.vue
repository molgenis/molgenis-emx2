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

  <div class=" ">
    <TableEMX2
      :columns="tableColumns"
      :rows="dataRows"
      :count="numberOfRows"
    ></TableEMX2>
  </div>
</template>
d

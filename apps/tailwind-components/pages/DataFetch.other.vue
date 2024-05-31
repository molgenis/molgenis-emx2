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

async function refetch() {
  metadata.value = await fetchMetadata(schemaId.value);
}
const metadata = ref(await fetchMetadata(schemaId.value));
</script>

<template>
  <div class="flex">
    <div class="flex-1">
      <div v-if="metadata">
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
    </div>
    <div class="h-12 ml-4 mt-2">
      <h3>Params</h3>
      schema id:
      <input type="text" v-model="schemaId" />
      <button
        class="items-center px-5 text-heading-lg bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
        @click="refetch"
      >
        re-fetch
      </button>
    </div>
  </div>
</template>

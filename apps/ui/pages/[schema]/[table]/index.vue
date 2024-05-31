<script setup lang="ts">
const schemaId = useRoute().params.schema as string;
const tableId = useRoute().params.table as string;
const store = useMetaStore();

const meta = await store.fetchSchemaMetaData(schemaId as string);
store.$patch((state) => {
  state.metaData[schemaId as string] = meta;
});

const tableMeta = store.getTableMeta(schemaId as string, tableId as string);
</script>
<template>
  <div>
    <h1>{{ schemaId }}</h1>
    <h2>{{ tableId }}</h2>

    <Container class="my-3">
      <DisplayList class="text-white" title="Data tables" :columnCount="3">
        <DisplayListItem v-for="column in tableMeta.columns">
          {{ column.id }} - {{ column.columnType }}
        </DisplayListItem>
      </DisplayList>
    </Container>
  </div>
</template>

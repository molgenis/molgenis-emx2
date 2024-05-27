<script setup lang="ts">
import type { ITableMetaData } from "../../../../meta-data-utils/src/types";

const { schema, table } = useRoute().params;

const store = useMetaStore();
const meta = await store.fetchSchemaMetaData(schema as string);

const tableMeta = computed(() => {
  const result = meta.tables.find(
    (t: ITableMetaData) =>
      t.id.toLowerCase() === (table as string).toLowerCase()
  );
  if (!result) {
    throw new Error(`Table with id ${table} not found in schema ${schema}`);
  }
  return result;
});
</script>
<template>
  <div>
    <h1>{{ schema }}</h1>
    <h2>{{ table }}</h2>

    <Container class="my-3">
      <DisplayList class="text-white" title="Data tables" :columnCount="3">
        <DisplayListItem v-for="column in tableMeta.columns">
          {{ column.id }} - {{ column.columnType }}
        </DisplayListItem>
      </DisplayList>
    </Container>
  </div>
</template>

<script setup lang="ts">
import type {
  ISchemaMetaData,
  ITableMetaData,
} from "../../metadata-utils/src/types";

const sampleType = ref("complex");

// just assuming that the table is there for the demo
const schemaId = computed(() =>
  sampleType.value === "simple" ? "pet store" : "catalogue"
);
const tableId = computed(() =>
  sampleType.value === "simple" ? "Pet" : "Collections"
);

const { data: schemaMeta, refresh: refetchMetadata } = await useAsyncData(
  "form sample",
  () => fetchMetadata(schemaId.value)
);

const tableMeta = computed(
  () =>
    (schemaMeta.value as ISchemaMetaData).tables.find(
      (table) => table.id === tableId.value
    ) as ITableMetaData
);

const updateSampleType = (event: Event) => {
  sampleType.value = (event.target as HTMLSelectElement).value;
  refetchMetadata();
};
</script>

<template>
  <div class="pb-10">
    <select @change="updateSampleType">
      <option value="complex">Complex form example</option>
      <option value="simple">Simple form example</option>
    </select>
  </div>
  <FormFields class="p-8" :meta-data="tableMeta"></FormFields>
</template>

<script setup lang="ts">
import type { FormFields } from "#build/components";
import type {
  columnValue,
  ISchemaMetaData,
  ITableMetaData,
} from "../../metadata-utils/src/types";

const sampleType = ref("simple");

// just assuming that the table is there for the demo
const schemaId = computed(() =>
  sampleType.value === "simple" ? "pet store" : "catalogue-demo"
);
const tableId = computed(() =>
  sampleType.value === "simple" ? "Pet" : "Resources"
);

const {
  data: schemaMeta,
  refresh: refetchMetadata,
  status,
} = await useAsyncData("form sample", () => fetchMetadata(schemaId.value));

const tableMeta = computed(
  () =>
    (schemaMeta.value as ISchemaMetaData)?.tables.find(
      (table) => table.id === tableId.value
    ) as ITableMetaData
);

function refetch() {
  refetchMetadata();
}

const data = ref([] as Record<string, columnValue>[]);

const formFields = ref<InstanceType<typeof FormFields>>();
</script>

<template>
  <div>Demo controles:</div>

  <div class="p-4 border-2 mb-2">
    <select
      @change="refetch()"
      v-model="sampleType"
      class="border-1 border-black"
    >
      <option value="simple">Simple form example</option>
      <option value="complex">Complex form example</option>
    </select>

    <div>schema id = {{ schemaId }}</div>
    <div>table id = {{ tableId }}</div>

    <button
      class="border-gray-900 border-[1px] p-2 bg-gray-200"
      @click="formFields?.validate"
    >
      External Validate
    </button>
  </div>

  <FormFields
    v-if="tableMeta && status == 'success'"
    class="p-8"
    :metadata="tableMeta"
    :data="data"
    ref="formFields"
  ></FormFields>
</template>

<script setup lang="ts">
import type { FormFields } from "#build/components";
import type {
  columnValue,
  ISchemaMetaData,
  ITableMetaData,
} from "../../metadata-utils/src/types";

const exampleName = ref("simple");

const exampleMap = ref({
  simple: {
    schema: "pet store",
    table: "Pet",
  },
  "pet store user": {
    schema: "pet store",
    table: "User",
  },
  complex: {
    schema: "catalogue-demo",
    table: "Resources",
  },
});

// just assuming that the table is there for the demo
const exampleConfig = computed(() => exampleMap.value[exampleName.value]);

const {
  data: schemaMeta,
  refresh: refetchMetadata,
  status,
} = await useAsyncData("form sample", () =>
  fetchMetadata(exampleConfig.value.schema)
);

const tableMeta = computed(
  () =>
    (schemaMeta.value as ISchemaMetaData)?.tables.find(
      (table) => table.id === exampleConfig.value.table
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
      v-model="exampleName"
      class="border-1 border-black"
    >
      <option value="simple">Simple form example</option>
      <option value="complex">Complex form example</option>
      <option value="pet store user">Pet store user</option>
    </select>

    <div>schema id = {{ exampleConfig.schema }}</div>
    <div>table id = {{ exampleConfig.table }}</div>

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

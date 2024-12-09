<script setup lang="ts">
import type { FormFields } from "#build/components";
import type {
  columnValue,
  ISchemaMetaData,
  ITableMetaData,
} from "../../metadata-utils/src/types";
import type { IListboxOption } from "../types/listbox";

const exampleForms: IListboxOption[] = [
  { value: "simple", label: "Simple form example" },
  { value: "complex", label: "Complex form example" },
];

const formType = ref<IListboxOption>(exampleForms[0]);

// just assuming that the table is there for the demo
const schemaId = computed(() =>
  formType.value.value === "simple" ? "pet store" : "catalogue-demo"
);
const tableId = computed(() =>
  formType.value.value === "simple" ? "Pet" : "Resources"
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
  console.log("refetching...");
  refetchMetadata();
}

const data = ref([] as Record<string, columnValue>[]);

const formFields = ref<InstanceType<typeof FormFields>>();
</script>

<template>
  <div>Demo controles:</div>

  <div class="p-4 border-2 mb-2">
    <InputLabel for="form-example" id="form-example-title">
      Select a form to display
    </InputLabel>
    <InputListbox
      id="form-example"
      label-id="form-example-title"
      @change="refetch"
      v-model="formType"
      :options="exampleForms"
      placeholder="Select a form type"
    />

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

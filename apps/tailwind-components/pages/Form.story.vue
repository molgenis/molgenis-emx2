<script setup lang="ts">
import type { FormFields } from "#build/components";
import type {
  columnValue,
  IFieldError,
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

const formValues = ref<Record<string, columnValue>>({});

function onModelUpdate(value: Record<string, columnValue>) {
  formValues.value = value;
}

const errors = ref<Record<string, IFieldError[]>>({});

function onErrors(newErrors: Record<string, IFieldError[]>) {
  errors.value = newErrors;
}
</script>

<template>
  <div class="flex flex-row">
    <FormFields
      v-if="tableMeta && status == 'success'"
      ref="formFields"
      class="basis-1/2 p-8"
      :metadata="tableMeta"
      :data="data"
      @update:model-value="onModelUpdate"
      @error="onErrors"
    ></FormFields>

    <div class="basis-1/2">
      <div>Demo controls, settings and status:</div>

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

        <div class="mt-4 flex flex-row">
          <div v-if="Object.keys(formValues).length" class="basis-1/2">
            <h3 class="text-label">Values</h3>
            <dl class="flex">
              <template v-for="(value, key) in formValues">
                <dt v-if="value" class="font-bold">{{ key }}:</dt>
                <dd v-if="value" class="ml-1">{{ value }}</dd>
              </template>
            </dl>
          </div>
          <div v-if="Object.keys(errors).length" class="basis-1/2">
            <h3 class="text-label">Errors</h3>

            <dl class="flex">
              <template v-for="(value, key) in errors">
                <dt v-if="value.length" class="font-bold">{{ key }}:</dt>
                <dd v-if="value.length" class="ml-1">{{ value }}</dd>
              </template>
            </dl>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

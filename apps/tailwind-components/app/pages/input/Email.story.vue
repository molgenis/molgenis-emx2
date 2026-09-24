<template>
  <div id="fields-container" class="col-span-3 py-50px overflow-y-auto">
    <ClientOnly>
      <FormFields id="email-form" :form="form" />
    </ClientOnly>
  </div>
</template>

<script setup lang="ts">
import { useForm } from "#imports";
import { ref } from "vue";
import type {
  ColumnType,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";

const model = ref({});
const metadata = ref<ITableMetaData>({
  id: "Types",
  label: "Email Form",
  tableType: "DATA",
  name: "Types",
  schemaId: "Types",
  columns: [
    {
      id: "emailType",
      columnType: "EMAIL" as ColumnType,
      label: "Input an email address",
    },
  ],
});
const form = useForm(metadata, model);
</script>

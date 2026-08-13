<template>
  <div id="fields-container" class="col-span-3 py-50px overflow-y-auto">
    <ClientOnly>
      <FormFields id="form-hyperlink" :form="form" />
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
const metadata: ITableMetaData = {
  label: "Hyperlink",
  id: "Types",
  name: "Types",
  schemaId: "Types",
  tableType: "DATA",
  columns: [
    {
      id: "hyperlinkType",
      columnType: "HYPERLINK" as ColumnType,
      label: "Input a hyperlink",
    },
  ],
};
const form = useForm(metadata, model);
</script>

<template>
  <div id="fields-container" class="col-span-3 py-50px overflow-y-auto">
    <ClientOnly>
      <FormFields id="form-hyperlink" :form="form" />
    </ClientOnly>
  </div>
</template>
<script setup lang="ts">
import { ref } from "vue";
import type { ColumnType } from "../../../../metadata-utils/src/types";
import { useForm } from "#imports";

const model = ref({});
const metadata = {
  label: "Hyperlink",
  id: "Types",
  name: "Types",
  schemaId: "Types",
  tableType: "my kind of table",
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

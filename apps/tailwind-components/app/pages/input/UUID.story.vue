<template>
  <div id="fields-container" class="col-span-3 py-50px overflow-y-auto">
    <ClientOnly>
      <FormFields id="uuid-form" :form="form" />
    </ClientOnly>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import type {
  ColumnType,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import useForm from "../../composables/useForm";

const model = ref({});
const metadata = ref<ITableMetaData>({
  id: "Types",
  label: "UUID Form",
  tableType: "DATA",
  name: "Types",
  schemaId: "Types",
  columns: [
    {
      id: "uuidType",
      columnType: "UUID" as ColumnType,
      label: "Please enter a UUID",
    },
  ],
});
const form = useForm(metadata, model);
</script>

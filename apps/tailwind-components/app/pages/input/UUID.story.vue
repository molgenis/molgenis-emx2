<template>
  <div id="fields-container" class="col-span-3 py-50px overflow-y-auto">
    <ClientOnly>
      <FormFields
        id="uuid-form"
        v-model="model"
        :columns="metadata.columns"
        :visibleColumnErrors="visibleColumnErrors"
        @update="onUpdateColumn"
        @blur="onBlurColumn"
      />
    </ClientOnly>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import type { ColumnType } from "../../../../metadata-utils/src/types";
import { useForm } from "../../../.nuxt/imports";

const model = ref({});
const metadata = ref({
  id: "Types",
  label: "UUID Form",
  tableType: "FORM",
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
const { visibleColumnErrors, onUpdateColumn, onBlurColumn } = useForm(
  metadata,
  model
);
</script>

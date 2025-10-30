<template>
  <div id="fields-container" class="col-span-3 py-50px overflow-y-auto">
    <FormFields
      id="email-form"
      v-model="model"
      :columns="metadata.columns"
      :error-map="errorMap"
      @update="onUpdateColumn"
      @blur="onBlurColumn"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import type { ColumnType } from "../../../../metadata-utils/src/types";
import { useForm } from "#imports";

const model = ref({});
const metadata = ref({
  id: "Types",
  label: "Email Form",
  tableType: "FORM",
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
const { errorMap, onUpdateColumn, onBlurColumn } = useForm(metadata, model);
</script>

<template>
  <div id="fields-container" class="col-span-3 py-50px overflow-y-auto">
    <FormFields
      id="form-hyperlink"
      :columns="metadata.columns"
      v-model="model"
      :error-map="errorMap"
      @update="onUpdateColumn"
      @blur="onBlurColumn"
    />
  </div>
</template>
<script setup lang="ts">
import { ref } from "vue";
import type { ColumnType } from "../../../metadata-utils/src/types";
import { useForm } from "#imports";

const model = ref({});
const metadata = {
  label: "Hyperlink",
  id: "Types",
  tableType: "my kind of table",
  columns: [
    {
      id: "hyperlinkType",
      columnType: "HYPERLINK" as ColumnType,
      label: "Input a hyperlink",
    },
  ],
};
const { errorMap, onUpdateColumn, onBlurColumn } = useForm(metadata, model);
</script>

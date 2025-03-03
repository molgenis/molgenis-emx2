<template>
  <h2>Email component</h2>
  <p>Using a form with input of type email you can have email validation</p>
  <FormFields
    id="email-form"
    schemaId="schema-email"
    v-model="model"
    :metadata="metadata"
    v-model:errors="errorMap"
    :sections="sections"
  />
</template>

<script setup lang="ts">
import type { ColumnType } from "../../../metadata-utils/src/types";

const model = ref({});
const metadata = ref({
  id: "email-form",
  label: "Email Form",
  tableType: "FORM",
  columns: [
    {
      id: "input-email",
      columnType: "EMAIL" as ColumnType,
      label: "Input an email address",
    },
  ],
});
const activeChapterId = ref("_scroll_to_top");
const errorMap = ref({});
const sections = useSections(metadata, activeChapterId, errorMap);
</script>

<template>
  <h2>Email component</h2>
  <p>Using a form with input of type email you can have email validation</p>
  <FormFields
    id="email-form"
    schemaId="schema-email"
    v-model="model"
    :metadata="{
      id: 'email-form',
      label: 'Email Form',
      tableType: 'FORM',
      columns: [
        {
          id: 'input-email',
          columnType: 'EMAIL',
          label: 'Input an email address',
        },
      ],
    }"
    :chapters="chapters"
    :visibleMap="visibleMap"
    :errorMap="errorMap"
    :activeChapterId="activeChapterId"
  />
</template>

<script setup lang="ts">
import type { ColumnType } from "../../../metadata-utils/src/types";

const model = ref({ "input-email": null });
const chapters = ref([
  {
    label: "_top",
    id: "_scroll_to_top",
    columns: [
      {
        id: "input-email",
        columnType: "EMAIL" as ColumnType,
        label: "Input an email address",
      },
    ],
    isActive: false,
    errorCount: 0,
  },
]);
const visibleMap = ref({
  "input-email": true,
});
const activeChapterId = ref(null);
const errorMap = ref({});
</script>

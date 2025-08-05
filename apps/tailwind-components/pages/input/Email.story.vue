<template>
  <div id="fields-container" class="col-span-3 py-50px overflow-y-auto">
    <FormFields
      id="email-form"
      schemaId="type test"
      v-model="model"
      :metadata="metadata"
      v-model:errors="errorMap"
      :sections="sections"
    />
  </div>
</template>

<script setup lang="ts">
import { useSections } from "#imports";
import { ref } from "vue";
import type { ColumnType } from "../../../metadata-utils/src/types";

const model = ref({});
const metadata = ref({
  id: "Types",
  label: "Email Form",
  tableType: "FORM",
  columns: [
    {
      id: "emailType",
      columnType: "EMAIL" as ColumnType,
      label: "Input an email address",
    },
  ],
});
const activeChapterId = ref("_scroll_to_top");
const errorMap = ref({});
const sections = useSections(metadata, activeChapterId, errorMap);
</script>

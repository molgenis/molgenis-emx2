<template>
  <div id="fields-container" class="col-span-3 py-50px overflow-y-auto">
    <FormFields
      id="form-hyperlink"
      schemaId="type test"
      :metadata="metadata"
      v-model="model"
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

const activeChapterId = ref<string>("_scroll_to_top");
const errorMap = ref({});
const sections = useSections(metadata, activeChapterId, errorMap);
</script>

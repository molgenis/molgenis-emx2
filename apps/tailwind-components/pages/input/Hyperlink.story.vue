<template>
  <p>
    The <code>Hyperlink Component</code> enables you to use the String component
    with the added validation of the input being a hyperlink.
  </p>
  <FormFields
    id="form-hyperlink"
    schemaId="type test"
    :metadata="metadata"
    v-model="model"
    v-model:errors="errorMap"
    :sections="sections"
  />
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

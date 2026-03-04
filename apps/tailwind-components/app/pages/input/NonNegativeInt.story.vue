<template>
  <div id="fields-container" class="col-span-3 py-50px overflow-y-auto">
    <ClientOnly>
      <FormFields id="form-non-negative-int" :form="form" />
    </ClientOnly>
  </div>
</template>
<script setup lang="ts">
import { ref } from "vue";
import type { ColumnType } from "../../../../metadata-utils/src/types";
import { useForm } from "#imports";

const model = ref({});
const metadata = {
  label: "NonNegativeInt",
  id: "Types",
  name: "Types",
  schemaId: "Types",
  tableType: "Form",
  columns: [
    {
      id: "nonNegativeIntType",
      columnType: "NON_NEGATIVE_INT" as ColumnType,
      label: "Input a non negative int",
    },
  ],
};
const form = useForm(metadata, model);
</script>

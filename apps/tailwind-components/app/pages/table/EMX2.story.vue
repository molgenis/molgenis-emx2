<script setup lang="ts">
import { computed, ref } from "vue";
import type { ITableSettings } from "../../../types/types";
import DemoDataControls from "../../DemoDataControls.vue";
import type { ITableMetaData } from "../../../../metadata-utils/src/types";

const tableSettings = ref<ITableSettings>({
  page: 1,
  pageSize: 10,
  orderby: { column: "", direction: "ASC" },
  search: "",
});

const isEditable = ref(false);

const metadata = ref<ITableMetaData>();
const schemaId = ref<string>("type test");

const tableId = computed(() => {
  if (metadata.value) {
    return metadata.value.id;
  }
  return "";
});
</script>

<template>
  <div class="py-5 ">
    <DemoDataControls
      :include-row-select="false"
      v-model:metadata="metadata"
      v-model:schemaId="schemaId"
    />
    <label class="text-title font-bold" for="is-editable">Is Editable: </label>
    <InputCheckbox id="is-editable" v-model="isEditable" name="is-editable" />
    <div class="py-10" />
    <TableEMX2
      v-model:settings="tableSettings"
      :key="`${schemaId}-${tableId}`"
      :schema-id="schemaId"
      :table-id="tableId ?? ''"
      :is-editable="isEditable"
    />
  </div>
</template>

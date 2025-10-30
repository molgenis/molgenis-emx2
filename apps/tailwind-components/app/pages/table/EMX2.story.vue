<script setup lang="ts">
import { ref, watch } from "vue";
import type { ITableSettings } from "../../../types/types";
import DemoDataControls from "../../DemoDataControls.vue";
import type { ITableMetaData } from "../../../../metadata-utils/src/types";
import { useRoute, useRouter } from "vue-router";

const tableSettings = ref<ITableSettings>({
  page: 1,
  pageSize: 10,
  orderby: { column: "", direction: "ASC" },
  search: "",
});

const router = useRouter();
const route = useRoute();

const isEditable = ref(false);
const metadata = ref<ITableMetaData>();
const schemaId = ref<string>((route.query.schema as string) || "type test");
const tableId = ref<string>((route.query.table as string) || "Types");

watch([schemaId, tableId], ([newSchemaId, newTableId]) => {
  router.push({
    query: {
      schema: newSchemaId,
      table: newTableId,
    },
  });
});
</script>

<template>
  <div class="py-5 space-y-2">
    <DemoDataControls
      v-model:metadata="metadata"
      v-model:schemaId="schemaId"
      v-model:tableId="tableId"
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

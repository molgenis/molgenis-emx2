<template>
  <FormEditModal
    v-if="metadata"
    :metadata="metadata"
    :schemaId="schemaId"
    :key="metadata.id"
    :isInsert="true"
  />

  <div>
    <DemoDataControls
      v-model:metadata="metadata"
      v-model:schemaId="schemaId"
      v-model:tableId="tableId"
    >
    </DemoDataControls>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import type { ITableMetaData } from "../../../../metadata-utils/src/types";
import DemoDataControls from "../../DemoDataControls.vue";
import { useRoute, useRouter } from "vue-router";

const router = useRouter();
const route = useRoute();

const metadata = ref<ITableMetaData>();
const schemaId = ref<string>((route.query.schema as string) || "");
const tableId = ref<string>((route.query.table as string) || "");

watch([schemaId, tableId], ([newSchemaId, newTableId]) => {
  router.push({
    query: {
      schema: newSchemaId,
      table: newTableId,
    },
  });
});
</script>

<template>
  <FormEditModal
    v-if="metadata"
    :metadata="metadata"
    :schemaId="schemaId"
    :formValues="formValues"
    :key="`${schemaId} - ${metadata.id} - ${JSON.stringify(formValues)}`"
  ></FormEditModal>

  <div>
    <DemoDataControls
      v-model:metadata="metadata"
      v-model:schemaId="schemaId"
      v-model:formValues="formValues"
      v-model:tableId="tableId"
      :include-row-select="true"
      :row-index="rowIndex"
    >
    </DemoDataControls>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import type {
  columnId,
  columnValue,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import DemoDataControls from "../../DemoDataControls.vue";
import { useRoute, useRouter } from "vue-router";

const router = useRouter();
const route = useRoute();

const metadata = ref<ITableMetaData>();
const schemaId = ref<string>((route.query.schema as string) || "pet store");
const formValues = ref<Record<columnId, columnValue>>({});
const tableId = ref<string>((route.query.table as string) || "Category");
const rowIndex = ref<number>(
  route.query.rowIndex ? Number(route.query.rowIndex) : 0
);

watch([schemaId, tableId], ([newSchemaId, newTableId]) => {
  router.push({
    query: {
      schema: newSchemaId,
      table: newTableId,
    },
  });
});
</script>

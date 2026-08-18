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
  orderedColumnsIds: [],
});

const router = useRouter();
const route = useRoute();

const canInsert = ref(false);
const canUpdate = ref(false);
const canDelete = ref(false);
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

<template>
  <div class="overflow-auto">
    <div class="py-5 space-y-2">
      <DemoDataControls
        v-model:metadata="metadata"
        v-model:schemaId="schemaId"
        v-model:tableId="tableId"
      />
      <div>
        <label class="text-title font-bold" for="can-insert">
          Can insert:
        </label>
        <InputCheckbox id="can-insert" v-model="canInsert" name="can-insert" />
      </div>
      <div>
        <label class="text-title font-bold" for="can-update">
          Can update:
        </label>
        <InputCheckbox id="can-update" v-model="canUpdate" name="can-update" />
      </div>
      <div>
        <label class="text-title font-bold" for="can-delete">
          Can delete:
        </label>
        <InputCheckbox id="can-delete" v-model="canDelete" name="can-delete" />
      </div>
      <div class="py-10" />

      <TableEMX2
        v-model:settings="tableSettings"
        :key="`${schemaId}-${tableId}`"
        :schemaId="schemaId"
        :tableId="tableId ?? ''"
        :canInsert="canInsert"
        :canUpdate="canUpdate"
        :canDelete="canDelete"
        :enableFilters="true"
      />
    </div>
  </div>
</template>

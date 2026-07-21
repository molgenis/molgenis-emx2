<script setup lang="ts">
import { ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { ITableMetaData } from "../../../../metadata-utils/src/types";
import DemoDataControls from "../../DemoDataControls.vue";
import DataList from "../../components/display/DataList.vue";
import { provideRecordNavigation } from "../../composables/useRecordNavigation";

const router = useRouter();
const route = useRoute();

const schemaId = ref<string>((route.query.schema as string) || "pet store");
const tableId = ref<string>((route.query.table as string) || "Pet");
const metadata = ref<ITableMetaData>();
const formValues = ref<Record<string, any>>({});

const layout = ref<"TABLE" | "CARDS" | "LIST" | "LINKS">("TABLE");
const pageSize = ref(10);

const clickLog = ref<string[]>([]);

provideRecordNavigation({
  async navigateToRecord(schema, table, row) {
    const message = `Navigate: /${schema}/${table} — row: ${JSON.stringify(
      row
    ).slice(0, 100)}`;
    clickLog.value.unshift(message);
    if (clickLog.value.length > 10) clickLog.value.pop();
  },
});

watch([schemaId, tableId], ([newSchemaId, newTableId]) => {
  router.push({
    query: { schema: newSchemaId, table: newTableId },
  });
});
</script>

<template>
  <div class="p-5 space-y-8">
    <h1 class="text-2xl font-bold">DataList Component</h1>
    <p class="text-gray-600 dark:text-gray-400">
      Fetches paginated data from EMX2 backend and renders as TABLE, CARDS,
      LIST, or LINKS. Click any row to see the navigation event.
    </p>

    <div class="space-y-4 p-4 bg-gray-100 dark:bg-gray-800 rounded">
      <DemoDataControls
        v-model:metadata="metadata"
        v-model:schemaId="schemaId"
        v-model:tableId="tableId"
        v-model:formValues="formValues"
      />

      <div class="flex items-center gap-4 flex-wrap">
        <div class="flex items-center gap-2">
          <label for="layout" class="font-bold">Layout:</label>
          <select
            id="layout"
            v-model="layout"
            class="border border-black p-2 dark:bg-gray-700 dark:border-gray-600"
          >
            <option value="TABLE">TABLE</option>
            <option value="CARDS">CARDS</option>
            <option value="LIST">LIST</option>
            <option value="LINKS">LINKS</option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <label for="pageSize" class="font-bold">Page size:</label>
          <input
            id="pageSize"
            v-model.number="pageSize"
            type="number"
            min="1"
            max="100"
            class="border border-black p-2 w-20 dark:bg-gray-700 dark:border-gray-600"
          />
        </div>
      </div>
    </div>

    <div
      v-if="clickLog.length"
      class="p-4 bg-gray-50 dark:bg-gray-800 rounded border dark:border-gray-600"
    >
      <div class="flex justify-between items-center mb-2">
        <span class="font-medium">Navigation Log:</span>
        <button
          class="text-sm text-blue-600 hover:underline"
          @click="clickLog = []"
        >
          Clear
        </button>
      </div>
      <ul class="space-y-1 text-sm font-mono">
        <li
          v-for="(log, index) in clickLog"
          :key="index"
          class="text-gray-700 dark:text-gray-300"
        >
          {{ log }}
        </li>
      </ul>
    </div>

    <div class="p-4 border rounded dark:border-gray-600">
      <DataList
        :key="`${schemaId}-${tableId}-${layout}-${pageSize}`"
        :schema-id="schemaId"
        :table-id="tableId"
        :layout="layout"
        :page-size="pageSize"
      />
    </div>
  </div>
</template>

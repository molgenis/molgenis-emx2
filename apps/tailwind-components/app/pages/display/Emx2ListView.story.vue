<script setup lang="ts">
import { ref, watch, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import type {
  IColumn,
  IRow,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import DemoDataControls from "../../DemoDataControls.vue";
import Emx2ListView from "../../components/display/Emx2ListView.vue";
import RecordListView from "../../components/display/RecordListView.vue";

const router = useRouter();
const route = useRoute();

const schemaId = ref<string>((route.query.schema as string) || "pet store");
const tableId = ref<string>((route.query.table as string) || "Pet");
const metadata = ref<ITableMetaData>();

const showSearch = ref(true);
const pagingLimit = ref(5);
const useMockData = ref(false);
const clickLog = ref<string[]>([]);

// Update URL when schema/table changes
watch([schemaId, tableId], ([newSchemaId, newTableId]) => {
  router.push({
    query: {
      schema: newSchemaId,
      table: newTableId,
    },
  });
});

function getRefClickAction(col: IColumn, row: IRow) {
  return () => {
    const message = `Clicked: ${col.id || "row"} -> ${JSON.stringify(row)}`;
    clickLog.value.unshift(message);
    if (clickLog.value.length > 5) clickLog.value.pop();
  };
}

function clearLog() {
  clickLog.value = [];
}

// Mock data for offline testing
const mockRefColumn = {
  id: "pets",
  label: "Pets",
  columnType: "REF_ARRAY" as const,
  refTableId: "Pet",
  refSchemaId: "pet store",
  refLabel: "${name} - ${species}",
  refLabelDefault: "${name}",
  refLinkId: "name",
};

const mockRows: IRow[] = [
  { name: "Fluffy", species: "Dog", breed: "Golden Retriever" },
  { name: "Whiskers", species: "Cat", breed: "Persian" },
  { name: "Buddy", species: "Dog", breed: "Labrador" },
  { name: "Shadow", species: "Cat", breed: "Siamese" },
  { name: "Max", species: "Dog", breed: "German Shepherd" },
  { name: "Luna", species: "Cat", breed: "Maine Coon" },
  { name: "Rocky", species: "Dog", breed: "Bulldog" },
  { name: "Mittens", species: "Cat", breed: "British Shorthair" },
  { name: "Duke", species: "Dog", breed: "Beagle" },
  { name: "Simba", species: "Cat", breed: "Ragdoll" },
  { name: "Charlie", species: "Dog", breed: "Poodle" },
  { name: "Tiger", species: "Cat", breed: "Bengal" },
];

const mockPage = ref(1);
const mockVisibleRows = computed(() => {
  const start = (mockPage.value - 1) * pagingLimit.value;
  return mockRows.slice(start, start + pagingLimit.value);
});
</script>

<template>
  <div class="p-5 space-y-8">
    <h1 class="text-2xl font-bold">Emx2ListView Component</h1>
    <p class="text-gray-600 dark:text-gray-400">
      Fetches and displays a list of records from EMX2 backend with search and
      pagination.
    </p>

    <!-- Controls -->
    <div class="space-y-4 p-4 bg-gray-100 dark:bg-gray-800 rounded">
      <DemoDataControls
        v-model:metadata="metadata"
        v-model:schemaId="schemaId"
        v-model:tableId="tableId"
      />

      <div class="flex flex-wrap items-center gap-4">
        <div class="flex items-center gap-2">
          <input id="showSearch" v-model="showSearch" type="checkbox" />
          <label for="showSearch">Show search</label>
        </div>

        <div class="flex items-center gap-2">
          <label for="pagingLimit">Page size:</label>
          <select
            id="pagingLimit"
            v-model="pagingLimit"
            class="border border-black p-1 dark:bg-gray-700 dark:border-gray-600"
          >
            <option :value="3">3</option>
            <option :value="5">5</option>
            <option :value="10">10</option>
            <option :value="25">25</option>
          </select>
        </div>

        <div class="flex items-center gap-2">
          <input id="useMock" v-model="useMockData" type="checkbox" />
          <label for="useMock">Use mock data (offline mode)</label>
        </div>
      </div>
    </div>

    <!-- Click log -->
    <div
      class="p-4 bg-gray-50 dark:bg-gray-800 rounded border dark:border-gray-600"
    >
      <div class="flex justify-between items-center mb-2">
        <span class="font-medium">Click Event Log:</span>
        <button class="text-sm text-blue-600 hover:underline" @click="clearLog">
          Clear
        </button>
      </div>
      <div v-if="clickLog.length === 0" class="text-gray-400 italic">
        No clicks yet - click any item to see events
      </div>
      <ul v-else class="space-y-1 text-sm font-mono">
        <li
          v-for="(log, index) in clickLog"
          :key="index"
          class="text-gray-700 dark:text-gray-300"
        >
          {{ log }}
        </li>
      </ul>
    </div>

    <!-- Live Backend Data -->
    <div v-if="!useMockData" class="space-y-4">
      <h2 class="text-xl font-semibold">Live Backend Data</h2>
      <p class="text-sm text-gray-500">
        Connected to: {{ schemaId }} / {{ tableId }}
      </p>

      <div class="p-4 border rounded dark:border-gray-600">
        <Emx2ListView
          :key="`${schemaId}-${tableId}-${pagingLimit}`"
          :schema-id="schemaId"
          :table-id="tableId"
          :show-search="showSearch"
          :paging-limit="pagingLimit"
          :get-ref-click-action="getRefClickAction"
        />
      </div>
    </div>

    <!-- Mock Data (offline mode) -->
    <div v-else class="space-y-4">
      <h2 class="text-xl font-semibold">Mock Data (Offline Mode)</h2>
      <p class="text-sm text-gray-500">
        Using static mock data. Uncheck "Use mock data" to connect to backend.
      </p>

      <div class="p-4 border rounded dark:border-gray-600">
        <RecordListView
          :rows="mockVisibleRows"
          :ref-column="mockRefColumn"
          v-model:page="mockPage"
          :page-size="pagingLimit"
          :total-count="mockRows.length"
          :get-ref-click-action="getRefClickAction"
        />
      </div>
    </div>

    <!-- With filter example -->
    <div v-if="!useMockData" class="space-y-4">
      <h2 class="text-xl font-semibold">With Filter Example</h2>
      <p class="text-sm text-gray-500">
        Same component with a filter applied (if applicable to data)
      </p>

      <div class="p-4 border rounded dark:border-gray-600">
        <Emx2ListView
          :key="`${schemaId}-${tableId}-filtered-${pagingLimit}`"
          :schema-id="schemaId"
          :table-id="tableId"
          :show-search="false"
          :paging-limit="3"
          :get-ref-click-action="getRefClickAction"
        />
      </div>
    </div>

    <!-- Dark mode test -->
    <div class="space-y-4">
      <h2 class="text-xl font-semibold">Dark Mode Test</h2>
      <p class="text-sm text-gray-500">
        Toggle dark mode in your browser/OS to verify styling.
      </p>
      <div
        class="p-4 border rounded bg-white dark:bg-gray-900 dark:border-gray-600"
      >
        <RecordListView
          :rows="mockVisibleRows"
          :ref-column="mockRefColumn"
          v-model:page="mockPage"
          :page-size="pagingLimit"
          :total-count="mockRows.length"
          :get-ref-click-action="getRefClickAction"
        />
      </div>
    </div>
  </div>
</template>

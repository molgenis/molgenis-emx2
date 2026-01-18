<script setup lang="ts">
import { ref, watch, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import type {
  IColumn,
  IRow,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import DemoDataControls from "../../DemoDataControls.vue";
import Emx2RecordView from "../../components/display/Emx2RecordView.vue";
import RecordView from "../../components/display/RecordView.vue";

const router = useRouter();
const route = useRoute();

const schemaId = ref<string>((route.query.schema as string) || "pet store");
const tableId = ref<string>((route.query.table as string) || "Pet");
const metadata = ref<ITableMetaData>();
const formValues = ref<Record<string, any>>({});

const showEmpty = ref(false);
const clickLog = ref<string[]>([]);
const useMockData = ref(false);
const viewColumnsInput = ref("");

// Parse viewColumns from comma-separated input
const viewColumns = computed(() => {
  if (!viewColumnsInput.value.trim()) return undefined;
  return viewColumnsInput.value
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
});

// Extract rowId from formValues based on key columns
const rowId = computed(() => {
  if (!metadata.value || !formValues.value) return {};
  const keyColumns = metadata.value.columns?.filter((c) => c.key === 1) || [];
  const result: Record<string, any> = {};
  for (const col of keyColumns) {
    if (formValues.value[col.id] !== undefined) {
      result[col.id] = formValues.value[col.id];
    }
  }
  return result;
});

const hasRowId = computed(() => Object.keys(rowId.value).length > 0);

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
    const message = `Clicked: ${col.id} -> ${JSON.stringify(row)}`;
    clickLog.value.unshift(message);
    if (clickLog.value.length > 5) clickLog.value.pop();
  };
}

function clearLog() {
  clickLog.value = [];
}

// Mock data for when backend is unavailable
const mockMetadata: ITableMetaData = {
  id: "Pet",
  schemaId: "pet store",
  name: "Pet",
  label: "Pet",
  tableType: "DATA",
  columns: [
    { id: "name", label: "Name", columnType: "STRING", key: 1 },
    { id: "species", label: "Species", columnType: "STRING" },
    {
      id: "detailsSection",
      label: "Details",
      columnType: "SECTION",
      description: "Pet details",
    },
    {
      id: "breed",
      label: "Breed",
      columnType: "STRING",
      section: "detailsSection",
    },
    {
      id: "birthDate",
      label: "Birth Date",
      columnType: "DATE",
      section: "detailsSection",
    },
    {
      id: "weight",
      label: "Weight (kg)",
      columnType: "DECIMAL",
      section: "detailsSection",
    },
    {
      id: "ownerSection",
      label: "Owner Information",
      columnType: "SECTION",
    },
    {
      id: "owner",
      label: "Owner",
      columnType: "REF",
      section: "ownerSection",
      refTableId: "Owner",
      refSchemaId: "pet store",
    },
  ],
};

const mockRow = {
  name: "Fluffy",
  species: "Dog",
  breed: "Golden Retriever",
  birthDate: "2021-03-15",
  weight: 28.5,
  owner: { firstName: "John", lastName: "Doe" },
};
</script>

<template>
  <div class="p-5 space-y-8">
    <h1 class="text-2xl font-bold">Emx2RecordView Component</h1>
    <p class="text-gray-600 dark:text-gray-400">
      Fetches metadata and row data from EMX2 backend and renders using
      RecordView.
    </p>

    <!-- Controls -->
    <div class="space-y-4 p-4 bg-gray-100 dark:bg-gray-800 rounded">
      <DemoDataControls
        v-model:metadata="metadata"
        v-model:schemaId="schemaId"
        v-model:tableId="tableId"
        v-model:formValues="formValues"
        :include-row-select="true"
        :row-index="1"
      />

      <div class="flex flex-col gap-2">
        <label for="viewColumns" class="text-title font-bold">
          View Columns (comma-separated, leave empty for all):
        </label>
        <input
          id="viewColumns"
          v-model="viewColumnsInput"
          type="text"
          class="border border-black p-2 dark:bg-gray-700 dark:border-gray-600"
          placeholder="e.g., name, species, breed"
        />
      </div>

      <div class="flex items-center gap-4">
        <div class="flex items-center gap-2">
          <input id="showEmpty" v-model="showEmpty" type="checkbox" />
          <label for="showEmpty">Show empty values</label>
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
        No clicks yet - click any REF link to see events
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

      <div v-if="!hasRowId" class="p-4 border rounded dark:border-gray-600">
        <p class="text-gray-500 italic">
          Select a row above to view its details.
        </p>
      </div>

      <div v-else class="p-4 border rounded dark:border-gray-600">
        <Emx2RecordView
          :key="`${schemaId}-${tableId}-${JSON.stringify(rowId)}`"
          :schema-id="schemaId"
          :table-id="tableId"
          :row-id="rowId"
          :view-columns="viewColumns"
          :show-empty="showEmpty"
          :get-ref-click-action="getRefClickAction"
        >
          <template #header>
            <div class="mb-6 pb-4 border-b dark:border-gray-600">
              <h1 class="text-3xl font-bold">{{ tableId }} Record</h1>
              <p class="text-gray-500">Row ID: {{ JSON.stringify(rowId) }}</p>
            </div>
          </template>
          <template #footer>
            <div
              class="mt-6 pt-4 border-t dark:border-gray-600 text-sm text-gray-500"
            >
              Schema: {{ schemaId }} | Table: {{ tableId }}
            </div>
          </template>
        </Emx2RecordView>
      </div>
    </div>

    <!-- Mock Data (offline mode) -->
    <div v-else class="space-y-4">
      <h2 class="text-xl font-semibold">Mock Data (Offline Mode)</h2>
      <p class="text-sm text-gray-500">
        Using static mock data. Uncheck "Use mock data" to connect to backend.
      </p>

      <div class="p-4 border rounded dark:border-gray-600">
        <RecordView
          :metadata="mockMetadata"
          :row="mockRow"
          :show-empty="showEmpty"
          :get-ref-click-action="getRefClickAction"
        >
          <template #header>
            <div class="mb-6 pb-4 border-b dark:border-gray-600">
              <h1 class="text-3xl font-bold">{{ mockRow.name }}</h1>
              <p class="text-gray-500">Mock Pet Record</p>
            </div>
          </template>
        </RecordView>
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
        <RecordView
          :metadata="mockMetadata"
          :row="mockRow"
          :show-empty="showEmpty"
          :get-ref-click-action="getRefClickAction"
        />
      </div>
    </div>
  </div>
</template>

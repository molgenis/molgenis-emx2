<script setup lang="ts">
import { ref } from "vue";
import type {
  IColumn,
  ITableMetaData,
  IRefColumn,
} from "../../../../metadata-utils/src/types";
import RecordView from "../../components/display/RecordView.vue";

const showEmpty = ref(false);
const clickLog = ref<string[]>([]);

// Mock metadata with full hierarchy: SECTION > HEADING > columns
const mockMetadata: ITableMetaData = {
  id: "Pet",
  schemaId: "pet store",
  name: "Pet",
  label: "Pet",
  tableType: "DATA",
  columns: [
    // Orphan columns (no section/heading)
    { id: "id", label: "ID", columnType: "AUTO_ID" },
    { id: "name", label: "Name", columnType: "STRING" },

    // Section 1: General Info
    {
      id: "generalSection",
      label: "General Information",
      columnType: "SECTION",
      description: "Basic details about the pet",
    },
    {
      id: "species",
      label: "Species",
      columnType: "ONTOLOGY",
      section: "generalSection",
    },
    {
      id: "breed",
      label: "Breed",
      columnType: "STRING",
      section: "generalSection",
    },
    {
      id: "birthDate",
      label: "Birth Date",
      columnType: "DATE",
      section: "generalSection",
    },

    // Section 1 > Heading: Physical
    {
      id: "physicalHeading",
      label: "Physical Characteristics",
      columnType: "HEADING",
      section: "generalSection",
    },
    {
      id: "weight",
      label: "Weight (kg)",
      columnType: "DECIMAL",
      heading: "physicalHeading",
    },
    {
      id: "color",
      label: "Color",
      columnType: "STRING",
      heading: "physicalHeading",
    },

    // Section 2: Owner Info
    {
      id: "ownerSection",
      label: "Owner Information",
      columnType: "SECTION",
      description: "Details about the pet owner",
    },
    {
      id: "owner",
      label: "Owner",
      columnType: "REF",
      section: "ownerSection",
      refTableId: "Owner",
      refSchemaId: "pet store",
      refLabel: "${firstName} ${lastName}",
      refLabelDefault: "${firstName}",
      refLinkId: "id",
    } as IRefColumn,
    {
      id: "ownerContact",
      label: "Contact Email",
      columnType: "EMAIL",
      section: "ownerSection",
    },

    // Section 3: Related
    { id: "relatedSection", label: "Related Records", columnType: "SECTION" },
    {
      id: "orders",
      label: "Orders",
      columnType: "REF_ARRAY",
      section: "relatedSection",
      refTableId: "Order",
      refSchemaId: "pet store",
      refLabel: "${orderId} - ${status}",
      refLabelDefault: "${orderId}",
      refLinkId: "orderId",
    } as IRefColumn,
    {
      id: "tags",
      label: "Tags",
      columnType: "ONTOLOGY_ARRAY",
      section: "relatedSection",
    },
  ],
};

// Mock row data
const mockRow = {
  id: "PET-001",
  name: "Fluffy",
  species: { name: "Dog", definition: "A domesticated carnivorous mammal" },
  breed: "Golden Retriever",
  birthDate: "2021-03-15",
  weight: 28.5,
  color: "Golden",
  owner: { firstName: "John", lastName: "Doe", id: 1 },
  ownerContact: "john.doe@example.com",
  orders: Array.from({ length: 8 }, (_, i) => ({
    orderId: `ORD-${String(i + 1).padStart(3, "0")}`,
    status: ["Pending", "Shipped", "Delivered"][i % 3],
  })),
  tags: [
    { name: "Friendly", definition: "Good with people and other pets" },
    { name: "Trained", definition: "Has basic obedience training" },
    { name: "Vaccinated" },
  ],
};

// Row with some empty values
const mockRowWithEmpty = {
  ...mockRow,
  breed: null,
  weight: undefined,
  ownerContact: "",
  tags: [],
};

function getRefClickAction(col: IColumn, row: any) {
  return () => {
    const message = `Clicked: ${col.id} -> ${JSON.stringify(row)}`;
    clickLog.value.unshift(message);
    if (clickLog.value.length > 5) clickLog.value.pop();
  };
}

function clearLog() {
  clickLog.value = [];
}
</script>

<template>
  <div class="p-5 space-y-8">
    <h1 class="text-2xl font-bold">RecordView Component</h1>
    <p class="text-gray-600 dark:text-gray-400">
      Renders a complete record with two-level hierarchy: SECTION (text-2xl) >
      HEADING (text-xl) > columns. Orphan columns (no section/heading) appear
      first.
    </p>

    <div
      class="flex items-center gap-2 p-4 bg-gray-100 dark:bg-gray-800 rounded"
    >
      <input id="showEmpty" v-model="showEmpty" type="checkbox" />
      <label for="showEmpty">Show empty values</label>
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

    <!-- Full record view -->
    <div class="space-y-4">
      <h2 class="text-xl font-semibold">Complete Record View</h2>
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
              <p class="text-gray-500">Record ID: {{ mockRow.id }}</p>
            </div>
          </template>
          <template #footer>
            <div
              class="mt-6 pt-4 border-t dark:border-gray-600 text-sm text-gray-500"
            >
              Footer slot example - could contain actions, timestamps, etc.
            </div>
          </template>
        </RecordView>
      </div>
    </div>

    <!-- Record with empty values -->
    <div class="space-y-4">
      <h2 class="text-xl font-semibold">Record with Empty Values</h2>
      <p class="text-sm text-gray-500">
        Toggle "Show empty values" above to see hidden columns.
      </p>
      <div class="p-4 border rounded dark:border-gray-600">
        <RecordView
          :metadata="mockMetadata"
          :row="mockRowWithEmpty"
          :show-empty="showEmpty"
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

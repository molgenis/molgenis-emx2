<script setup lang="ts">
import { ref } from "vue";
import type { IColumn, IRefColumn } from "../../../../metadata-utils/src/types";
import type { ISectionColumn } from "../../../types/types";
import RecordSection from "../../components/display/RecordSection.vue";

const showEmpty = ref(false);
const clickLog = ref<string[]>([]);

// Heading columns
const sectionHeading: IColumn = {
  id: "general",
  label: "General Information",
  columnType: "SECTION",
  description: "Basic details about the record",
};

const heading: IColumn = {
  id: "contact",
  label: "Contact Details",
  columnType: "HEADING",
  description: "How to reach the owner",
};

// Sample columns for the section
const sampleColumns: ISectionColumn[] = [
  {
    meta: { id: "name", label: "Name", columnType: "STRING" },
    value: "Fluffy",
  },
  {
    meta: { id: "age", label: "Age", columnType: "INT" },
    value: 3,
  },
  {
    meta: { id: "species", label: "Species", columnType: "ONTOLOGY" },
    value: { name: "Dog", definition: "A domesticated carnivorous mammal" },
  },
  {
    meta: {
      id: "owner",
      label: "Owner",
      columnType: "REF",
      refTableId: "Owner",
      refSchemaId: "pet store",
      refLabel: "${firstName} ${lastName}",
      refLabelDefault: "${firstName}",
      refLinkId: "id",
    } as IRefColumn,
    value: { firstName: "John", lastName: "Doe", id: 1 },
  },
];

// Sample columns including REF_ARRAY (to test full-width layout)
const columnsWithLists: ISectionColumn[] = [
  {
    meta: { id: "name", label: "Pet Name", columnType: "STRING" },
    value: "Buddy",
  },
  {
    meta: { id: "breed", label: "Breed", columnType: "STRING" },
    value: "Golden Retriever",
  },
  {
    meta: {
      id: "vaccinations",
      label: "Vaccinations",
      columnType: "REF_ARRAY",
      refTableId: "Vaccination",
      refSchemaId: "pet store",
      refLabel: "${name} (${date})",
      refLabelDefault: "${name}",
      refLinkId: "id",
    } as IRefColumn,
    value: [
      { id: 1, name: "Rabies", date: "2024-01-15" },
      { id: 2, name: "Distemper", date: "2024-02-20" },
      { id: 3, name: "Parvovirus", date: "2024-03-10" },
    ],
  },
  {
    meta: {
      id: "visits",
      label: "Vet Visits",
      columnType: "REFBACK",
      refTableId: "Visit",
      refSchemaId: "pet store",
      refLabel: "${date} - ${reason}",
      refLabelDefault: "${date}",
      refLinkId: "id",
    } as IRefColumn,
    value: [
      { id: 1, date: "2024-01-10", reason: "Annual checkup" },
      { id: 2, date: "2024-04-05", reason: "Ear infection" },
      { id: 3, date: "2024-06-20", reason: "Vaccination" },
      { id: 4, date: "2024-08-15", reason: "Dental cleaning" },
      { id: 5, date: "2024-10-01", reason: "Follow-up" },
      { id: 6, date: "2024-11-12", reason: "Skin allergy" },
    ],
  },
];

// Columns with some empty values
const columnsWithEmpty: ISectionColumn[] = [
  {
    meta: { id: "name", label: "Name", columnType: "STRING" },
    value: "Max",
  },
  {
    meta: { id: "nickname", label: "Nickname", columnType: "STRING" },
    value: null,
  },
  {
    meta: { id: "description", label: "Description", columnType: "TEXT" },
    value: "",
  },
  {
    meta: { id: "tags", label: "Tags", columnType: "ONTOLOGY_ARRAY" },
    value: [],
  },
];

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
    <h1 class="text-2xl font-bold">RecordSection Component</h1>
    <p class="text-gray-600 dark:text-gray-400">
      Groups related columns with an optional heading. Sections use larger fonts
      (text-2xl), headings use smaller (text-xl).
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
        No clicks yet
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

    <!-- Section with SECTION type heading (larger font) -->
    <div class="space-y-4">
      <h2 class="text-xl font-semibold">Section Type (text-2xl heading)</h2>
      <div class="p-4 border rounded dark:border-gray-600">
        <RecordSection
          :heading="sectionHeading"
          :is-section="true"
          :columns="sampleColumns"
          :show-empty="showEmpty"
          :get-ref-click-action="getRefClickAction"
        />
      </div>
    </div>

    <!-- Section with HEADING type heading (smaller font) -->
    <div class="space-y-4">
      <h2 class="text-xl font-semibold">Heading Type (text-xl heading)</h2>
      <div class="p-4 border rounded dark:border-gray-600">
        <RecordSection
          :heading="heading"
          :is-section="false"
          :columns="sampleColumns"
          :show-empty="showEmpty"
          :get-ref-click-action="getRefClickAction"
        />
      </div>
    </div>

    <!-- Section without heading -->
    <div class="space-y-4">
      <h2 class="text-xl font-semibold">No Heading (orphan columns)</h2>
      <div class="p-4 border rounded dark:border-gray-600">
        <RecordSection
          :heading="null"
          :columns="sampleColumns"
          :show-empty="showEmpty"
          :get-ref-click-action="getRefClickAction"
        />
      </div>
    </div>

    <!-- Section with REF_ARRAY/REFBACK (full-width list layout) -->
    <div class="space-y-4">
      <h2 class="text-xl font-semibold">
        With REF_ARRAY/REFBACK (full-width lists)
      </h2>
      <p class="text-sm text-gray-500">
        List columns (REF_ARRAY, REFBACK) are rendered full-width below regular
        columns. The second list has pagination (6 items, page size 5).
      </p>
      <div class="p-4 border rounded dark:border-gray-600">
        <RecordSection
          :heading="sectionHeading"
          :is-section="true"
          :columns="columnsWithLists"
          :show-empty="showEmpty"
          :get-ref-click-action="getRefClickAction"
        />
      </div>
    </div>

    <!-- Section with empty values -->
    <div class="space-y-4">
      <h2 class="text-xl font-semibold">With Empty Values</h2>
      <p class="text-sm text-gray-500">
        Toggle "Show empty values" above to see hidden columns.
      </p>
      <div class="p-4 border rounded dark:border-gray-600">
        <RecordSection
          :heading="sectionHeading"
          :is-section="true"
          :columns="columnsWithEmpty"
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
        <RecordSection
          :heading="sectionHeading"
          :is-section="true"
          :columns="sampleColumns"
          :show-empty="showEmpty"
          :get-ref-click-action="getRefClickAction"
        />
      </div>
    </div>
  </div>
</template>

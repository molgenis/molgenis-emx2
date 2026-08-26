<template>
  <Story
    title="Value"
    description="Every column type, rendered through value/EMX2.vue."
  >
    <div class="flex flex-col gap-4">
      <p class="text-body-base">
        A row showing its own type name in place of a value has no branch in
        <code>EMX2.vue</code> and falls through. HEADING and SECTION do that by
        design: they are layout, and <code>tableQuery.ts</code> filters them out
        before a query is built.
      </p>

      <div
        v-for="{ columnType, column, data } in cases"
        :key="columnType"
        class="grid grid-cols-[14rem_1fr] gap-4 border-b border-gray-200 py-2"
      >
        <h4 class="text-heading-sm font-bold">{{ columnType }}</h4>
        <div><ValueEMX2 :metadata="column" :data="data" /></div>
      </div>
    </div>
  </Story>
</template>

<script setup lang="ts">
import { computed } from "vue";
import ValueEMX2 from "../components/value/EMX2.vue";
import type { ColumnType, IColumn } from "../../../metadata-utils/src/types";

interface Case {
  data: unknown;
  column?: Partial<IColumn>;
}

const LOREM =
  "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod " +
  "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim " +
  "veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea " +
  "commodo consequat. Duis aute irure dolor in reprehenderit in voluptate " +
  "velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat " +
  "cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id " +
  "est laborum. Sed ut perspiciatis unde omnis iste natus error sit " +
  "voluptatem accusantium doloremque laudantium, totam rem aperiam.";

const refTable = { refSchemaId: "pet store", refTableId: "Person" };
const ontologyTable = { refSchemaId: "pet store", refTableId: "Diagnosis" };

const people = [
  "Alice",
  "Bob",
  "Carol",
  "Dave",
  "Erin",
  "Frank",
  "Grace",
  "Heidi",
].map((name) => ({ name }));

const terms = [
  ["Fever", "Elevated body temperature."],
  ["Cough", "Sudden expulsion of air from the lungs."],
  ["Fatigue", "A feeling of tiredness."],
  ["Nausea", "A feeling of sickness."],
  ["Headache", "Pain in the head or upper neck."],
  ["Chills", "A feeling of coldness with shivering."],
  ["Dizziness", "A sensation of spinning."],
  ["Rash", "A change in skin colour or texture."],
].map(([name, definition]) => ({ name, definition }));

// Keyed by ColumnType, so the compiler fails this page when a type is added
// and left uncovered.
const byType: Record<ColumnType, Case> = {
  STRING: { data: "Pet store" },
  STRING_ARRAY: {
    data: ["red", "green", "blue", "yellow", "purple", "orange", "pink"],
  },
  TEXT: { data: LOREM },
  TEXT_ARRAY: { data: ["First note.", "Second note.", "Third note."] },
  AUTO_ID: { data: "PS-000042" },
  UUID: { data: "550e8400-e29b-41d4-a716-446655440000" },
  UUID_ARRAY: {
    data: [
      "550e8400-e29b-41d4-a716-446655440000",
      "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed",
    ],
  },
  BOOL: { data: true },
  BOOL_ARRAY: { data: [true, false, true] },
  INT: { data: 42 },
  INT_ARRAY: { data: [1, 1, 2, 3, 5, 8, 13] },
  NON_NEGATIVE_INT: { data: 7 },
  NON_NEGATIVE_INT_ARRAY: { data: [0, 1, 2, 3, 4, 5, 6] },
  LONG: { data: "9007199254740993" },
  LONG_ARRAY: { data: ["9007199254740993", "9007199254740994"] },
  DECIMAL: { data: 3.14159 },
  DECIMAL_ARRAY: { data: [1.5, 2.25, 3.125] },
  DATE: { data: "2024-03-15" },
  DATE_ARRAY: { data: ["2024-03-15", "2024-06-01", "2024-12-24"] },
  DATETIME: { data: "2024-03-15T14:32:00Z" },
  DATETIME_ARRAY: {
    data: ["2024-03-15T14:32:00Z", "2024-06-01T09:00:00Z"],
  },
  PERIOD: { data: "P1Y2M" },
  PERIOD_ARRAY: { data: ["P1Y", "P6M", "P30D"] },
  EMAIL: { data: "alice@example.org" },
  EMAIL_ARRAY: { data: ["alice@example.org", "bob@example.org"] },
  HYPERLINK: { data: "https://molgenis.org" },
  HYPERLINK_ARRAY: {
    data: ["https://molgenis.org", "https://github.com/molgenis"],
  },
  FILE: {
    data: {
      id: "f1",
      filename: "protocol.pdf",
      extension: "pdf",
      size: 348_213,
      url: "/api/file/f1",
    },
  },
  JSON: { data: { key: "value", nested: { count: 2 } } },
  REF: {
    column: { ...refTable, refLabel: "${name}" },
    data: { name: "Alice" },
  },
  REF_ARRAY: { column: { ...refTable, refLabel: "${name}" }, data: people },
  REFBACK: {
    column: {
      refSchemaId: "pet store",
      refTableId: "Visit",
      refLabel: "${date} - ${reason}",
    },
    data: [
      { date: "2023-01-10", reason: "Checkup" },
      { date: "2023-06-22", reason: "Vaccination" },
    ],
  },
  RADIO: {
    column: { ...refTable, refLabel: "${name}" },
    data: { name: "Bob" },
  },
  SELECT: {
    column: { ...refTable, refLabel: "${name}" },
    data: { name: "Carol" },
  },
  CHECKBOX: { column: { ...refTable, refLabel: "${name}" }, data: people },
  MULTISELECT: { column: { ...refTable, refLabel: "${name}" }, data: people },
  // No refLabel, so this exercises the fallback rendering of a ref object.
  ONTOLOGY: {
    column: ontologyTable,
    data: {
      name: "Diabetes",
      definition: "A metabolic disease that causes high blood sugar.",
      code: "E11",
      codesystem: "ICD10",
    },
  },
  ONTOLOGY_ARRAY: { column: ontologyTable, data: terms },
  HEADING: { data: "Contact details" },
  SECTION: { data: "Clinical" },
};

const cases = computed(() =>
  (Object.keys(byType) as ColumnType[]).map((columnType) => ({
    columnType,
    data: byType[columnType].data,
    column: {
      ...byType[columnType].column,
      columnType,
      id: columnType.toLowerCase(),
      label: columnType,
    },
  }))
);
</script>

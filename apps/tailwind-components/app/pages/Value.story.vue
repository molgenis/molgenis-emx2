<template>
  <div
    class="flex flex-col gap-4"
    :class="
      onContentSurface
        ? 'bg-content text-title-contrast p-6 rounded-base'
        : 'text-title surface-inverted'
    "
  >
    <div class="flex flex-wrap items-end gap-6 border-b border-gray-200 pb-4">
      <div class="flex flex-col gap-1">
        <InputLabel :for="'values-per-array'">Values per array</InputLabel>
        <InputInt id="values-per-array" v-model="itemCount" class="w-32" />
      </div>
      <div class="flex flex-col gap-1">
        <InputLabel :for="'lines-when-collapsed'">
          Lines when collapsed
        </InputLabel>
        <InputInt id="lines-when-collapsed" v-model="maxLines" class="w-32" />
      </div>
      <div class="flex items-center gap-2">
        <InputCheckbox id="collapse" v-model="collapse" />
        <InputLabel :for="'collapse'">Collapse</InputLabel>
      </div>
      <div class="flex items-center gap-2">
        <InputCheckbox id="content-surface" v-model="onContentSurface" />
        <InputLabel :for="'content-surface'">On a content surface</InputLabel>
      </div>
      <div class="flex flex-col gap-1">
        <InputLabel :for="'values-rendered'"
          >Values rendered at a time</InputLabel
        >
        <InputInt id="values-rendered" v-model="renderLimit" class="w-32" />
      </div>
    </div>
    <p class="text-body-base">
      A long value is bounded by height and revealed with <b>show more</b>. It
      is never cut out of the page, so a search engine and the browser's own
      find still reach it.
    </p>

    <div
      v-for="{ columnType, column, data } in cases"
      :key="columnType"
      class="grid grid-cols-[14rem_1fr] gap-4 border-b border-gray-200 py-2"
    >
      <h4 class="text-heading-sm font-bold">{{ columnType }}</h4>
      <div class="min-w-0">
        <ValueEMX2
          :metadata="column"
          :data="data"
          :maxLines="lines"
          :renderLimit="tranche"
          :collapse="collapse"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import ValueEMX2 from "../components/value/EMX2.vue";
import type { ColumnType, IColumn } from "../../../metadata-utils/src/types";

interface Case {
  data: unknown;
  column?: Partial<IColumn>;
}

const itemCount = ref<number | string>(8);
const maxLines = ref<number | string>(3);
const renderLimit = ref<number | string>(1000);

const count = computed(() => Math.max(0, Number(itemCount.value) || 0));
const lines = computed(() => Math.max(1, Number(maxLines.value) || 1));
const tranche = computed(() => Math.max(1, Number(renderLimit.value) || 1));
// Values carry the content surface's link colour and do not adapt to a surface the
// theme colours itself. The switch is what shows that.
const onContentSurface = ref(true);
// A table cell turns this off: it bounds the value itself and opens a popup.
const collapse = ref(true);

function repeatTo<T>(base: T[], count: number): T[] {
  if (base.length === 0 || count <= 0) return [];
  return Array.from(
    { length: count },
    (_, index) => base[index % base.length] as T
  );
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

const peopleBase = [
  "Alice",
  "Bob",
  "Carol",
  "Dave",
  "Erin",
  "Frank",
  "Grace",
  "Heidi",
].map((name) => ({ name }));

const termsBase = [
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
const byType = computed<Record<ColumnType, Case>>(() => {
  const n = count.value;
  const people = repeatTo(peopleBase, n);
  const terms = repeatTo(termsBase, n);
  const grow = <T>(base: T[]) => repeatTo(base, n);
  return {
    STRING: { data: "Pet store" },
    STRING_ARRAY: {
      data: grow([
        "red",
        "green",
        "blue",
        "yellow",
        "purple",
        "orange",
        "pink",
      ]),
    },
    TEXT: { data: LOREM },
    TEXT_ARRAY: { data: grow(["First note.", "Second note.", "Third note."]) },
    AUTO_ID: { data: "PS-000042" },
    UUID: { data: "550e8400-e29b-41d4-a716-446655440000" },
    UUID_ARRAY: {
      data: grow([
        "550e8400-e29b-41d4-a716-446655440000",
        "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed",
      ]),
    },
    BOOL: { data: true },
    BOOL_ARRAY: { data: grow([true, false, true]) },
    INT: { data: 42 },
    INT_ARRAY: { data: grow([1, 1, 2, 3, 5, 8, 13]) },
    NON_NEGATIVE_INT: { data: 7 },
    NON_NEGATIVE_INT_ARRAY: { data: grow([0, 1, 2, 3, 4, 5, 6]) },
    LONG: { data: "9007199254740993" },
    LONG_ARRAY: { data: grow(["9007199254740993", "9007199254740994"]) },
    DECIMAL: { data: 3.14159 },
    DECIMAL_ARRAY: { data: grow([1.5, 2.25, 3.125]) },
    DATE: { data: "2024-03-15" },
    DATE_ARRAY: { data: grow(["2024-03-15", "2024-06-01", "2024-12-24"]) },
    DATETIME: { data: "2024-03-15T14:32:00Z" },
    DATETIME_ARRAY: {
      data: grow(["2024-03-15T14:32:00Z", "2024-06-01T09:00:00Z"]),
    },
    PERIOD: { data: "P1Y2M" },
    PERIOD_ARRAY: { data: grow(["P1Y", "P6M", "P30D"]) },
    EMAIL: { data: "alice@example.org" },
    EMAIL_ARRAY: { data: grow(["alice@example.org", "bob@example.org"]) },
    HYPERLINK: { data: "https://molgenis.org" },
    HYPERLINK_ARRAY: {
      data: grow(["https://molgenis.org", "https://github.com/molgenis"]),
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
});

const cases = computed(() =>
  (Object.keys(byType.value) as ColumnType[]).map((columnType) => ({
    columnType,
    data: byType.value[columnType].data,
    column: {
      ...byType.value[columnType].column,
      columnType,
      id: columnType.toLowerCase(),
      label: columnType,
    },
  }))
);
</script>

<script setup lang="ts">
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";
import DataTable from "../../components/display/DataTable.vue";
import { provideRecordNavigation } from "../../composables/useRecordNavigation";
import { ref } from "vue";

const clickLog = ref<string[]>([]);

provideRecordNavigation({
  async navigateToRecord(schema, table, row) {
    const message = `Navigate: /${schema}/${table} — ${JSON.stringify(
      row
    ).slice(0, 80)}`;
    clickLog.value.unshift(message);
    if (clickLog.value.length > 8) clickLog.value.pop();
  },
});

const personColumns: IColumn[] = [
  { id: "name", label: "Name", columnType: "STRING", key: 1, role: "TITLE" },
  { id: "age", label: "Age", columnType: "INT" },
  { id: "email", label: "Email", columnType: "EMAIL" },
];

const personRows: IRow[] = [
  { name: "Alice Jansen", age: 34, email: "alice@example.com" },
  { name: "Bob de Vries", age: 28, email: "bob@example.com" },
  { name: "Carol Bakker", age: 45, email: "carol@example.com" },
];

const cohortColumns: IColumn[] = [
  { id: "id", label: "Cohort ID", columnType: "STRING", key: 1 },
  { id: "name", label: "Name", columnType: "STRING", role: "TITLE" },
  { id: "country", label: "Country", columnType: "STRING" },
  { id: "participants", label: "Participants", columnType: "INT" },
];

const cohortRows: IRow[] = [
  {
    id: "AMST-01",
    name: "Amsterdam Cohort",
    country: "Netherlands",
    participants: 12450,
  },
  {
    id: "ROTT-02",
    name: "Rotterdam Study",
    country: "Netherlands",
    participants: 8900,
  },
  {
    id: "LOND-03",
    name: "UK Biobank",
    country: "United Kingdom",
    participants: 500000,
  },
  {
    id: "BERL-04",
    name: "Berlin Health Study",
    country: "Germany",
    participants: 3200,
  },
];

const customActionColumns: IColumn[] = [
  { id: "title", label: "Title", columnType: "STRING", key: 1 },
  { id: "status", label: "Status", columnType: "STRING" },
];

const customActionRows: IRow[] = [
  { title: "Publication A", status: "Published" },
  { title: "Publication B", status: "Draft" },
];

function handleTitleClick(col: IColumn, row: IRow) {
  clickLog.value.unshift(`Column click: "${col.id}" → ${row.title}`);
  if (clickLog.value.length > 8) clickLog.value.pop();
}
</script>

<template>
  <div class="p-5 space-y-10">
    <h1 class="text-2xl font-bold">DataTable Component</h1>
    <p class="text-body-muted">
      Renders tabular data. The TITLE (or first key) column is the link column.
      Rows are clickable when schemaId and tableId are provided.
    </p>

    <div
      v-if="clickLog.length"
      class="p-4 bg-surface-subtle rounded border border-divider"
    >
      <div class="flex justify-between items-center mb-2">
        <span class="font-medium text-sm">Event Log:</span>
        <button
          class="text-sm text-link hover:underline"
          @click="clickLog = []"
        >
          Clear
        </button>
      </div>
      <ul class="space-y-1 text-sm font-mono">
        <li
          v-for="(entry, index) in clickLog"
          :key="index"
          class="text-body-base"
        >
          {{ entry }}
        </li>
      </ul>
    </div>

    <section class="space-y-2">
      <h2 class="text-lg font-semibold">Empty state</h2>
      <p class="text-sm text-body-muted">No rows → shows "No items" row</p>
      <div class="border border-divider rounded p-4">
        <DataTable :columns="personColumns" :rows="[]" />
      </div>
    </section>

    <section class="space-y-2">
      <h2 class="text-lg font-semibold">Basic — read-only rows</h2>
      <p class="text-sm text-body-muted">
        No schemaId/tableId — link column renders as plain bold text
      </p>
      <div class="border border-divider rounded p-4">
        <DataTable :columns="personColumns" :rows="personRows" />
      </div>
    </section>

    <section class="space-y-2">
      <h2 class="text-lg font-semibold">Clickable rows (schemaId + tableId)</h2>
      <p class="text-sm text-body-muted">
        TITLE column becomes a link; clicking a row fires navigateToRecord
      </p>
      <div class="border border-divider rounded p-4">
        <DataTable
          :columns="cohortColumns"
          :rows="cohortRows"
          schema-id="catalogue"
          table-id="Cohorts"
        />
      </div>
    </section>

    <section class="space-y-2">
      <h2 class="text-lg font-semibold">
        Custom column action via columnConfig
      </h2>
      <p class="text-sm text-body-muted">
        columnConfig.title.clickAction overrides row navigation for the title
        column
      </p>
      <div class="border border-divider rounded p-4">
        <DataTable
          :columns="customActionColumns"
          :rows="customActionRows"
          :column-config="{
            title: { clickAction: handleTitleClick },
          }"
        />
      </div>
    </section>

    <section class="space-y-2">
      <h2 class="text-lg font-semibold">Actions slot</h2>
      <p class="text-sm text-body-muted">
        The #actions slot renders before the link column value in each row
      </p>
      <div class="border border-divider rounded p-4">
        <DataTable :columns="personColumns" :rows="personRows">
          <template #actions="{ row }">
            <span
              class="text-xs text-link cursor-pointer hover:underline"
              @click="clickLog.unshift(`Action: ${row.name}`)"
            >
              [edit]
            </span>
          </template>
        </DataTable>
      </div>
    </section>
  </div>
</template>

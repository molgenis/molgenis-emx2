<script setup lang="ts">
import { ref } from "vue";
import DataLinks from "../../components/display/DataLinks.vue";
import { provideRecordNavigation } from "../../composables/useRecordNavigation";

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

const cohortRows = [
  { id: "AMST-01", name: "Amsterdam Cohort" },
  { id: "ROTT-02", name: "Rotterdam Study" },
  { id: "LOND-03", name: "UK Biobank" },
];

const publicationRows = [
  { pmid: "38123456", title: "Genome-wide study of BMI", year: 2024 },
  { pmid: "37654321", title: "Longitudinal cohort analysis", year: 2023 },
  { pmid: "36987654", title: "Multi-omics integration review", year: 2022 },
];

const personRows = [
  { firstName: "Alice", lastName: "Jansen", email: "alice@example.com" },
  { firstName: "Bob", lastName: "de Vries", email: "bob@example.com" },
  { firstName: "Carol", lastName: "Bakker", email: "carol@example.com" },
];
</script>

<template>
  <div class="p-5 space-y-10">
    <h1 class="text-2xl font-bold">DataLinks Component</h1>
    <p class="text-body-muted">
      Renders a bulleted list of rows. Each row is a link when schemaId and
      tableId are provided. The label is built from rowLabelTemplate.
    </p>

    <div
      v-if="clickLog.length"
      class="p-4 bg-surface-subtle rounded border border-divider"
    >
      <div class="flex justify-between items-center mb-2">
        <span class="font-medium text-sm">Navigation Log:</span>
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
      <p class="text-sm text-body-muted">
        No rows → shows "No items" italic text
      </p>
      <div class="border border-divider rounded p-4">
        <DataLinks :rows="[]" schema-id="catalogue" table-id="Cohorts" />
      </div>
    </section>

    <section class="space-y-2">
      <h2 class="text-lg font-semibold">
        Clickable links — default label (id or name)
      </h2>
      <p class="text-sm text-body-muted">
        When schemaId + tableId are set each row is an anchor. The default label
        falls back to row.id → row.name → JSON.
      </p>
      <div class="border border-divider rounded p-4">
        <DataLinks
          :rows="cohortRows"
          schema-id="catalogue"
          table-id="Cohorts"
        />
      </div>
    </section>

    <section class="space-y-2">
      <h2 class="text-lg font-semibold">Clickable links — rowLabelTemplate</h2>
      <p class="text-sm text-body-muted">
        Template "${pmid}: ${title} (${year})" interpolates row fields into the
        label text
      </p>
      <div class="border border-divider rounded p-4">
        <DataLinks
          :rows="publicationRows"
          row-label-template="${pmid}: ${title} (${year})"
          schema-id="catalogue"
          table-id="Publications"
        />
      </div>
    </section>

    <section class="space-y-2">
      <h2 class="text-lg font-semibold">Static text — no navigation</h2>
      <p class="text-sm text-body-muted">
        Without schemaId/tableId the rows render as plain text spans, not links
      </p>
      <div class="border border-divider rounded p-4">
        <DataLinks
          :rows="personRows"
          row-label-template="${firstName} ${lastName}"
        />
      </div>
    </section>

    <section class="space-y-2">
      <h2 class="text-lg font-semibold">Multi-field template</h2>
      <p class="text-sm text-body-muted">
        Combining multiple fields with navigation enabled
      </p>
      <div class="border border-divider rounded p-4">
        <DataLinks
          :rows="personRows"
          row-label-template="${firstName} ${lastName} — ${email}"
          schema-id="catalogue"
          table-id="Contacts"
        />
      </div>
    </section>
  </div>
</template>

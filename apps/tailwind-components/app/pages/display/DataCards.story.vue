<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
import DataCards from "../../components/display/DataCards.vue";

const columns: IColumn[] = [
  { id: "name", label: "Name", columnType: "STRING", key: 1, role: "TITLE" },
  {
    id: "description",
    label: "Description",
    columnType: "TEXT",
    key: 0,
    role: "DESCRIPTION",
  },
  { id: "type", label: "Type", columnType: "STRING", key: 0, role: "DETAIL" },
  {
    id: "contact",
    label: "Contact",
    columnType: "STRING",
    key: 0,
    role: "DETAIL",
  },
];

const rows = [
  {
    name: "Biobank Amsterdam",
    description: "A large cohort of patients in Amsterdam.",
    type: "Population-based",
    contact: "info@biobank-ams.nl",
  },
  {
    name: "Rotterdam Study",
    description: "Long-running prospective cohort in Rotterdam.",
    type: "Prospective",
    contact: "info@erasmus.nl",
  },
];

const rowsWithRefs = [
  {
    name: "Genomics Study",
    description: "Genomics research cohort",
    type: { name: "Observational" },
    contact: null,
  },
  {
    name: "Multi-center Trial",
    description: null,
    type: "Interventional",
    contact: [{ name: "Alice" }, { name: "Bob" }],
  },
];

const minimalRows = [{ name: "Simple Entry" }, { name: "Another Entry" }];

const spec = `
## Features
- Renders a grid of cards from a rows array
- Inlines title, description, and detail fields per row
- Supports 1- or 2-column grid layout
- Optional href links per row via schemaId + tableId
- Shows "No items" when rows is empty

## Props
| Prop | Type | Default |
|------|------|---------|
| rows | Record<string, any>[] | required |
| columns | IColumn[] | undefined |
| gridColumns | 1 \| 2 | 2 |
| rowLabelTemplate | string | undefined |
| schemaId | string | undefined |
| tableId | string | undefined |

## Test Checklist
- [ ] Cards render in 2-col grid by default
- [ ] Cards render in 1-col grid when gridColumns=1
- [ ] Title links when schemaId + tableId provided
- [ ] Title plain text when no schemaId/tableId
- [ ] Description shown when DESCRIPTION column present
- [ ] Detail columns shown as label-value pairs
- [ ] Ref object values render correctly
- [ ] No dl rendered when no columns
- [ ] "No items" shown for empty rows
`;
</script>

<template>
  <Story title="DataCards" :spec="spec">
    <div class="p-5 space-y-6">
      <div
        class="bg-content p-6 rounded shadow-primary space-y-6 text-record-value"
      >
        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">
            With all fields (2-col grid, with links)
          </h2>
          <DataCards
            :rows="rows"
            :columns="columns"
            schema-id="testSchema"
            table-id="Cohort"
          />
        </div>

        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">
            With ref values
          </h2>
          <DataCards
            :rows="rowsWithRefs"
            :columns="columns"
            schema-id="testSchema"
            table-id="Cohort"
          />
        </div>

        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">
            Without columns (no detail list)
          </h2>
          <DataCards :rows="minimalRows" />
        </div>

        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">
            Single column layout
          </h2>
          <DataCards :rows="rows" :columns="columns" :grid-columns="1" />
        </div>

        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">Empty state</h2>
          <DataCards :rows="[]" :columns="columns" />
        </div>
      </div>
    </div>
  </Story>
</template>

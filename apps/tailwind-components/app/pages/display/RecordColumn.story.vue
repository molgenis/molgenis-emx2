<script setup lang="ts">
import { ref, computed } from "vue";
import type {
  IColumn,
  IRefColumn,
  IRow,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import DisplayRecordColumn from "../../components/display/RecordColumn.vue";

const showEmpty = ref(false);
const clickLog = ref<string[]>([]);

// Click handler for logging
function handleRefClick(col: IColumn, row: IRow) {
  const message = `Clicked REF: column="${col.id}", value=${JSON.stringify(
    row
  )}`;
  clickLog.value.unshift(message);
  if (clickLog.value.length > 5) {
    clickLog.value.pop();
  }
}

const stringColumn: IColumn = {
  id: "name",
  label: "Name",
  columnType: "STRING",
};

const intColumn: IColumn = {
  id: "age",
  label: "Age",
  columnType: "INT",
};

const boolColumn: IColumn = {
  id: "active",
  label: "Active",
  columnType: "BOOL",
};

const decimalColumn: IColumn = {
  id: "price",
  label: "Price",
  columnType: "DECIMAL",
};

const dateColumn: IColumn = {
  id: "birthDate",
  label: "Birth Date",
  columnType: "DATE",
};

const textColumn: IColumn = {
  id: "description",
  label: "Description",
  columnType: "TEXT",
};

const emailColumn: IColumn = {
  id: "email",
  label: "Email",
  columnType: "EMAIL",
};

const hyperlinkColumn: IColumn = {
  id: "website",
  label: "Website",
  columnType: "HYPERLINK",
};

const refColumn = computed<IRefColumn>(() => ({
  id: "pet",
  label: "Pet",
  columnType: "REF",
  refTableId: "Pet",
  refSchemaId: "pet store",
  refLabel: "${name}",
  refLabelDefault: "${name}",
  refLinkId: "name",
  displayConfig: { clickAction: handleRefClick },
}));

// RADIO and SELECT render same as REF (single object value)
const radioColumn: IColumn = {
  id: "status",
  label: "Status",
  columnType: "RADIO",
  refLabel: "${name}",
  refLabelDefault: "${name}",
};

const selectColumn: IColumn = {
  id: "priority",
  label: "Priority",
  columnType: "SELECT",
  refLabel: "${name}",
  refLabelDefault: "${name}",
};

// CHECKBOX and MULTISELECT render same as REF_ARRAY (array of objects)
const checkboxColumn: IColumn = {
  id: "features",
  label: "Features",
  columnType: "CHECKBOX",
  refLabel: "${name}",
  refLabelDefault: "${name}",
};

const multiselectColumn: IColumn = {
  id: "categories",
  label: "Categories",
  columnType: "MULTISELECT",
  refLabel: "${name}",
  refLabelDefault: "${name}",
};

const refArrayColumn = computed<IRefColumn>(() => ({
  id: "pets",
  label: "Pets",
  columnType: "REF_ARRAY",
  refTableId: "Pet",
  refSchemaId: "pet store",
  refLabel: "${name}",
  refLabelDefault: "${name}",
  refLinkId: "name",
  displayConfig: { clickAction: handleRefClick },
}));

const refArrayLargeColumn = computed<IRefColumn>(() => ({
  id: "orders",
  label: "Orders (15 items, paginated)",
  columnType: "REF_ARRAY",
  refTableId: "Order",
  refSchemaId: "pet store",
  refLabel: "${orderId} - ${status}",
  refLabelDefault: "${orderId}",
  refLinkId: "orderId",
  displayConfig: { clickAction: handleRefClick },
}));

const refBackColumn = computed<IRefColumn>(() => ({
  id: "owner",
  label: "Owner",
  columnType: "REFBACK",
  refTableId: "Owner",
  refSchemaId: "pet store",
  refLabel: "${firstName} ${lastName}",
  refLabelDefault: "${firstName}",
  refLinkId: "id",
  displayConfig: { clickAction: handleRefClick },
}));

// Column for slot usage example
const slotRefArrayColumn: IRefColumn = {
  id: "pets",
  label: "Pets (Custom Slot)",
  columnType: "REF_ARRAY",
  refTableId: "Pet",
  refSchemaId: "pet store",
  refLabel: "${name}",
  refLabelDefault: "${name}",
  refLinkId: "name",
};

// Mock ref table metadata for table display mode
const petTableMetadata: ITableMetaData = {
  id: "Pet",
  schemaId: "pet store",
  name: "Pet",
  label: "Pet",
  tableType: "DATA",
  columns: [
    { id: "name", label: "Name", columnType: "STRING", key: 1 },
    { id: "species", label: "Species", columnType: "STRING" },
    { id: "age", label: "Age", columnType: "INT" },
    { id: "vaccinated", label: "Vaccinated", columnType: "BOOL" },
  ],
};

// Column with table display mode
const tableDisplayColumn = computed<IRefColumn>(() => ({
  id: "pets",
  label: "Pets (Table Mode)",
  columnType: "REF_ARRAY",
  refTableId: "Pet",
  refSchemaId: "pet store",
  refLabel: "${name}",
  refLabelDefault: "${name}",
  refLinkId: "name",
  refTableMetadata: petTableMetadata,
  displayConfig: {
    component: "table",
    visibleColumns: ["name", "species", "age"],
    clickAction: handleRefClick,
  },
}));

// Column with table display mode and custom page size
const tableDisplayLargeColumn = computed<IRefColumn>(() => ({
  id: "pets",
  label: "Pets (Table Mode, pageSize=3)",
  columnType: "REF_ARRAY",
  refTableId: "Pet",
  refSchemaId: "pet store",
  refLabel: "${name}",
  refLabelDefault: "${name}",
  refLinkId: "name",
  refTableMetadata: petTableMetadata,
  displayConfig: {
    component: "table",
    visibleColumns: ["name", "species", "age", "vaccinated"],
    pageSize: 3,
    clickAction: handleRefClick,
  },
}));

// Mock pet data for table mode
const petTableData = [
  { name: "Fluffy", species: "Cat", age: 3, vaccinated: true },
  { name: "Buddy", species: "Dog", age: 5, vaccinated: true },
  { name: "Max", species: "Dog", age: 2, vaccinated: false },
  { name: "Whiskers", species: "Cat", age: 4, vaccinated: true },
  { name: "Charlie", species: "Bird", age: 1, vaccinated: false },
  { name: "Luna", species: "Cat", age: 2, vaccinated: true },
  { name: "Rocky", species: "Dog", age: 7, vaccinated: true },
];

const ontologyColumn: IColumn = {
  id: "species",
  label: "Species",
  columnType: "ONTOLOGY",
};

const ontologyArrayColumn: IColumn = {
  id: "tags",
  label: "Tags",
  columnType: "ONTOLOGY_ARRAY",
};

// Mock data for REF_ARRAY with pagination (15 items)
const largeRefArrayValue = Array.from({ length: 15 }, (_, i) => ({
  orderId: `ORD-${String(i + 1).padStart(3, "0")}`,
  status: ["Pending", "Shipped", "Delivered"][i % 3],
}));

// Mock ontology data with tree structure
const ontologyTreeValue = {
  name: "Mammal",
  definition: "Warm-blooded vertebrates",
  children: [
    { name: "Canine", definition: "Dog family" },
    { name: "Feline", definition: "Cat family" },
  ],
};

const emptyColumn: IColumn = {
  id: "empty",
  label: "Empty Field",
  columnType: "STRING",
};

function clearLog() {
  clickLog.value = [];
}
</script>

<template>
  <div class="p-5 space-y-6">
    <h1 class="text-2xl font-bold">RecordColumn Component</h1>
    <p class="text-gray-600">
      Renders a single column value with type-based dispatch.
    </p>

    <div class="flex items-center gap-2 p-4 bg-gray-100 rounded">
      <input id="showEmpty" v-model="showEmpty" type="checkbox" />
      <label for="showEmpty">Show empty values as "not provided"</label>
    </div>

    <!-- Wrap examples in bg-content to match RecordView/RecordSection -->
    <div
      class="bg-content p-6 rounded shadow-primary space-y-6 text-record-value"
    >
      <div class="space-y-4">
        <h2 class="text-xl font-semibold text-record-heading">
          Primitive Types
        </h2>

        <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
          <span class="font-medium text-record-label">STRING:</span>
          <DisplayRecordColumn
            :column="stringColumn"
            value="John Doe"
            :show-empty="showEmpty"
          />
        </div>

        <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
          <span class="font-medium text-record-label">INT:</span>
          <DisplayRecordColumn
            :column="intColumn"
            :value="42"
            :show-empty="showEmpty"
          />
        </div>

        <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
          <span class="font-medium text-record-label">BOOL (true):</span>
          <DisplayRecordColumn
            :column="boolColumn"
            :value="true"
            :show-empty="showEmpty"
          />
        </div>

        <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
          <span class="font-medium text-record-label">BOOL (false):</span>
          <DisplayRecordColumn
            :column="boolColumn"
            :value="false"
            :show-empty="showEmpty"
          />
        </div>

        <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
          <span class="font-medium text-record-label">DECIMAL:</span>
          <DisplayRecordColumn
            :column="decimalColumn"
            :value="19.99"
            :show-empty="showEmpty"
          />
        </div>

        <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
          <span class="font-medium text-record-label">DATE:</span>
          <DisplayRecordColumn
            :column="dateColumn"
            value="2024-01-15"
            :show-empty="showEmpty"
          />
        </div>

        <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
          <span class="font-medium text-record-label">TEXT:</span>
          <DisplayRecordColumn
            :column="textColumn"
            value="A longer description text that demonstrates how text values are displayed."
            :show-empty="showEmpty"
          />
        </div>

        <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
          <span class="font-medium text-record-label">EMAIL:</span>
          <DisplayRecordColumn
            :column="emailColumn"
            value="john@example.com"
            :show-empty="showEmpty"
          />
        </div>

        <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
          <span class="font-medium text-record-label">HYPERLINK:</span>
          <DisplayRecordColumn
            :column="hyperlinkColumn"
            value="https://molgenis.org"
            :show-empty="showEmpty"
          />
        </div>
      </div>

      <div class="space-y-4">
        <h2 class="text-xl font-semibold text-record-heading">
          Single Value Types (REF, RADIO, SELECT)
        </h2>
        <p class="text-sm text-gray-500">
          REF is clickable, RADIO and SELECT display the same way but without
          click action.
        </p>

        <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
          <span class="font-medium text-record-label">REF:</span>
          <DisplayRecordColumn
            :column="refColumn"
            :value="{ name: 'Fluffy' }"
            :show-empty="showEmpty"
          />
        </div>

        <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
          <span class="font-medium text-record-label">RADIO:</span>
          <DisplayRecordColumn
            :column="radioColumn"
            :value="{ name: 'Active' }"
            :show-empty="showEmpty"
          />
        </div>

        <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
          <span class="font-medium text-record-label">SELECT:</span>
          <DisplayRecordColumn
            :column="selectColumn"
            :value="{ name: 'High' }"
            :show-empty="showEmpty"
          />
        </div>

        <div class="mt-4 p-4 bg-gray-50 rounded border">
          <div class="flex justify-between items-center mb-2">
            <span class="font-medium text-record-label">Click Event Log:</span>
            <button
              class="text-sm text-blue-600 hover:underline"
              @click="clearLog"
            >
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
              class="text-gray-700"
            >
              {{ log }}
            </li>
          </ul>
        </div>
      </div>

      <div class="space-y-4">
        <h2 class="text-xl font-semibold text-record-heading">
          Array Types (REF_ARRAY, REFBACK, CHECKBOX, MULTISELECT)
        </h2>
        <p class="text-sm text-gray-500">
          All array types render as paginated lists. Click any link to see the
          event log.
        </p>

        <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
          <span class="font-medium text-record-label"
            >REF_ARRAY (3 items):</span
          >
          <DisplayRecordColumn
            :column="refArrayColumn"
            :value="[{ name: 'Fluffy' }, { name: 'Buddy' }, { name: 'Max' }]"
            :show-empty="showEmpty"
          />
        </div>

        <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
          <span class="font-medium text-record-label"
            >REF_ARRAY (15 items):</span
          >
          <DisplayRecordColumn
            :column="refArrayLargeColumn"
            :value="largeRefArrayValue"
            :show-empty="showEmpty"
          />
        </div>

        <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
          <span class="font-medium text-record-label">REFBACK:</span>
          <DisplayRecordColumn
            :column="refBackColumn"
            :value="[
              { firstName: 'John', lastName: 'Doe', id: 1 },
              { firstName: 'Jane', lastName: 'Smith', id: 2 },
            ]"
            :show-empty="showEmpty"
          />
        </div>

        <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
          <span class="font-medium text-record-label">CHECKBOX:</span>
          <DisplayRecordColumn
            :column="checkboxColumn"
            :value="[{ name: 'WiFi' }, { name: 'Parking' }, { name: 'Pool' }]"
            :show-empty="showEmpty"
          />
        </div>

        <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
          <span class="font-medium text-record-label">MULTISELECT:</span>
          <DisplayRecordColumn
            :column="multiselectColumn"
            :value="[
              { name: 'Electronics' },
              { name: 'Books' },
              { name: 'Clothing' },
              { name: 'Home' },
              { name: 'Sports' },
              { name: 'Toys' },
            ]"
            :show-empty="showEmpty"
          />
        </div>
      </div>

      <div class="space-y-4">
        <h2 class="text-xl font-semibold text-record-heading">
          Custom Slot Example
        </h2>
        <p class="text-sm text-gray-500">
          REF_ARRAY/REFBACK columns support a #list slot for custom rendering.
          This example shows a custom styled list using the slot.
        </p>

        <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
          <span class="font-medium text-record-label"
            >REF_ARRAY (Custom Slot):</span
          >
          <DisplayRecordColumn
            :column="slotRefArrayColumn"
            :value="[{ name: 'Fluffy' }, { name: 'Buddy' }, { name: 'Max' }]"
            :show-empty="showEmpty"
          >
            <template #list="{ column, value }">
              <div class="p-3 bg-blue-50 border border-blue-200 rounded">
                <p class="text-sm font-semibold text-blue-900 mb-2">
                  Custom list rendering for {{ column.label }}
                </p>
                <ul class="list-disc list-inside text-blue-700">
                  <li v-for="(item, idx) in value" :key="idx">
                    {{ item.name }} (custom style)
                  </li>
                </ul>
              </div>
            </template>
          </DisplayRecordColumn>
        </div>

        <div class="p-4 bg-gray-50 border border-gray-200 rounded text-sm">
          <strong>Note:</strong> When no #list slot is provided, the default
          bullet list rendering with pagination is used.
        </div>
      </div>

      <div class="space-y-4">
        <h2 class="text-xl font-semibold text-record-heading">
          Table Display Mode (displayConfig.component='table')
        </h2>
        <p class="text-sm text-gray-500">
          REF_ARRAY columns can use displayConfig to render as a table. Requires
          refTableMetadata and visibleColumns to be set.
        </p>

        <div class="space-y-2">
          <span class="font-medium text-record-label"
            >REF_ARRAY with table mode (3 columns):</span
          >
          <DisplayRecordColumn
            :column="tableDisplayColumn"
            :value="petTableData.slice(0, 3)"
            :show-empty="showEmpty"
          />
        </div>

        <div class="space-y-2">
          <span class="font-medium text-record-label"
            >REF_ARRAY with table mode (7 items, pageSize=3):</span
          >
          <DisplayRecordColumn
            :column="tableDisplayLargeColumn"
            :value="petTableData"
            :show-empty="showEmpty"
          />
        </div>

        <div class="p-4 bg-gray-50 border border-gray-200 rounded text-sm">
          <strong>Note:</strong> Table mode requires:
          <ul class="list-disc list-inside mt-2">
            <li>
              <code>displayConfig.component = 'table'</code>
            </li>
            <li>
              <code>displayConfig.visibleColumns</code> array of column IDs
            </li>
            <li><code>refTableMetadata</code> with column definitions</li>
          </ul>
        </div>
      </div>

      <div class="space-y-4">
        <h2 class="text-xl font-semibold text-record-heading">
          Ontology Types
        </h2>

        <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
          <span class="font-medium text-record-label">ONTOLOGY (single):</span>
          <DisplayRecordColumn
            :column="ontologyColumn"
            :value="{
              name: 'Dog',
              definition: 'A domesticated carnivorous mammal',
            }"
            :show-empty="showEmpty"
          />
        </div>

        <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
          <span class="font-medium text-record-label"
            >ONTOLOGY_ARRAY (flat list):</span
          >
          <DisplayRecordColumn
            :column="ontologyArrayColumn"
            :value="[
              { name: 'Friendly', definition: 'Good with people' },
              { name: 'Trained', definition: 'Has basic obedience training' },
              { name: 'Vaccinated' },
            ]"
            :show-empty="showEmpty"
          />
        </div>

        <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
          <span class="font-medium text-record-label"
            >ONTOLOGY (tree with children):</span
          >
          <DisplayRecordColumn
            :column="ontologyColumn"
            :value="ontologyTreeValue"
            :show-empty="showEmpty"
          />
        </div>
      </div>

      <div class="space-y-4">
        <h2 class="text-xl font-semibold text-record-heading">Empty Values</h2>
        <p class="text-sm text-gray-500">
          Toggle "Show empty values" above to see the difference.
        </p>

        <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
          <span class="font-medium text-record-label">null value:</span>
          <DisplayRecordColumn
            :column="emptyColumn"
            :value="null"
            :show-empty="showEmpty"
          />
          <span v-if="!showEmpty" class="col-start-2 text-xs text-gray-400">
            (nothing rendered)
          </span>
        </div>

        <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
          <span class="font-medium text-record-label">undefined value:</span>
          <DisplayRecordColumn
            :column="emptyColumn"
            :value="undefined"
            :show-empty="showEmpty"
          />
          <span v-if="!showEmpty" class="col-start-2 text-xs text-gray-400">
            (nothing rendered)
          </span>
        </div>

        <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
          <span class="font-medium text-record-label">empty string:</span>
          <DisplayRecordColumn
            :column="emptyColumn"
            value=""
            :show-empty="showEmpty"
          />
          <span v-if="!showEmpty" class="col-start-2 text-xs text-gray-400">
            (nothing rendered)
          </span>
        </div>
      </div>
    </div>
    <!-- close bg-content wrapper -->
  </div>
</template>

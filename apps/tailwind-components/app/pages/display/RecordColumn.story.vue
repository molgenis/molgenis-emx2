<script setup lang="ts">
import { ref } from "vue";
import type { IColumn, IRefColumn } from "../../../../metadata-utils/src/types";
import DisplayRecordColumn from "../../components/display/RecordColumn.vue";

const showEmpty = ref(false);
const clickLog = ref<string[]>([]);

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

const refColumn: IRefColumn = {
  id: "pet",
  label: "Pet",
  columnType: "REF",
  refTableId: "Pet",
  refSchemaId: "pet store",
  refLabel: "${name}",
  refLabelDefault: "${name}",
  refLinkId: "name",
};

const refArrayColumn: IRefColumn = {
  id: "pets",
  label: "Pets",
  columnType: "REF_ARRAY",
  refTableId: "Pet",
  refSchemaId: "pet store",
  refLabel: "${name}",
  refLabelDefault: "${name}",
  refLinkId: "name",
};

const refArrayLargeColumn: IRefColumn = {
  id: "orders",
  label: "Orders (15 items, paginated)",
  columnType: "REF_ARRAY",
  refTableId: "Order",
  refSchemaId: "pet store",
  refLabel: "${orderId} - ${status}",
  refLabelDefault: "${orderId}",
  refLinkId: "orderId",
};

const refBackColumn: IRefColumn = {
  id: "owner",
  label: "Owner",
  columnType: "REFBACK",
  refTableId: "Owner",
  refSchemaId: "pet store",
  refLabel: "${firstName} ${lastName}",
  refLabelDefault: "${firstName}",
  refLinkId: "id",
};

// Smart mode columns (for live data fetching via Emx2ListView)
const smartRefArrayColumn: IRefColumn = {
  id: "pets",
  label: "Pets (Smart Mode)",
  columnType: "REF_ARRAY",
  refTableId: "Pet",
  refSchemaId: "pet store",
  refLabel: "${name}",
  refLabelDefault: "${name}",
  refLinkId: "name",
};

const smartRefBackColumn: IRefColumn = {
  id: "pets",
  label: "Pets (Smart REFBACK)",
  columnType: "REFBACK",
  refTableId: "Pet",
  refSchemaId: "pet store",
  refLabel: "${name}",
  refLabelDefault: "${name}",
  refLinkId: "name",
  refBackId: "category",
};

// Mock parent row ID for refback filter
const parentRowId = { name: "cat" };

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

function getRefClickAction(col: IColumn, row: any) {
  return () => {
    const message = `Clicked REF: column="${col.id}", value=${JSON.stringify(
      row
    )}`;
    clickLog.value.unshift(message);
    if (clickLog.value.length > 5) {
      clickLog.value.pop();
    }
  };
}

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

    <div class="space-y-4">
      <h2 class="text-xl font-semibold">Primitive Types</h2>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">STRING:</span>
        <DisplayRecordColumn
          :column="stringColumn"
          value="John Doe"
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">INT:</span>
        <DisplayRecordColumn
          :column="intColumn"
          :value="42"
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">BOOL (true):</span>
        <DisplayRecordColumn
          :column="boolColumn"
          :value="true"
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">BOOL (false):</span>
        <DisplayRecordColumn
          :column="boolColumn"
          :value="false"
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">DECIMAL:</span>
        <DisplayRecordColumn
          :column="decimalColumn"
          :value="19.99"
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">DATE:</span>
        <DisplayRecordColumn
          :column="dateColumn"
          value="2024-01-15"
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">TEXT:</span>
        <DisplayRecordColumn
          :column="textColumn"
          value="A longer description text that demonstrates how text values are displayed."
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">EMAIL:</span>
        <DisplayRecordColumn
          :column="emailColumn"
          value="john@example.com"
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">HYPERLINK:</span>
        <DisplayRecordColumn
          :column="hyperlinkColumn"
          value="https://molgenis.org"
          :show-empty="showEmpty"
        />
      </div>
    </div>

    <div class="space-y-4">
      <h2 class="text-xl font-semibold">REF Type (Clickable)</h2>
      <p class="text-sm text-gray-500">Click the link to see the event log.</p>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">REF:</span>
        <DisplayRecordColumn
          :column="refColumn"
          :value="{ name: 'Fluffy' }"
          :show-empty="showEmpty"
          :get-ref-click-action="getRefClickAction"
        />
      </div>

      <div class="mt-4 p-4 bg-gray-50 rounded border">
        <div class="flex justify-between items-center mb-2">
          <span class="font-medium">Click Event Log:</span>
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
      <h2 class="text-xl font-semibold">REF_ARRAY Types (with pagination)</h2>
      <p class="text-sm text-gray-500">Click any link to see the event log.</p>

      <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
        <span class="font-medium">REF_ARRAY (3 items):</span>
        <DisplayRecordColumn
          :column="refArrayColumn"
          :value="[{ name: 'Fluffy' }, { name: 'Buddy' }, { name: 'Max' }]"
          :show-empty="showEmpty"
          :get-ref-click-action="getRefClickAction"
        />
      </div>

      <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
        <span class="font-medium">REF_ARRAY (15 items, paginated):</span>
        <DisplayRecordColumn
          :column="refArrayLargeColumn"
          :value="largeRefArrayValue"
          :show-empty="showEmpty"
          :get-ref-click-action="getRefClickAction"
        />
      </div>

      <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
        <span class="font-medium">REFBACK:</span>
        <DisplayRecordColumn
          :column="refBackColumn"
          :value="[
            { firstName: 'John', lastName: 'Doe', id: 1 },
            { firstName: 'Jane', lastName: 'Smith', id: 2 },
          ]"
          :show-empty="showEmpty"
          :get-ref-click-action="getRefClickAction"
        />
      </div>
    </div>

    <div class="space-y-4">
      <h2 class="text-xl font-semibold">
        Smart Mode (Emx2ListView with live data)
      </h2>
      <p class="text-sm text-gray-500">
        When schemaId is provided, REF_ARRAY/REFBACK use Emx2ListView to fetch
        data from backend. Requires running backend with "pet store" schema.
      </p>

      <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
        <span class="font-medium">REF_ARRAY (Smart):</span>
        <DisplayRecordColumn
          :column="smartRefArrayColumn"
          :value="null"
          schema-id="pet store"
          :show-empty="showEmpty"
          :get-ref-click-action="getRefClickAction"
        />
      </div>

      <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
        <span class="font-medium">REFBACK (Smart with filter):</span>
        <DisplayRecordColumn
          :column="smartRefBackColumn"
          :value="null"
          schema-id="pet store"
          :parent-row-id="parentRowId"
          :show-empty="showEmpty"
          :get-ref-click-action="getRefClickAction"
        />
      </div>

      <div class="p-4 bg-yellow-50 border border-yellow-200 rounded text-sm">
        <strong>Note:</strong> Smart mode examples require a running backend
        with the "pet store" schema. Without backend, they will show loading or
        error state.
      </div>
    </div>

    <div class="space-y-4">
      <h2 class="text-xl font-semibold">Ontology Types</h2>

      <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
        <span class="font-medium">ONTOLOGY (single):</span>
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
        <span class="font-medium">ONTOLOGY_ARRAY (flat list):</span>
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
        <span class="font-medium">ONTOLOGY (tree with children):</span>
        <DisplayRecordColumn
          :column="ontologyColumn"
          :value="ontologyTreeValue"
          :show-empty="showEmpty"
        />
      </div>
    </div>

    <div class="space-y-4">
      <h2 class="text-xl font-semibold">Empty Values</h2>
      <p class="text-sm text-gray-500">
        Toggle "Show empty values" above to see the difference.
      </p>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">null value:</span>
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
        <span class="font-medium">undefined value:</span>
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
        <span class="font-medium">empty string:</span>
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
</template>

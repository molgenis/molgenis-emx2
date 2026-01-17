<script setup lang="ts">
import { ref } from "vue";
import type { IColumn, IRefColumn } from "../../../../metadata-utils/src/types";
import DisplayRecordField from "../../components/display/RecordField.vue";

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

const refArrayColumn: IColumn = {
  id: "pets",
  label: "Pets",
  columnType: "REF_ARRAY",
};

const refBackColumn: IColumn = {
  id: "owner",
  label: "Owner",
  columnType: "REFBACK",
};

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
    <h1 class="text-2xl font-bold">RecordField Component</h1>
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
        <DisplayRecordField
          :column="stringColumn"
          value="John Doe"
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">INT:</span>
        <DisplayRecordField
          :column="intColumn"
          :value="42"
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">BOOL (true):</span>
        <DisplayRecordField
          :column="boolColumn"
          :value="true"
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">BOOL (false):</span>
        <DisplayRecordField
          :column="boolColumn"
          :value="false"
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">DECIMAL:</span>
        <DisplayRecordField
          :column="decimalColumn"
          :value="19.99"
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">DATE:</span>
        <DisplayRecordField
          :column="dateColumn"
          value="2024-01-15"
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">TEXT:</span>
        <DisplayRecordField
          :column="textColumn"
          value="A longer description text that demonstrates how text values are displayed."
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">EMAIL:</span>
        <DisplayRecordField
          :column="emailColumn"
          value="john@example.com"
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">HYPERLINK:</span>
        <DisplayRecordField
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
        <DisplayRecordField
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
      <h2 class="text-xl font-semibold">Placeholder Types</h2>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">REF_ARRAY:</span>
        <DisplayRecordField
          :column="refArrayColumn"
          :value="[{ name: 'Pet1' }, { name: 'Pet2' }]"
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">REFBACK:</span>
        <DisplayRecordField
          :column="refBackColumn"
          :value="[{ name: 'Owner1' }]"
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">ONTOLOGY:</span>
        <DisplayRecordField
          :column="ontologyColumn"
          :value="{ name: 'Dog' }"
          :show-empty="showEmpty"
        />
      </div>

      <div class="grid grid-cols-[150px_1fr] gap-2 items-center">
        <span class="font-medium">ONTOLOGY_ARRAY:</span>
        <DisplayRecordField
          :column="ontologyArrayColumn"
          :value="[{ name: 'Tag1' }, { name: 'Tag2' }]"
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
        <DisplayRecordField
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
        <DisplayRecordField
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
        <DisplayRecordField
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

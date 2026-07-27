<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { ITableMetaData } from "../../../../metadata-utils/src/types";
import DemoDataControls from "../../DemoDataControls.vue";
import Button from "../../components/Button.vue";
import RecordActions from "../../components/display/RecordActions.vue";

const router = useRouter();
const route = useRoute();

const schemaId = ref<string>((route.query.schema as string) || "pet store");
const tableId = ref<string>((route.query.table as string) || "Pet");
const metadata = ref<ITableMetaData>();
const formValues = ref<Record<string, any>>({});

const eventLog = ref<string[]>([]);
const reloadKey = ref(0);

const rowId = computed(() => {
  if (!metadata.value || !formValues.value) return {};
  const keyColumns = metadata.value.columns?.filter((c) => c.key === 1) || [];
  const result: Record<string, any> = {};
  for (const col of keyColumns) {
    if (formValues.value[col.id] !== undefined) {
      result[col.id] = formValues.value[col.id];
    }
  }
  return result;
});

const hasRowId = computed(() => Object.keys(rowId.value).length > 0);

watch([schemaId, tableId], ([newSchemaId, newTableId]) => {
  router.push({
    query: {
      schema: newSchemaId,
      table: newTableId,
    },
  });
});

function logEvent(name: string) {
  eventLog.value.push(`${new Date().toLocaleTimeString()} — ${name}`);
}

function afterChanged() {
  logEvent("changed");
  reloadKey.value++;
}

function clearLog() {
  eventLog.value = [];
}
</script>

<template>
  <div class="p-5 space-y-8">
    <h1 class="text-2xl font-bold">RecordActions Component</h1>
    <p class="text-gray-600 dark:text-gray-400">
      Edit and Delete buttons for a single record, with their modals. Fetches
      the row through its own subclass table, so a row selected via a superclass
      still shows and saves its subclass fields. Requires a live backend and an
      Editor role or admin session — the buttons are hidden otherwise.
    </p>

    <div class="space-y-4 p-4 bg-gray-100 dark:bg-gray-800 rounded">
      <DemoDataControls
        v-model:metadata="metadata"
        v-model:schemaId="schemaId"
        v-model:tableId="tableId"
        v-model:formValues="formValues"
        :include-row-select="true"
        :row-index="1"
      />
    </div>

    <div
      class="p-4 bg-gray-50 dark:bg-gray-800 rounded border dark:border-gray-600"
    >
      <div class="flex justify-between items-center mb-2">
        <span class="font-medium">Emitted Event Log:</span>
        <Button type="outline" size="small" @click="clearLog">Clear</Button>
      </div>
      <div v-if="eventLog.length === 0" class="text-gray-400 italic">
        No events yet
      </div>
      <ul v-else class="space-y-1 text-sm font-mono">
        <li
          v-for="(entry, index) in eventLog"
          :key="index"
          class="text-gray-700 dark:text-gray-300"
        >
          {{ entry }}
        </li>
      </ul>
    </div>

    <div class="space-y-4">
      <h2 class="text-xl font-semibold">Live Backend Data</h2>
      <p class="text-sm text-gray-500">
        Editing a record emits <code>changed</code>; deleting emits
        <code>deleted</code>. Consumers reload the record on
        <code>changed</code> and navigate away on <code>deleted</code>; this
        story only logs them and re-mounts the component.
      </p>

      <div v-if="!hasRowId" class="p-4 border rounded dark:border-gray-600">
        <p class="text-gray-500 italic">
          Select a row above to show its actions.
        </p>
      </div>

      <div v-else class="p-4 border rounded dark:border-gray-600">
        <RecordActions
          :key="`${schemaId}-${tableId}-${JSON.stringify(rowId)}-${reloadKey}`"
          :schema-id="schemaId"
          :table-id="tableId"
          :row-id="rowId"
          @changed="afterChanged"
          @deleted="logEvent('deleted')"
        />
      </div>
    </div>

    <div class="space-y-4">
      <h2 class="text-xl font-semibold">Test Checklist</h2>
      <ul
        class="list-disc list-inside text-sm text-gray-600 dark:text-gray-400"
      >
        <li>Signed out: no buttons render at all</li>
        <li>Signed in as Editor or admin: Edit and Delete both render</li>
        <li>
          Edit opens the form pre-filled with the row's values, including
          subclass fields when the selected table is a superclass
        </li>
        <li>
          Saving or cancelling the edit closes the modal and logs "changed"
        </li>
        <li>
          Delete shows the record preview with the same subclass fields before
          confirming, and logs "deleted"
        </li>
      </ul>
    </div>
  </div>
</template>

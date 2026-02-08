<template>
  <div class="p-6">
    <h1 class="text-heading-2xl font-bold mb-6">FilterPicker Component</h1>

    <DemoDataControls
      v-model:schemaId="schemaId"
      v-model:tableId="tableId"
      v-model:metadata="metadata"
    />

    <div class="flex gap-4 items-center mb-6 flex-wrap">
      <div class="flex gap-2 items-center">
        <label class="font-semibold">Mobile:</label>
        <button
          @click="isMobile = !isMobile"
          :class="[
            'px-3 py-1 rounded',
            isMobile ? 'bg-blue-500 text-white' : 'bg-gray-200',
          ]"
        >
          {{ isMobile ? "On" : "Off" }}
        </button>
      </div>
    </div>

    <div
      v-if="columns.length"
      :key="`${schemaId}-${tableId}`"
      :class="[
        'rounded-t-3px rounded-b-50px',
        isMobile ? 'max-w-[375px]' : 'max-w-md',
      ]"
    >
      <FilterSidebar
        v-model:filter-states="filterStates"
        v-model:search-terms="searchTerms"
        :all-columns="columns"
        :schema-id="schemaId"
        :show-search="true"
        :mobile-display="isMobile"
      />
    </div>
    <div v-else class="p-4 opacity-50 italic">No columns loaded</div>

    <div class="mt-4 p-4 bg-white rounded border max-w-md">
      <h3 class="font-semibold mb-2">Current Filter Values</h3>
      <div class="text-body-sm space-y-1 font-mono">
        <div v-for="[key, val] in filterStates" :key="key">
          <strong>{{ key }}:</strong> {{ JSON.stringify(val) }}
        </div>
        <div v-if="filterStates.size === 0" class="opacity-50 italic">
          No active filters
        </div>
      </div>
    </div>

    <div class="mt-4 p-4 bg-white rounded border max-w-md">
      <h3 class="font-semibold mb-2">Columns ({{ columns.length }})</h3>
      <div class="text-body-sm space-y-0.5 font-mono max-h-40 overflow-y-auto">
        <div v-for="col in columns" :key="col.id" class="flex gap-2">
          <span class="opacity-40 w-28 truncate">{{ col.columnType }}</span>
          <span>{{ col.label }}</span>
        </div>
      </div>
    </div>

    <section class="mt-12 p-6 bg-gray-50 rounded border max-w-2xl">
      <h2 class="text-heading-lg font-semibold mb-4">Specification</h2>
      <div class="text-body-sm space-y-4">
        <p>
          Searchable dropdown for selecting which filters are visible in the
          sidebar. Uses smart defaults (ontology and ref columns first).
        </p>

        <h3 class="font-semibold">FilterPicker dropdown</h3>
        <ul class="list-disc pl-5">
          <li>"Add filter" button with Plus icon opens dropdown</li>
          <li>Search input (size=tiny) filters columns by label, case-insensitive</li>
          <li>Columns grouped by heading, collapsible with caret</li>
          <li>Type priority within groups: ONTOLOGY > REF > numeric/date > string > bool</li>
          <li>Excludes HEADING, SECTION types and mg_* columns</li>
          <li>All non-REF columns have checkboxes to toggle visibility</li>
          <li>REF/REF_ARRAY columns have checkbox + expand caret (caret-down/up after label)</li>
          <li>REF checkbox = adds simple ref filter (select/radio in sidebar)</li>
          <li>REF expand = shows nested fields with checkboxes for granular filtering</li>
          <li>ONTOLOGY/ONTOLOGY_ARRAY: checkbox only, no expand</li>
          <li>Headings and columns show tooltip on hover (label, id, type, description)</li>
          <li>Dropdown width: w-96, max-h-80 scrollable</li>
          <li>Close: click outside or Escape key</li>
          <li>"Reset to defaults" restores smart default selection</li>
        </ul>

        <h3 class="font-semibold">Smart defaults</h3>
        <ul class="list-disc pl-5">
          <li>First 5 ONTOLOGY/ONTOLOGY_ARRAY columns</li>
          <li>Fill remaining slots (up to 5) with REF/REF_ARRAY columns</li>
        </ul>

        <h3 class="font-semibold">Sidebar filter display</h3>
        <ul class="list-disc pl-5">
          <li>Each visible filter has X button (top-right) to remove it</li>
          <li>Search input size matches filter inputs (default/medium)</li>
          <li>Direct REF filter: shows as ref select/radio input</li>
          <li>Nested field filter: label shows as "Parent.child" (dot notation)</li>
          <li>Expanded REF in sidebar: nested filters indented with left border</li>
        </ul>

        <h3 class="font-semibold">Test Checklist</h3>
        <ul class="list-disc pl-5">
          <li>Click "Add filter" - dropdown opens with grouped columns</li>
          <li>Check non-REF column - filter appears in sidebar</li>
          <li>Check REF column - simple ref filter appears in sidebar</li>
          <li>Expand REF, check nested field - "Parent.child" filter appears</li>
          <li>Click X on filter - removes from sidebar and unchecks in picker</li>
          <li>"Reset to defaults" - restores smart defaults</li>
          <li>Search filters by label, case-insensitive</li>
          <li>Collapse/expand heading groups</li>
          <li>Escape closes dropdown</li>
          <li>Switch schema/table - sidebar resets</li>
          <li>Toggle Mobile - check 375px width styling</li>
          <li>Switch themes - verify readability on sidebar gradient</li>
        </ul>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type {
  IColumn,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";
import DemoDataControls from "../../DemoDataControls.vue";
import FilterSidebar from "../../components/filter/Sidebar.vue";

const route = useRoute();
const router = useRouter();

const schemaId = ref<string>((route.query.schema as string) || "pet store");
const tableId = ref<string>((route.query.table as string) || "Pet");
const metadata = ref<ITableMetaData>();
const isMobile = ref(false);
const filterStates = ref<Map<string, IFilterValue>>(new Map());
const searchTerms = ref("");

const columns = computed<IColumn[]>(() => metadata.value?.columns ?? []);

watch([schemaId, tableId], ([newSchema, newTable]) => {
  filterStates.value = new Map();
  searchTerms.value = "";
  router.push({ query: { schema: newSchema, table: newTable } });
});
</script>

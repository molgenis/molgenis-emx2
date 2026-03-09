<template>
  <div class="p-6">
    <h1 class="text-heading-2xl font-bold mb-6">Filter Sidebar</h1>

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
      v-if="sidebarReady"
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
        :table-id="tableId"
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

const schemaId = ref<string>((route.query.schema as string) || "type test");
const tableId = ref<string>((route.query.table as string) || "Types");
const metadata = ref<ITableMetaData>();
const isMobile = ref(false);
const filterStates = ref<Map<string, IFilterValue>>(new Map());
const searchTerms = ref("");

const columns = computed<IColumn[]>(() => metadata.value?.columns ?? []);

const sidebarReady = ref(false);

const EXCLUDED_TYPES = ["HEADING", "SECTION"];

watch(
  columns,
  (cols) => {
    if (!cols.length) {
      sidebarReady.value = false;
      return;
    }
    const filterableIds = cols
      .filter(
        (c) => !EXCLUDED_TYPES.includes(c.columnType) && !c.id.startsWith("mg_")
      )
      .map((c) => c.id);
    router
      .replace({
        query: {
          schema: schemaId.value,
          table: tableId.value,
          mg_filters: filterableIds.join(","),
        },
      })
      .then(() => {
        sidebarReady.value = true;
      });
  },
  { immediate: true }
);

watch([schemaId, tableId], ([newSchema, newTable]) => {
  filterStates.value = new Map();
  searchTerms.value = "";
  sidebarReady.value = false;
  router.replace({ query: { schema: newSchema, table: newTable } });
});
</script>

<script setup lang="ts">
import { ref, watch, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import type {
  IColumn,
  IRow,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import DemoDataControls from "../../DemoDataControls.vue";
import DetailPageLayout from "../../components/layout/DetailPageLayout.vue";
import SideNav from "../../components/SideNav.vue";
import PageHeader from "../../components/PageHeader.vue";
import BreadCrumbs from "../../components/BreadCrumbs.vue";
import Emx2RecordView from "../../components/display/Emx2RecordView.vue";

const router = useRouter();
const route = useRoute();

const schemaId = ref<string>((route.query.schema as string) || "pet store");
const tableId = ref<string>((route.query.table as string) || "Pet");
const metadata = ref<ITableMetaData>();
const formValues = ref<Record<string, any>>({});

const showSideNav = ref(true);
const showEmpty = ref(false);
const clickLog = ref<string[]>([]);

// Extract rowId from formValues based on key columns
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

// Compute sections for SideNav from metadata SECTION and HEADING columns
const sections = computed(() => {
  if (!metadata.value?.columns) return [];

  const result: Array<{ id: string; label: string }> = [];

  const sectionColumns = metadata.value.columns.filter(
    (c) => c.columnType === "SECTION"
  );
  const headingColumns = metadata.value.columns.filter(
    (c) => c.columnType === "HEADING"
  );

  // Process SECTIONs with their nested HEADINGs
  for (const section of sectionColumns) {
    result.push({ id: section.id, label: section.label || section.id });

    const sectionHeadings = headingColumns.filter(
      (h) => h.section === section.id
    );
    for (const heading of sectionHeadings) {
      result.push({ id: heading.id, label: heading.label || heading.id });
    }
  }

  // Add orphan HEADINGs (not in any section)
  const orphanHeadings = headingColumns.filter((h) => !h.section);
  for (const heading of orphanHeadings) {
    result.push({ id: heading.id, label: heading.label || heading.id });
  }

  return result;
});

// Build breadcrumbs
const crumbs = computed(() => [
  { label: "Home", url: "/" },
  { label: schemaId.value, url: `/${schemaId.value}` },
  { label: tableId.value, url: `/${schemaId.value}/list/${tableId.value}` },
  { label: formValues.value?.name || "Record", url: "" },
]);

// Record title from formValues
const recordTitle = computed(() => {
  if (!formValues.value) return "Record";
  return (
    formValues.value.name ||
    formValues.value.label ||
    formValues.value.id ||
    JSON.stringify(rowId.value)
  );
});

// Update URL when schema/table changes
watch([schemaId, tableId], ([newSchemaId, newTableId]) => {
  router.push({
    query: {
      schema: newSchemaId,
      table: newTableId,
    },
  });
});

function getRefClickAction(col: IColumn, row: IRow) {
  return () => {
    const message = `Clicked: ${col.id} -> ${JSON.stringify(row)}`;
    clickLog.value.unshift(message);
    if (clickLog.value.length > 5) clickLog.value.pop();
  };
}

function clearLog() {
  clickLog.value = [];
}
</script>

<template>
  <div class="p-4">
    <h1 class="text-heading-xl mb-4">DetailPageLayout + Emx2RecordView</h1>
    <p class="text-body-base mb-4">
      This story demonstrates the full detail page pattern with live EMX2
      backend data. Select a schema, table, and row to view.
    </p>

    <!-- Controls -->
    <div class="space-y-4 p-4 bg-gray-100 dark:bg-gray-800 rounded mb-6">
      <DemoDataControls
        v-model:metadata="metadata"
        v-model:schemaId="schemaId"
        v-model:tableId="tableId"
        v-model:formValues="formValues"
        :include-row-select="true"
        :row-index="1"
      />

      <div class="flex items-center gap-4">
        <div class="flex items-center gap-2">
          <input
            id="show-side-nav"
            type="checkbox"
            v-model="showSideNav"
            class="hover:cursor-pointer"
          />
          <label for="show-side-nav" class="hover:cursor-pointer">
            showSideNav
          </label>
        </div>
        <div class="flex items-center gap-2">
          <input id="showEmpty" v-model="showEmpty" type="checkbox" />
          <label for="showEmpty">Show empty values</label>
        </div>
      </div>
    </div>

    <!-- Click log -->
    <div
      v-if="clickLog.length"
      class="mb-6 p-4 bg-blue-50 dark:bg-blue-900 rounded border border-blue-200 dark:border-blue-700"
    >
      <div class="flex justify-between items-center mb-2">
        <span class="font-semibold">Click Log (REF clicks)</span>
        <button class="text-sm text-blue-600 hover:underline" @click="clearLog">
          Clear
        </button>
      </div>
      <ul class="text-sm space-y-1">
        <li
          v-for="(log, i) in clickLog"
          :key="i"
          class="text-gray-700 dark:text-gray-300"
        >
          {{ log }}
        </li>
      </ul>
    </div>

    <!-- No row selected state -->
    <div v-if="!hasRowId" class="p-8 border rounded dark:border-gray-600">
      <p class="text-gray-500 italic text-center">
        Select a row above to view the detail page layout.
      </p>
    </div>

    <!-- Detail Page Layout -->
    <DetailPageLayout v-else :show-side-nav="showSideNav">
      <template #header>
        <PageHeader :id="`header-${tableId}`" :title="recordTitle">
          <template #prefix>
            <BreadCrumbs :crumbs="crumbs" />
          </template>
        </PageHeader>
      </template>

      <template #sidebar>
        <SideNav
          v-if="sections.length"
          :title="tableId.toUpperCase()"
          :sections="sections"
          :scroll-offset="80"
        />
        <div v-else class="text-gray-400 italic text-sm p-4">
          No sections defined in metadata
        </div>
      </template>

      <template #main>
        <Emx2RecordView
          :key="`${schemaId}-${tableId}-${JSON.stringify(rowId)}`"
          :schema-id="schemaId"
          :table-id="tableId"
          :row-id="rowId"
          :show-empty="showEmpty"
          :get-ref-click-action="getRefClickAction"
        />
      </template>
    </DetailPageLayout>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { ITableMetaData } from "../../../../metadata-utils/src/types";
import DetailView from "../../components/display/DetailView.vue";
import PageHeader from "../../components/PageHeader.vue";
import BreadCrumbs from "../../components/BreadCrumbs.vue";
import DemoDataControls from "../../DemoDataControls.vue";

const router = useRouter();
const route = useRoute();

const schemaId = ref<string>((route.query.schema as string) || "pet store");
const tableId = ref<string>((route.query.table as string) || "Pet");
const metadata = ref<ITableMetaData>();
const formValues = ref<Record<string, any>>({});

const showSideNav = ref(true);
const showEmpty = ref(false);

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

const crumbs = computed(() => [
  { label: "Home", url: "/" },
  { label: schemaId.value, url: `/${schemaId.value}` },
  { label: tableId.value, url: `/${schemaId.value}/list/${tableId.value}` },
  { label: formValues.value?.name || "Record", url: "" },
]);

const recordTitle = computed(() => {
  if (!formValues.value) return "Record";
  return (
    formValues.value.name ||
    formValues.value.label ||
    formValues.value.id ||
    JSON.stringify(rowId.value)
  );
});

watch([schemaId, tableId], ([newSchemaId, newTableId]) => {
  router.push({
    query: { schema: newSchemaId, table: newTableId },
  });
});
</script>

<template>
  <div class="p-4">
    <h1 class="text-heading-xl mb-4">DetailPageLayout</h1>
    <p class="text-body-base mb-4">
      DetailView fetches data and renders with auto-generated SideNav from
      SECTION columns.
    </p>

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

    <div v-if="!hasRowId" class="p-8 border rounded dark:border-gray-600">
      <p class="text-gray-500 italic text-center">
        Select a row above to view the detail page layout.
      </p>
    </div>

    <DetailView
      v-else
      :key="`${schemaId}-${tableId}-${JSON.stringify(rowId)}`"
      :schema-id="schemaId"
      :table-id="tableId"
      :row-id="rowId"
      :show-empty="showEmpty"
      :show-side-nav="showSideNav"
    />
  </div>
</template>

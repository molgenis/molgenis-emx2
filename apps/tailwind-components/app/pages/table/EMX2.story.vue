<script setup lang="ts">
import { ref, watch } from "vue";
import type { ITableSettings } from "../../../types/types";
import type { IColumn } from "../../../../metadata-utils/src/types";
import DemoDataControls from "../../DemoDataControls.vue";
import type { ITableMetaData } from "../../../../metadata-utils/src/types";
import { useRoute, useRouter } from "vue-router";
import { useFilters } from "../../composables/useFilters";
import fetchTableMetadata from "../../composables/fetchTableMetadata";

const tableSettings = ref<ITableSettings>({
  page: 1,
  pageSize: 10,
  orderby: { column: "", direction: "ASC" },
  search: "",
});

const router = useRouter();
const route = useRoute();

const isEditable = ref(false);
const showFilters = ref(false);
const metadata = ref<ITableMetaData>();
const schemaId = ref<string>((route.query.schema as string) || "");
const tableId = ref<string>((route.query.table as string) || "");
const filterColumns = ref<IColumn[]>([]);

const { filterStates, searchValue, gqlFilter, removeFilter, clearFilters } =
  useFilters(filterColumns, { urlSync: showFilters.value });

watch(searchValue, (val) => {
  if (showFilters.value) {
    tableSettings.value.search = val;
  }
});

watch(
  [schemaId, tableId],
  async ([newSchemaId, newTableId]) => {
    router.push({
      query: {
        schema: newSchemaId,
        table: newTableId,
      },
    });
    if (newSchemaId && newTableId) {
      try {
        const meta = await fetchTableMetadata(newSchemaId, newTableId);
        filterColumns.value = meta.columns.filter(
          (c) =>
            !c.id.startsWith("mg") &&
            !["HEADING", "SECTION", "FILE"].includes(c.columnType)
        );
      } catch {
        filterColumns.value = [];
      }
    }
  },
  { immediate: true }
);
</script>

<template>
  <div class="py-5 space-y-2">
    <DemoDataControls
      v-model:metadata="metadata"
      v-model:schemaId="schemaId"
      v-model:tableId="tableId"
    />
    <label class="text-title font-bold" for="is-editable">Is Editable: </label>
    <InputCheckbox id="is-editable" v-model="isEditable" name="is-editable" />
    <label class="text-title font-bold" for="show-filters"
      >Show Filters:
    </label>
    <InputCheckbox
      id="show-filters"
      v-model="showFilters"
      name="show-filters"
    />
    <div class="py-10" />

    <div :class="{ 'flex gap-6': showFilters }">
      <FilterSidebar
        v-if="showFilters && filterColumns.length"
        v-model:filterStates="filterStates"
        v-model:searchTerms="searchValue"
        :allColumns="filterColumns"
        :schemaId="schemaId"
        :tableId="tableId"
        :showSearch="true"
        class="w-64 shrink-0"
      />
      <div class="flex-1 min-w-0">
        <TableEMX2
          v-model:settings="tableSettings"
          :key="`${schemaId}-${tableId}`"
          :schema-id="schemaId"
          :table-id="tableId ?? ''"
          :is-editable="isEditable"
          :filter="showFilters ? gqlFilter : undefined"
          :hide-search="showFilters"
        />
      </div>
    </div>
  </div>
</template>

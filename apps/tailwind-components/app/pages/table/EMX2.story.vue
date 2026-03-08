<script setup lang="ts">
import { ref, watch, computed } from "vue";
import type { ITableSettings } from "../../../types/types";
import type { ITableMetaData } from "../../../../metadata-utils/src/types";
import { useRoute, useRouter } from "vue-router";
import { useFilters } from "../../composables/useFilters";
import FilterSidebar from "../../components/filter/Sidebar.vue";
import ActiveFilters from "../../components/filter/ActiveFilters.vue";

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

const filterColumns = computed(
  () =>
    metadata.value?.columns?.filter(
      (c) =>
        !c.id.startsWith("mg") &&
        !["HEADING", "SECTION", "FILE"].includes(c.columnType)
    ) ?? []
);

const { filterStates, searchValue, gqlFilter, removeFilter, clearFilters } =
  useFilters(filterColumns, { urlSync: showFilters.value });

watch(searchValue, (val) => {
  if (showFilters.value) {
    tableSettings.value.search = val;
  }
});

watch([schemaId, tableId], ([newSchemaId, newTableId]) => {
  router.push({
    query: {
      schema: newSchemaId,
      table: newTableId,
    },
  });
});
</script>

<template>
  <Story title="TableEMX2">
    <div class="py-5 space-y-2">
      <DemoDataControls
        v-model:metadata="metadata"
        v-model:schemaId="schemaId"
        v-model:tableId="tableId"
      />
      <label class="text-title font-bold" for="is-editable"
        >Is Editable:
      </label>
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

      <template v-if="schemaId && tableId">
        <div :class="{ 'flex gap-6': showFilters }">
          <FilterSidebar
            v-if="showFilters"
            v-model:filterStates="filterStates"
            v-model:searchTerms="searchValue"
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
            >
              <template v-if="showFilters" #below-toolbar>
                <ActiveFilters
                  :filters="filterStates"
                  :columns="filterColumns"
                  @remove="removeFilter"
                  @clear-all="clearFilters"
                />
              </template>
            </TableEMX2>
          </div>
        </div>
      </template>
      <p v-else class="text-title-contrast">
        Please select a schema and table using the controls above.
      </p>
    </div>
  </Story>
</template>

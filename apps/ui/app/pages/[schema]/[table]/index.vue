<script setup lang="ts">
import { computed, ref, useId } from "vue";
import type {
  Crumb,
  ITableSettings,
  sortDirection,
} from "../../../../../tailwind-components/types/types";
import fetchTableMetadata from "../../../../../tailwind-components/app/composables/fetchTableMetadata";
import { useRoute, useRouter } from "#app/composables/router";
import { useSession } from "../../../../../tailwind-components/app/composables/useSession";
import { useFilters } from "../../../../../tailwind-components/app/composables/useFilters";
import { watch } from "vue";
import { useHead } from "#app";
import TableEMX2 from "../../../../../tailwind-components/app/components/table/TableEMX2.vue";
import FilterSidebar from "../../../../../tailwind-components/app/components/filter/Sidebar.vue";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import type { IRow } from "../../../../../metadata-utils/src/types";
import { getPrimaryKey } from "../../../../../tailwind-components/app/utils/getPrimaryKey";
import { keySlug } from "../../../../../tailwind-components/app/utils/navigationUtils";
import Button from "../../../../../tailwind-components/app/components/Button.vue";
import ActiveFilters from "../../../../../tailwind-components/app/components/filter/ActiveFilters.vue";

const route = useRoute();
const router = useRouter();
const schemaId = route.params.schema as string;
const tableId = route.params.table as string;

useHead({ title: `${tableId} - ${schemaId}  - Molgenis` });

const currentPage = computed(() => {
  const queryPageNumber = Number(route.query?.page);
  return !isNaN(queryPageNumber) && typeof queryPageNumber === "number"
    ? Math.round(queryPageNumber)
    : 1;
});

const orderbyColumn = computed(() => route.query.orderby as string);
const orderbyDirection = computed(() =>
  route.query.order ? (route.query.order as sortDirection) : "ASC"
);

const tableSettings = ref<ITableSettings>({
  page: currentPage.value,
  pageSize: 10,
  orderby: {
    column: orderbyColumn.value,
    direction: orderbyDirection.value,
  },
  search: "",
});

const tableMetadata = await fetchTableMetadata(schemaId, tableId);

const filterColumns = computed(
  () =>
    tableMetadata?.columns?.filter(
      (c) =>
        !c.id.startsWith("mg") &&
        !["HEADING", "SECTION", "FILE"].includes(c.columnType)
    ) ?? []
);
const filters = useFilters(filterColumns, {
  urlSync: true,
  route,
  router,
  schemaId,
  tableId,
});

const gqlFilter = computed(() => filters.gqlFilter.value);

watch(
  () => filters.searchValue.value,
  (val) => {
    tableSettings.value.search = val;
  }
);

function handleSettingsUpdate() {
  const query = {
    ...route.query,
    orderby: tableSettings.value.orderby.column,
    order: !tableSettings.value.orderby.column
      ? undefined
      : tableSettings.value.orderby.direction,
    page: tableSettings.value.page < 2 ? undefined : tableSettings.value.page,
  };

  router.replace({ query });
}

async function handleViewRowRequest(row: IRow) {
  const primaryKeys = await getPrimaryKey(row, tableId, schemaId);

  router.push({
    path: `/${schemaId}/${tableId}/${keySlug(primaryKeys)}`,
    query: {
      keys: JSON.stringify(primaryKeys),
    },
  });
}

const crumbs: Crumb[] = [
  { label: schemaId, url: `/${schemaId}` },
  { label: tableMetadata.label || tableMetadata.id, url: "" },
];

const currentBreadCrumb = computed(
  () => tableMetadata.label ?? tableMetadata.id
);

watch(tableSettings, handleSettingsUpdate, { deep: true });

const { isAdmin, session } = await useSession(schemaId);
</script>
<template>
  <div class="mx-auto lg:px-[30px] px-0">
    <PageHeader :title="tableMetadata?.label ?? ''" align="left">
      {{ tableMetadata }}
      <template #prefix>
        <BreadCrumbs
          :align="'left'"
          :crumbs="crumbs"
          :current="currentBreadCrumb"
        />
      </template>
    </PageHeader>

    <div class="flex gap-6">
      <FilterSidebar
        :filters="filters"
        :schemaId="schemaId"
        :tableId="tableId"
        :showSearch="true"
        class="w-64 shrink-0"
      />
      <div class="flex-1 min-w-0">
        <TableEMX2
          :schemaId="schemaId"
          :tableId="tableId"
          v-model:settings="tableSettings"
          :isEditable="
            session?.roles?.[schemaId]?.includes('Editor') || isAdmin
          "
          :filter="gqlFilter"
          :hide-search="true"
        >
          <template #below-toolbar>
            <ActiveFilters
              :filters="filters.activeFilters.value"
              @remove="filters.removeFilter"
              @clear-all="filters.clearFilters"
            />
          </template>
          <template #additional-row-actions="{ row }">
            <Button
              :id="useId()"
              :icon-only="true"
              type="inline"
              icon="info"
              size="small"
              label="view row details"
              @click="handleViewRowRequest(row)"
            />
          </template>
        </TableEMX2>
      </div>
    </div>
  </div>
</template>

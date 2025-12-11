<script setup lang="ts">
import { computed, ref } from "vue";
import type {
  Crumb,
  ITableSettings,
  KeyObject,
  RowPayload,
  sortDirection,
} from "../../../../../tailwind-components/types/types";
import fetchTableMetadata from "../../../../../tailwind-components/app/composables/fetchTableMetadata";
import { useRoute, useRouter } from "#app/composables/router";
import { useSession } from "../../../../../tailwind-components/app/composables/useSession";
import { watch } from "vue";
import { useHead } from "#app";
import TableEMX2 from "../../../../../tailwind-components/app/components/table/TableEMX2.vue";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import { getPrimaryKey } from "#imports";

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

const search = computed(() => route.query.search as string);

const tableSettings = ref<ITableSettings>({
  page: currentPage.value,
  pageSize: 10,
  orderby: {
    column: orderbyColumn.value,
    direction: orderbyDirection.value,
  },
  search: search.value || "",
});

const tableMetadata = await fetchTableMetadata(schemaId, tableId);

function handleSettingsUpdate() {
  const query = {
    ...route.query,
    orderby: tableSettings.value.orderby.column,
    order: !tableSettings.value.orderby.column
      ? undefined
      : tableSettings.value.orderby.direction,
    search:
      tableSettings.value.search === ""
        ? undefined
        : tableSettings.value.search,
    page: tableSettings.value.page < 2 ? undefined : tableSettings.value.page,
  };

  router.push({ query });
}

const crumbs: Crumb[] = [
  { label: schemaId, url: `/${schemaId}` },
  { label: tableMetadata.label || tableMetadata.id, url: "" },
];

const currentBreadCrumb = computed(
  () => tableMetadata.label ?? tableMetadata.id
);

watch(tableSettings, handleSettingsUpdate, { deep: true });

const { isAdmin, session } = await useSession();

/**
 * Generates human readable key from KeyObject, one way only, only used for readability
 */
const buildValueKey = (keyObject: KeyObject): string => {
  return Object.values(keyObject).reduce(
    (acc: string, val: string | KeyObject) => {
      const joiner = acc.length === 0 ? "" : "-";
      return (acc +=
        joiner + (typeof val === "string" ? val : buildValueKey(val)));
    },
    ""
  );
};

async function onRowClicked({ data, metadata }: RowPayload) {
  const primaryKeys = await getPrimaryKey(data, tableId, schemaId);

  router.push(
    `/${schemaId}/${tableId}/${
      buildValueKey(primaryKeys) + "?keys=" + JSON.stringify(primaryKeys)
    }`
  );
}
</script>
<template>
  <section class="mx-auto lg:px-[30px] px-0">
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

    <TableEMX2
      :schemaId="schemaId"
      :tableId="tableId"
      v-model:settings="tableSettings"
      :isEditable="session?.roles?.includes('Editor') || isAdmin"
      @row-clicked="onRowClicked($event)"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import type {
  ITableSettings,
  sortDirection,
} from "../../../../tailwind-components/types/types";
import fetchTableMetadata from "../../../../tailwind-components/composables/fetchTableMetadata";
import { useRoute, useRouter } from "#app/composables/router";
import { useSession } from "../../../../ui/composables/useSession";
import { watch } from "vue";

const route = useRoute();
const router = useRouter();
const schemaId = route.params.schema as string;
const tableId = route.params.table as string;

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

const crumbs = computed(() => {
  let crumb: { [key: string]: string } = {};
  crumb[schemaId] = `/${schemaId}`;
  return crumb;
});

const currentBreadCrumb = computed(
  () => tableMetadata.label ?? tableMetadata.id
);

watch(tableSettings, handleSettingsUpdate, { deep: true });

const { isAdmin } = useSession();
</script>
<template>
  <section class="mx-auto lg:px-[30px] px-0">
    <PageHeader :title="tableMetadata?.label ?? ''" align="left">
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
      :isEditable="isAdmin"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "#app/composables/router";
import { useHead } from "#app";
import { definePageMeta } from "#imports";
import type { Crumb } from "../../../../../tailwind-components/types/types";
import fetchTableMetadata from "../../../../../tailwind-components/app/composables/fetchTableMetadata";
import { useFilters } from "../../../../../tailwind-components/app/composables/useFilters";
import { getCardListColumnIds } from "../../../../../tailwind-components/app/utils/displayUtils";
import Container from "../../../../../tailwind-components/app/components/Container.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import Sidebar from "../../../../../tailwind-components/app/components/Sidebar.vue";
import FilterSidebarContent from "../../../../../tailwind-components/app/components/filter/SidebarContent.vue";
import ActiveFilters from "../../../../../tailwind-components/app/components/filter/ActiveFilters.vue";
import InputSearch from "../../../../../tailwind-components/app/components/input/Search.vue";
import DataList from "../../../../../tailwind-components/app/components/display/DataList.vue";

const route = useRoute();
const schemaId = route.params.schema as string;
const tableId = route.params.table as string;

useHead({ title: `${tableId} - ${schemaId} - Molgenis` });

definePageMeta({
  layout: "wide",
});

const tableMetadata = await fetchTableMetadata(schemaId, tableId);

const columns = computed(() => tableMetadata.columns ?? []);

const filters = useFilters(columns, {
  urlSync: true,
  schemaId,
  tableId,
});

const cardColumnIds = computed(() => getCardListColumnIds(columns.value));

const sidebarCollapsed = ref(false);

onMounted(() => {
  sidebarCollapsed.value = window.matchMedia("(max-width: 1023px)").matches;
});

const searchValue = computed({
  get: () => filters.searchValue.value,
  set: (value: string) => filters.setSearch(value),
});

const crumbs: Crumb[] = [
  { label: schemaId, url: `/${schemaId}` },
  {
    label: tableMetadata.label || tableMetadata.id,
    url: `/${schemaId}/${tableId}`,
  },
];
</script>

<template>
  <Container :wide="true">
    <PageHeader :title="tableMetadata.label ?? tableMetadata.id" align="left">
      <template #prefix>
        <BreadCrumbs :align="'left'" :crumbs="crumbs" current="List" />
      </template>
    </PageHeader>

    <div class="flex mb-[30px] justify-end">
      <div class="shrink-0 w-80 xl:w-96 lg:-ml-[30px] px-5">
        <InputSearch
          id="list-search-input"
          v-model="searchValue"
          class="w-full"
          size="medium"
          :placeholder="`Search ${tableId}`"
        />
      </div>
    </div>

    <div class="flex overflow-hidden gap-6 lg:-ml-[30px]">
      <Sidebar
        :collapsed="sidebarCollapsed"
        :active-filter-count="filters.activeFilters.value.length"
        @update:collapsed="sidebarCollapsed = $event"
      >
        <FilterSidebarContent
          :filters="filters"
          :columns="filters.columns.value"
          :schema-id="schemaId"
          :table-id="tableId"
        />
      </Sidebar>

      <div class="flex-1 min-w-0">
        <ActiveFilters
          :filters="filters.activeFilters.value"
          :search-value="filters.searchValue.value"
          @remove="filters.removeFilter"
          @clear-all="filters.clearFilters"
          @clear-search="filters.setSearch('')"
        />
        <DataList
          :schema-id="schemaId"
          :table-id="tableId"
          :filter="filters.gqlFilter.value"
          :visible-columns="cardColumnIds"
          layout="CARDS"
          :hide-search="true"
        />
      </div>
    </div>
  </Container>
</template>

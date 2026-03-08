<script setup lang="ts">
import type { IFilter } from "../../../../../interfaces/types";
import type { Crumb } from "../../../../../../tailwind-components/types/types";
import { useRoute, useRouter, useHead, useRuntimeConfig } from "#app";
import { buildQueryFilter } from "../../../../utils/buildQueryFilter";
import { computed, markRaw } from "vue";
import LayoutsSearchPage from "../../../../components/layouts/SearchPage.vue";
import FilterSidebar from "../../../../components/filter/Sidebar.vue";
import BreadCrumbs from "../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../../../tailwind-components/app/components/PageHeader.vue";
import SearchResults from "../../../../components/SearchResults.vue";
import FilterWell from "../../../../components/FilterWell.vue";
import SearchResultsList from "../../../../components/SearchResultsList.vue";
import DatasetCard from "../../../../components/DatasetCard.vue";
import Emx2ListView from "../../../../../../tailwind-components/app/components/display/Emx2ListView.vue";
import {
  conditionsFromPathQuery,
  mergeWithPageDefaults,
  toPathQueryConditions,
} from "../../../../utils/filterUtils";

const config = useRuntimeConfig();
const schema = config.public.schema as string;

const route = useRoute();
const router = useRouter();
const pageSize = 20;

const catalogue = route.params.catalogue as string;
const resourceId = route.params.resource as string;

const titlePrefix = catalogue === "all" ? "" : catalogue + " ";
useHead({
  title: `${titlePrefix}Datasets - ${resourceId}`,
  meta: [
    {
      name: "description",
      content: `Datasets from ${resourceId}`,
    },
  ],
});

const pageFilterTemplate: IFilter[] = [
  {
    id: "search",
    config: {
      label: "Search in datasets",
      type: "SEARCH",
      initialCollapsed: false,
    },
    search: "",
  },
  {
    id: "datasetType",
    config: {
      label: "Dataset type",
      type: "ONTOLOGY",
      ontologyTableId: "DatasetTypes",
      ontologySchema: "CatalogueOntologies",
      columnId: "datasetType",
      initialCollapsed: true,
    },
    conditions: [],
  },
  {
    id: "unitOfObservation",
    config: {
      label: "Unit of observation",
      type: "ONTOLOGY",
      ontologyTableId: "UnitsOfObservation",
      ontologySchema: "CatalogueOntologies",
      columnId: "unitOfObservation",
      initialCollapsed: true,
    },
    conditions: [],
  },
];

const filters = computed(() => {
  if (!route.query?.conditions) {
    return [...pageFilterTemplate];
  }
  const conditions = conditionsFromPathQuery(route.query.conditions as string);
  const filters = mergeWithPageDefaults(pageFilterTemplate, conditions);
  return filters;
});

const gqlFilter = computed(() => {
  let result = buildQueryFilter(filters.value);

  // Always filter by resource
  const resourceFilter = { resource: { id: { equals: resourceId } } };

  if (Object.keys(result).length === 0) {
    return resourceFilter;
  }

  return { _and: [result, resourceFilter] };
});

function onFilterChange(filters: IFilter[]) {
  const conditions = toPathQueryConditions(filters) || undefined;
  router.push({
    path: route.path,
    query: { conditions: conditions },
  });
}

const listDisplayConfig = {
  component: markRaw(DatasetCard),
  pageSize: pageSize,
};

const crumbs: Crumb[] = [
  { label: catalogue, url: `/${catalogue}` },
  { label: "datasets", url: `/${catalogue}/datasets` },
  { label: resourceId, url: "" },
];
</script>

<template>
  <LayoutsSearchPage>
    <template #side>
      <FilterSidebar
        title="Filters"
        :filters="filters"
        @update:filters="onFilterChange"
      />
    </template>
    <template #main>
      <SearchResults>
        <template #header>
          <PageHeader
            :title="`Datasets - ${resourceId}`"
            :description="`Datasets from ${resourceId}`"
            icon="image-table"
          >
            <template #prefix>
              <BreadCrumbs :crumbs="crumbs" />
            </template>
          </PageHeader>
        </template>

        <template #search-results>
          <FilterWell :filters="filters" @update:filters="onFilterChange" />

          <SearchResultsList>
            <Emx2ListView
              :schema-id="schema"
              table-id="Datasets"
              :filter="gqlFilter"
              :display-config="listDisplayConfig"
              :paging-limit="pageSize"
              :show-search="false"
              :component-props="{ catalogue: catalogue }"
            />
          </SearchResultsList>
        </template>
      </SearchResults>
    </template>
  </LayoutsSearchPage>
</template>

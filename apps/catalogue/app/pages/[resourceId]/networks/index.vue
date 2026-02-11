<script setup lang="ts">
import {
  useRoute,
  useRouter,
  useRuntimeConfig,
  useHead,
  useFetch,
  navigateTo,
} from "#app";
import { useCatalogueContext } from "#imports";
import type {
  IFilter,
  IMgError,
  activeTabType,
} from "../../../../interfaces/types";
import LayoutsSearchPage from "../../../components/layouts/SearchPage.vue";
import FilterSidebar from "../../../components/filter/Sidebar.vue";
import SearchResults from "../../../components/SearchResults.vue";
import SearchResultsViewTabs from "../../../components/SearchResultsViewTabs.vue";
import SearchResultsViewTabsMobile from "../../../components/SearchResultsViewTabsMobile.vue";
import SearchResultsCount from "../../../components/SearchResultsCount.vue";
import SearchResultsList from "../../../components/SearchResultsList.vue";
import FilterWell from "../../../components/FilterWell.vue";
import Pagination from "../../../../../tailwind-components/app/components/Pagination.vue";
import CardList from "../../../../../tailwind-components/app/components/CardList.vue";
import CardListItem from "../../../../../tailwind-components/app/components/CardListItem.vue";
import ResourceCard from "../../../components/ResourceCard.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import {
  conditionsFromPathQuery,
  mergeWithPageDefaults,
  toPathQueryConditions,
} from "../../../utils/filterUtils";
import { buildQueryFilter } from "../../../utils/buildQueryFilter";
import { computed, ref } from "vue";
import { logError } from "../../../utils/errorLogger";
import type { Crumb } from "../../../../../tailwind-components/types/types";

const config = useRuntimeConfig();
const schema = config.public.schema as string;

const route = useRoute();
const router = useRouter();
const pageSize = 10;
const { buildBreadcrumbs, resourceUrl } = useCatalogueContext();

const resourceId = route.params.resourceId as string;

const titlePrefix = `${resourceId} `;
const title = "networks";
const description = "Networks";
const image = "image-diagram";

useHead({
  title: titlePrefix + title.charAt(0).toUpperCase() + title.slice(1),
  meta: [{ name: "description", content: description }],
});

const currentPage = computed(() => {
  const queryPageNumber = Number(route.query?.page);
  return !isNaN(queryPageNumber) && typeof queryPageNumber === "number"
    ? Math.round(queryPageNumber)
    : 1;
});
const offset = computed(() => (currentPage.value - 1) * pageSize);

let pageFilterTemplate: IFilter[] = [
  {
    id: "search",
    config: {
      label: `Search in ${title}`,
      type: "SEARCH",
      searchTables: [],
      initialCollapsed: false,
    },
    search: "",
  },
  {
    id: "type",
    config: {
      label: "Network type",
      type: "ONTOLOGY",
      ontologyTableId: "ResourceTypes",
      ontologySchema: "CatalogueOntologies",
      filter: { tags: { equals: "network" } },
      columnId: "type",
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

const query = computed(() => {
  return `
  query Resources($filter:ResourcesFilter, $orderby:Resourcesorderby){
    Resources(limit: ${pageSize} offset: ${offset.value} filter:$filter  orderby:$orderby) {
      id
      name
      acronym
      description
      keywords
      numberOfParticipants
      startYear
      endYear
      type {
          name
      }
      design {
          name
      }
      datasets {
        name
        label
      }
    }
    Resources_agg (filter:$filter){
        count
    }
  }
  `;
});

const orderby = { acronym: "ASC" };

const gqlFilter = computed(() => {
  let result: any = {};

  result = buildQueryFilter(filters.value);

  if (!result.type) {
    result.type = { tags: { equals: "network" } };
  }

  if (resourceId === "all") {
    return result;
  }

  return {
    _and: [
      result,
      {
        _or: [
          { parentNetworks: { id: { equals: resourceId } } },
          {
            parentNetworks: {
              parentNetworks: { id: { equals: resourceId } },
            },
          },
        ],
      },
    ],
  };
});

const { data } = await useFetch<any, IMgError>(`/${schema}/graphql`, {
  key: `networks-${JSON.stringify(gqlFilter.value)}-${currentPage.value}`,
  method: "POST",
  body: {
    query: query,
    variables: { filter: gqlFilter, orderby },
  },
  onResponseError(_ctx) {
    logError({
      message: "onResponseError fetching data from GraphQL endpoint",
      statusCode: _ctx.response.status,
      data: _ctx.response._data,
    });
  },
});

const resources = computed(() => data.value?.data.Resources || []);
const numberOfResources = computed(
  () => data.value?.data.Resources_agg.count || 0
);

async function setCurrentPage(pageNumber: number) {
  await navigateTo({
    query: { ...route.query, page: pageNumber },
  });
  window.scrollTo({ top: 0 });
}

function onFilterChange(filters: IFilter[]) {
  const conditions = toPathQueryConditions(filters) || undefined;

  router.push({
    path: route.path,
    query: { ...route.query, page: 1, conditions: conditions },
  });
}

const activeTabName = ref((route.query.view as string) || "detailed");

function onActiveTabChange(tabName: activeTabType) {
  activeTabName.value = tabName;
  router.push({
    path: route.path,
    query: { ...route.query, view: tabName },
  });
}

const crumbs: Crumb[] =
  resourceId === "all"
    ? [
        { label: "home", url: "/" },
        { label: "networks", url: "" },
      ]
    : buildBreadcrumbs([
        { label: resourceId, url: resourceUrl(resourceId) },
        { label: "networks", url: "" },
      ]);
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
          <PageHeader :title="title" :description="description" :icon="image">
            <template #prefix>
              <BreadCrumbs :crumbs="crumbs" />
            </template>
            <template #suffix>
              <SearchResultsViewTabs
                class="hidden xl:flex"
                buttonLeftLabel="Detailed"
                buttonLeftName="detailed"
                buttonLeftIcon="view-normal"
                buttonRightLabel="Compact"
                buttonRightName="compact"
                buttonRightIcon="view-compact"
                :activeName="activeTabName"
                @update:activeName="onActiveTabChange"
              />
              <SearchResultsViewTabsMobile
                class="flex xl:hidden"
                button-top-label="View"
                button-top-name="detailed"
                button-top-icon="view-normal"
                button-bottom-label="View"
                button-bottom-name="compact"
                button-bottom-icon="view-compact"
                :activeName="activeTabName"
                @update:active-name="onActiveTabChange"
              >
                <FilterSidebar
                  title="Filters"
                  :filters="filters"
                  @update:filters="onFilterChange"
                  :mobileDisplay="true"
                />
              </SearchResultsViewTabsMobile>
            </template>
          </PageHeader>
        </template>

        <template #search-results>
          <SearchResultsCount
            :label="title?.toLocaleLowerCase()"
            :value="numberOfResources"
          />
          <FilterWell
            :filters="filters"
            @update:filters="onFilterChange"
          ></FilterWell>
          <SearchResultsList>
            <CardList v-if="resources.length > 0">
              <CardListItem v-for="resource in resources" :key="resource.name">
                <ResourceCard
                  :resource="resource"
                  :schema="schema"
                  :catalogue="resourceId"
                  :compact="activeTabName !== 'detailed'"
                />
              </CardListItem>
            </CardList>
            <div v-else class="flex justify-center pt-3">
              <span class="py-15 text-link">
                No resources found with current filters
              </span>
            </div>
          </SearchResultsList>
        </template>

        <template v-if="resources.length > 0" #pagination>
          <Pagination
            :current-page="currentPage"
            :totalPages="Math.ceil(numberOfResources / pageSize)"
            @update="setCurrentPage($event)"
          />
        </template>
      </SearchResults>
    </template>
  </LayoutsSearchPage>
</template>

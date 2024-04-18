<script setup lang="ts">
import type { IFilter, IMgError, activeTabType } from "~/interfaces/types";

const route = useRoute();
const router = useRouter();
const pageSize = 10;

const scoped = route.params.catalogue !== "all";
const catalogue = scoped ? route.params.catalogue : undefined;

useHead({ title: scoped ? `${catalogue} networks` : "Networks" });

const currentPage = computed(() => {
  const queryPageNumber = Number(route.query?.page);
  return !isNaN(queryPageNumber) && typeof queryPageNumber === "number"
    ? Math.round(queryPageNumber)
    : 1;
});
const offset = computed(() => (currentPage.value - 1) * pageSize);

const pageFilterTemplate: IFilter[] = [
  {
    id: "search",
    config: {
      label: "Search in networks",
      type: "SEARCH",
      initialCollapsed: false,
    },
    search: "",
  },
  {
    id: "type",
    config: {
      label: "Type",
      type: "ONTOLOGY",
      ontologyTableId: "NetworkTypes",
      ontologySchema: "CatalogueOntologies",
      columnId: "type",
      initialCollapsed: false,
    },
    conditions: [],
  },
];

const filters = computed(() => {
  // if there are not query conditions just use the page defaults
  if (!route.query?.conditions) {
    return [...pageFilterTemplate];
  }

  // get conditions from query
  const conditions = conditionsFromPathQuery(route.query.conditions as string);
  // merge with page defaults
  const filters = mergeWithPageDefaults(pageFilterTemplate, conditions);

  return filters;
});

const cardFields = `id
    name
    acronym
    description
    logo {
      id
      size
      extension
      url
    }`;

const query = computed(() => {
  if (scoped) {
    return `
query items($catalogueFilter: NetworksFilter, $filter: NetworksFilter $orderby: Networksorderby) {
  Networks(filter: $catalogueFilter) {
    id
    networks(limit: ${pageSize} offset: ${offset.value} orderby:$orderby filter:$filter) {
      ${cardFields}
    }
    networks_agg(filter:$filter) {
      count
    }
  }
}
    `;
  } else {
    return `
query items($filter:NetworksFilter, $orderby:Networksorderby){
  Networks(limit: ${pageSize} offset: ${offset.value} filter:$filter  orderby:$orderby) {
    ${cardFields}
  }
  Networks_agg (filter:$filter){
    count
  }
}
    `;
  }
});

const orderby = { acronym: "ASC" };

const gqlFilter = computed(() => {
  return buildQueryFilter(filters.value);
});

const catalogueFilter = scoped ? { id: { equals: catalogue } } : undefined;

const { data } = await useFetch<any, IMgError>(
  `/${useRoute().params.schema}/graphql`,
  {
    method: "POST",
    body: {
      query: query,
      variables: { filter: gqlFilter, orderby, catalogueFilter },
    },
    onResponseError(_ctx) {
      logError({
        message: "onResponseError fetching data from GraphQL endpoint",
        statusCode: _ctx.response.status,
        data: _ctx.response._data,
      });
    },
  }
);

const numberOfNetworks = computed(() => {
  return scoped
    ? data?.value?.data?.Networks[0]?.networks_agg.count
    : data?.value?.data?.Networks_agg?.count;
});

const networks = computed(() => {
  return scoped
    ? data.value?.data?.Networks[0]?.networks
    : data.value?.data?.Networks;
});

function setCurrentPage(pageNumber: number) {
  router.push({
    path: route.path,
    query: { ...route.query, page: pageNumber },
  });
}

function onFilterChange(filters: IFilter[]) {
  const conditions = toPathQueryConditions(filters) || undefined; // undefined is used to remove the query param from the URL;

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

const crumbs: any = {};
crumbs[
  route.params.catalogue as string
] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}`;
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
          <!-- <NavigationIconsMobile :link="" /> -->
          <PageHeader
            title="Networks"
            description="Collaborations of multiple institutions and/or cohorts with a common objective."
            icon="image-diagram"
          >
            <template #prefix>
              <BreadCrumbs :crumbs="crumbs" current="networks" />
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
                v-model:activeName="activeTabName"
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
          <SearchResultsCount :value="numberOfNetworks" label="network" />
          <FilterWell
            :filters="filters"
            @update:filters="onFilterChange"
          ></FilterWell>
          <SearchResultsList>
            <CardList v-if="networks?.length > 0">
              <CardListItem v-for="network in networks" :key="network.name">
                <NetworkCard
                  :network="network"
                  :schema="route.params.schema as string"
                  :compact="activeTabName !== 'detailed'"
                />
              </CardListItem>
            </CardList>
            <div v-else class="flex justify-center pt-3">
              <span class="py-15 text-blue-500">
                No Networks found with current filters
              </span>
            </div>
          </SearchResultsList>
        </template>

        <template v-if="networks?.length > 0" #pagination>
          <Pagination
            :current-page="currentPage"
            :totalPages="Math.ceil(numberOfNetworks / pageSize)"
            @update="setCurrentPage($event)"
          />
        </template>
      </SearchResults>
    </template>
  </LayoutsSearchPage>
</template>

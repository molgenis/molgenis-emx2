<script setup lang="ts">
import type { IFilter, IMgError, activeTabType } from "~/interfaces/types";

const route = useRoute();
const router = useRouter();
const pageSize = 10;

const titlePrefix =
  route.params.catalogue === "all" ? "" : route.params.catalogue + " ";
useHead({ title: titlePrefix + "Data sources" });

const currentPage = computed(() => {
  const queryPageNumber = Number(route.query?.page);
  return !isNaN(queryPageNumber) && typeof queryPageNumber === "number"
    ? Math.round(queryPageNumber)
    : 1;
});
let offset = computed(() => (currentPage.value - 1) * pageSize);

const pageFilterTemplate: IFilter[] = [
  {
    id: "search",
    config: {
      label: "Search in datasources",
      type: "SEARCH",
      initialCollapsed: false,
    },
    search: "",
  },
  {
    id: "areasOfInformation",
    config: {
      label: "Areas of information",
      type: "ONTOLOGY",
      ontologyTableId: "AreasOfInformationDs",
      ontologySchema: "CatalogueOntologies",
      columnId: "areasOfInformation",
    },
    conditions: [],
  },
  {
    id: "dataCategories",
    config: {
      label: "Data categories",
      type: "ONTOLOGY",
      ontologyTableId: "DatasourceTypes",
      ontologySchema: "CatalogueOntologies",
      columnId: "type",
    },
    conditions: [],
  },
];

if ("all" === route.params.catalogue) {
  pageFilterTemplate.push({
    id: "networks",
    config: {
      label: "Networks",
      type: "REF_ARRAY",
      columnId: "networks",
      refTableId: "Networks",
      refFields: {
        key: "id",
        name: "id",
        description: "name",
      },
    },
    conditions: [],
  });
}

const query = computed(() => {
  return `
  query DataSources($filter:DataSourcesFilter, $orderby:DataSourcesorderby){
    DataSources(limit: ${pageSize} offset: ${offset.value} filter:$filter  orderby:$orderby) {
      id
      name
      acronym
      description
      keywords
      numberOfParticipants
      type{name}
    }
    DataSources_agg (filter:$filter){
        count
    }
  }
  `;
});

const orderby = { acronym: "ASC" };

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

const gqlFilter = computed(() => {
  let result: any = {};

  result = buildQueryFilter(filters.value);

  // add hard coded page sepsific filters
  if ("all" !== route.params.catalogue) {
    result["networks"] = { id: { equals: route.params.catalogue } };
  }
  return result;
});

const { data } = await useFetch<any, IMgError>(
  `/${useRoute().params.schema}/graphql`,
  {
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
  }
);

const dataSources = computed(() => data?.value.data.DataSources || []);
const numberOfDataSources = computed(
  () => data?.value.data.DataSources_agg?.count || 0
);

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

type view = "detailed" | "compact";
const activeTabName = computed(() => {
  return (route.query.view as view | undefined) || "detailed";
});

function onActiveTabChange(tabName: activeTabType) {
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
            title="Data sources"
            description="Group of individuals sharing a defining demographic characteristic."
            icon="image-data-warehouse"
          >
            <template #prefix>
              <BreadCrumbs :crumbs="crumbs" current="data sources" />
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
              <SearchResultsViewTabsMobile class="flex xl:hidden">
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
            :value="numberOfDataSources"
            label="data source"
          />
          <FilterWell
            :filters="filters"
            @update:filters="onFilterChange"
          ></FilterWell>
          <SearchResultsList>
            <CardList v-if="dataSources.length > 0">
              <CardListItem
                v-for="datasource in dataSources"
                :key="datasource.name"
              >
                <DataSourceCard
                  :datasource="datasource"
                  :schema="route.params.schema as string"
                  :compact="activeTabName !== 'detailed'"
                  :catalogue="route.params.catalogue as string"
                />
              </CardListItem>
            </CardList>
            <div v-else class="flex justify-center pt-3">
              <span class="py-15 text-blue-500">
                No Datasources found with current filters
              </span>
            </div>
          </SearchResultsList>
        </template>

        <template v-if="dataSources.length > 0" #pagination>
          <Pagination
            :current-page="currentPage"
            :totalPages="Math.ceil(numberOfDataSources / pageSize)"
            @update="setCurrentPage($event)"
          />
        </template>
      </SearchResults>
    </template>
  </LayoutsSearchPage>
</template>

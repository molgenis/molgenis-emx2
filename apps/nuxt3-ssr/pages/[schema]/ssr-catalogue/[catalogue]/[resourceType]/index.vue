<script setup lang="ts">
import type { ISchemaMetaData, ITableMetaData } from "meta-data-utils";
import type { IFilter, IMgError, activeTabType } from "~~/interfaces/types";
import {
  buildRecordListQueryFields,
  extractExternalSchemas,
  extractKeyFromRecord,
} from "meta-data-utils";

const route = useRoute();
const router = useRouter();

const pageSize = 10;
const tableId: string = route.params.resourceType as string;
const schemaId = route.params.schema.toString();
const metadata = await fetchMetadata(schemaId);

const tableMetaData = computed(() => {
  const result = metadata.tables.find(
    (t: ITableMetaData) => t.id.toLowerCase() === tableId.toLowerCase()
  );
  if (!result) {
    throw new Error(`Table with id ${tableId} not found in schema ${schemaId}`);
  }
  return result;
});

const resourceType = tableMetaData.value.id;

const resourceAgg: string = resourceType + "_agg";

const externalschemaIds: string[] = extractExternalSchemas(metadata);
const externalSchemas = await Promise.all(externalschemaIds.map(fetchMetadata));
const schemas = externalSchemas.reduce(
  (acc: Record<string, ISchemaMetaData>, schema) => {
    acc[schema.id] = schema;
    return acc;
  },
  { [schemaId]: metadata }
);

const description = tableMetaData.value?.description;

const pageFilterTemplate: IFilter[] = [
  {
    id: "search",
    config: {
      label: `Search in ${tableMetaData.value.name}`,
      type: "SEARCH",
      searchTables: [],
      initialCollapsed: false,
    },
    search: "",
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

const currentPage = ref(1);

async function setCurrentPage(pageNumber: number) {
  await navigateTo({ query: { page: pageNumber } });
  window.scrollTo({ top: 0 });
}

if (route.query?.page) {
  const queryPageNumber = Number(route.query?.page);
  currentPage.value =
    typeof queryPageNumber === "number" ? Math.round(queryPageNumber) : 1;
}
let offset = computed(() => (currentPage.value - 1) * pageSize);

// build resource query for cards

const fields = buildRecordListQueryFields(
  tableMetaData.value.id,
  schemaId,
  schemas
);

const query = computed(() => {
  return `
  query ${resourceType}($filter:${resourceType}Filter, $orderby:${resourceType}orderby){
    ${resourceType}(limit: ${pageSize} offset: ${offset.value} filter:$filter  orderby:$orderby) {
      ${fields}
    }
    ${resourceType}_agg (filter:$filter){
        count
    }
  }
  `;
});
const orderby = {};

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

function buildRecordId(record: any) {
  return extractKeyFromRecord(
    record,
    tableMetaData.value.id,
    schemaId,
    schemas
  );
}
let crumbs: Record<string, string> = {};
if (route.params.catalogue) {
  crumbs[
    route.params.catalogue.toString()
  ] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}`;
} else {
  crumbs = {
    Home: `/${route.params.schema}/ssr-catalogue`,
    Browse: `/${route.params.schema}/ssr-catalogue/all`,
  };
}
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
          <PageHeader :title="tableMetaData.label" :description="description">
            <template #prefix>
              <BreadCrumbs :crumbs="crumbs" :current="resourceType" />
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
          <FilterWell
            :filters="filters"
            @update:filters="onFilterChange"
          ></FilterWell>
          <SearchResultsList>
            <CardList v-if="data?.data?.[resourceType]?.length > 0">
              <CardListItem
                v-for="resource in data?.data?.[resourceType]"
                :key="resource.name"
              >
                <ResourceCard
                  :resource="resource"
                  :schema="schemaId"
                  :table-id="tableMetaData.id"
                  :compact="activeTabName !== 'detailed'"
                  :resourceId="buildRecordId(resource)"
                />
              </CardListItem>
            </CardList>
            <div v-else class="flex justify-center pt-3">
              <span class="py-15 text-blue-500">
                No {{ tableMetaData.name }} found with current filters
              </span>
            </div>
          </SearchResultsList>
        </template>

        <template v-if="data?.data?.[resourceType]?.length > 0" #pagination>
          <Pagination
            :current-page="currentPage"
            :totalPages="Math.ceil(data?.data?.[resourceAgg].count / pageSize)"
            @update="setCurrentPage($event)"
          />
        </template>
      </SearchResults>
    </template>
  </LayoutsSearchPage>
</template>

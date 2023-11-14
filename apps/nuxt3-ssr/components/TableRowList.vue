<script setup lang="ts">
import type { ISchemaMetaData, ITableMetaData } from "meta-data-utils";
import type { IFilter } from "~~/interfaces/types";
import {
  buildRecordListQueryFields,
  extractExternalSchemas,
  extractKeyFromRecord,
} from "meta-data-utils";

const config = useRuntimeConfig();
const route = useRoute();
const router = useRouter();

const pageSize = 10;
const tableId: string = route.params.resourceType as string;
const schemaId = route.params.schema.toString();
const metadata = await fetchMetadata(schemaId);

const tableMetaData = computed(() => {
  const result = metadata.tables.find((t: ITableMetaData) => t.id === tableId);
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

let activeName = ref("detailed");
let filters: IFilter[] = reactive([
  {
    title: `Search in ${tableMetaData.value.name}`,
    columnType: "_SEARCH",
    search: "",
    searchTables: [],
    initialCollapsed: false,
  },
]);

const currentPage = ref(1);

function setCurrentPage(pageNumber: number) {
  router.push({ path: route.path, query: { page: pageNumber } });
  currentPage.value = pageNumber;
}

if (route.query?.page) {
  const queryPageNumber = Number(route.query?.page);
  currentPage.value =
    typeof queryPageNumber === "number" ? Math.round(queryPageNumber) : 1;
}
let offset = computed(() => (currentPage.value - 1) * pageSize);

watch(filters, () => {
  setCurrentPage(1);
});

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
let search = computed(() => {
  // @ts-ignore
  return filters.find((f) => f.columnType === "_SEARCH").search;
});
const filter = computed(() => buildQueryFilter(filters, search.value));

console.log("query: ", query.value);
const { data, pending, error, refresh } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    key: `${tableId}-list-${offset.value}`,
    baseURL: config.public.apiBase,
    method: "POST",
    body: {
      query,
      variables: { orderby, filter },
    },
  }
);

function buildRecordId(record: any) {
  return extractKeyFromRecord(
    record,
    tableMetaData.value.id,
    schemaId,
    schemas
  );
}
let crumbs: Record<string, string> = {
  Home: "..",
  Browse: `../browse`,
};
</script>
<template>
  <LayoutsSearchPage>
    <template #side>
      <FilterSidebar title="Filters" :filters="filters" />
    </template>
    <template #main>
      <SearchResults>
        <template #header>
          <!-- <NavigationIconsMobile :link="" /> -->
          <PageHeader :title="resourceName" :description="description">
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
                v-model:activeName="activeName"
              />
              <SearchResultsViewTabsMobile class="flex xl:hidden">
                <FilterSidebar
                  title="Filters"
                  :filters="filters"
                  :mobileDisplay="true"
                />
              </SearchResultsViewTabsMobile>
            </template>
          </PageHeader>
        </template>

        <template #search-results>
          <FilterWell :filters="filters"></FilterWell>
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
                  :compact="activeName !== 'detailed'"
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
        {{ error }}
      </SearchResults>
    </template>
  </LayoutsSearchPage>
</template>

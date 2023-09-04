<script setup lang="ts">
import { IFilter, ITableMetaData } from "~/interfaces/types";
const config = useRuntimeConfig();
const route = useRoute();
const router = useRouter();

const pageSize = 10;
const resourceName: string = route.params.resourceType as string;
const resourceType: string =
  resourceName.charAt(0).toUpperCase() + resourceName.slice(1);
const resourceAgg: string = resourceType + "_agg";

const metadata = await fetchMetadata();
const tableMetaData = metadata.tables.find(
  (t: ITableMetaData) => t.name === resourceType
);
const description = tableMetaData?.descriptions?.[0]?.value;

let activeName = ref("detailed");
let filters: IFilter[] = reactive([
  {
    title: `Search in ${resourceName}`,
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
const query = computed(() => {
  return `
  query ${resourceType}($filter:${resourceType}Filter, $orderby:${resourceType}orderby){
    ${resourceType}(limit: ${pageSize} offset: ${offset.value} filter:$filter  orderby:$orderby) {
      id
      name
      acronym
      description
    }
    ${resourceType}_agg (filter:$filter){
        count
    }
  }
  `;
});
const orderby = { acronym: "ASC" };
let search = computed(() => {
  // @ts-ignore
  return filters.find((f) => f.columnType === "_SEARCH").search;
});
const filter = computed(() => buildQueryFilter(filters, search.value));

const { data, pending, error, refresh } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    key: `cohorts-${offset.value}`,
    baseURL: config.public.apiBase,
    method: "POST",
    body: {
      query,
      variables: { orderby, filter },
    },
  }
);
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
          <PageHeader
            :title="resourceName"
            :description="description"
            icon="image-link"
          >
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
                  :schema="route.params.schema"
                  :resource-name="resourceName"
                  :compact="activeName !== 'detailed'"
                />
              </CardListItem>
            </CardList>
            <div v-else class="flex justify-center pt-3">
              <span class="py-15 text-blue-500">
                No {{ resourceName }} found with current filters
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

<script setup lang="ts">
const route = useRoute();
const router = useRouter();
const config = useRuntimeConfig();
const pageSize = 20;

useHead({ title: "Catalogues" });

const currentPage = ref(1);
if (route.query?.page) {
  const queryPageNumber = Number(route.query?.page);
  currentPage.value =
    typeof queryPageNumber === "number" ? Math.round(queryPageNumber) : 1;
}
let offset = computed(() => (currentPage.value - 1) * pageSize);

let filters: IFilter[] = reactive([
  {
    title: "Search in networks",
    columnType: "_SEARCH",
    search: "",
    initialCollapsed: false,
  },
  {
    title: "Countries",
    refTable: "Countries",
    columnName: "countries",
    columnType: "ONTOLOGY",
    conditions: [],
  },
  {
    title: "Organisations",
    columnName: "dataCategories",
    columnType: "REF_ARRAY",
    refTable: "Organisations",
    refFields: {
      key: "id",
      name: "id",
      description: "name",
    },
    conditions: [],
  },
  {
    title: "Topics",
    refTable: "AgeGroups",
    columnName: "ageGroups",
    columnType: "ONTOLOGY",
    filterTable: "collectionEvents",
    conditions: [],
  },
]);

let search = computed(() => {
  // @ts-ignore
  return filters.find((f) => f.columnType === "_SEARCH").search;
});

const query = computed(() => {
  return `
  query Networks($filter:NetworksFilter, $orderby:Networksorderby){
    Networks(limit: ${pageSize} offset: ${offset.value} filter:$filter  orderby:$orderby) {
      id
      name
      acronym
      description
      logo {
        id
        size
        extension
        url
      }
    }
    Networks_agg (filter:$filter){
      count
    }
  }
  `;
});

const orderby = { acronym: "ASC" };

const filter = computed(() => buildQueryFilter(filters, search.value));

let graphqlURL = computed(() => `/${route.params.schema}/catalogue/graphql`);
const { data, pending, error, refresh } = await useFetch(graphqlURL.value, {
  key: `networks-${offset.value}`,
  baseURL: config.public.apiBase,
  method: "POST",
  body: {
    query,
    variables: { orderby, filter },
  },
});

function setCurrentPage(pageNumber: number) {
  router.push({ path: route.path, query: { page: pageNumber } });
  currentPage.value = pageNumber;
}

watch(filters, () => {
  setCurrentPage(1);
});

let activeName = ref("detailed");
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
            title="Catalogues"
            description="Collaborations of multiple institutions and/or cohorts with a common objective."
          >
            <template #prefix>
              <BreadCrumbs :crumbs="{ Home: '.' }" current="catalogues" />
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
              <SearchResultsViewTabsMobile
                class="flex xl:hidden"
                v-model:activeName="activeName"
              >
                <FilterSidebar title="Filters" :filters="filters" />
              </SearchResultsViewTabsMobile>
            </template>
          </PageHeader>
        </template>

        <template #search-results>
          <FilterWell :filters="filters"></FilterWell>
          <SearchResultsList>
            <CardList v-if="data?.data?.Networks?.length > 0">
              <CardListItem
                v-for="network in data?.data?.Networks"
                :key="network.name"
              >
                <NetworkCard
                  :network="network"
                  :schema="route.params.schema"
                  :compact="activeName !== 'detailed'"
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

        <template v-if="data?.data?.Networks?.length > 0" #pagination>
          <Pagination
            :current-page="currentPage"
            :totalPages="Math.ceil(data?.data?.Networks_agg.count / pageSize)"
            @update="setCurrentPage($event)"
          />
        </template>
      </SearchResults>
    </template>
  </LayoutsSearchPage>
</template>

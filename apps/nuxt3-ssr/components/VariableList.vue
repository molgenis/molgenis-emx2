<script setup lang="ts">
const route = useRoute();
const router = useRouter();
const config = useRuntimeConfig();
const pageSize = 10;

useHead({ title: "Variables" });

const currentPage = ref(1);
if (route.query?.page) {
  const queryPageNumber = Number(route.query?.page);
  currentPage.value =
    typeof queryPageNumber === "number" ? Math.round(queryPageNumber) : 1;
}
let offset = computed(() => (currentPage.value - 1) * pageSize);

let filters: IFilter[] = reactive([
  {
    title: "Search in variables",
    columnType: "_SEARCH",
    search: "",
    initialCollapsed: false,
  },
  {
    title: "Resource",
    refTable: "Resources",
    columnName: "resource",
    columnType: "REF_ARRAY",
    conditions: [],
  },
  {
    title: "Dataset",
    refTable: "Datasets",
    columnName: "dataset",
    columnType: "REF_ARRAY",
    conditions: [],
  },
  {
    title: "Formats",
    refTable: "Formats",
    columnName: "format",
    columnType: "ONTOLOGY",
    conditions: [],
  },
]);

let search = computed(() => {
  // @ts-ignore
  return filters.find((f) => f.columnType === "_SEARCH").search;
});

const query = computed(() => {
  return `
  query Variables($filter:VariablesFilter, $orderby:Variablesorderby){
    Variables(limit: ${pageSize} offset: ${offset.value} filter:$filter  orderby:$orderby) {
      name
      label
      description
    }
    Variables_agg (filter:$filter){
      count
    }
  }
  `;
});

const orderby = { label: "ASC" };

const filter = computed(() => {
  let result = buildQueryFilter(filters, search.value);
  if (route.params.catalogue) {
    result._and["networks"] = { resource: { equals: route.params.catalogue } };
  }
  return result;
});

let graphqlURL = computed(() => `/${route.params.schema}/catalogue/graphql`);
const { data, pending, error, refresh } = await useFetch(graphqlURL.value, {
  key: `variables-${offset.value}`,
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

let crumbs: any = {};
if (route.params.catalogue) {
  crumbs[
    `${route.params.catalogue}`
  ] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}`;
} else {
  crumbs = {
    Home: `/${route.params.schema}/ssr-catalogue`,
  };
}
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
            title="Variables"
            description="A complete overview of available variables."
            icon="image-diagram"
          >
            <template #prefix>
              <BreadCrumbs :crumbs="crumbs" current="variables" />
            </template>
            <template #suffix>
              <SearchResultsViewTabs
                class="hidden xl:flex"
                buttonLeftLabel="List of variables"
                buttonLeftName="detailed"
                buttonLeftIcon="view-compact"
                buttonRightLabel="Harmonizations"
                buttonRightName="compact"
                buttonRightIcon="view-table"
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
            <CardList v-if="data?.data?.Variables?.length > 0">
              <CardListItem
                v-for="variable in data?.data?.Variables"
                :key="variable.name"
              >
                <VariableCard
                  :variable="variable"
                  :schema="route.params.schema"
                  :compact="activeName !== 'detailed'"
                />
              </CardListItem>
            </CardList>
            <div v-else class="flex justify-center pt-3">
              <span class="py-15 text-blue-500">
                No variables found with current filters
              </span>
            </div>
          </SearchResultsList>
        </template>

        <template v-if="data?.data?.Variables?.length > 0" #pagination>
          <Pagination
            :current-page="currentPage"
            :totalPages="Math.ceil(data?.data?.Variables_agg.count / pageSize)"
            @update="setCurrentPage($event)"
          />
        </template>
      </SearchResults>
    </template>
  </LayoutsSearchPage>
</template>

<script setup lang="ts">
const route = useRoute();
const config = useRuntimeConfig();
const pageSize = 10;
const currentPage = ref(1);
let offset = computed(() => (currentPage.value - 1) * pageSize);

let filters = reactive([
  {
    title: "Search in cohorts",
    columnType: "_SEARCH",
    search: "",
    searchTables: ["collectionEvents", "subcohorts"],
    initialCollapsed: false,
  },
  {
    title: "Areas of information",
    refTable: "AreasOfInformation",
    columnName: "areasOfInformation",
    columnType: "ONTOLOGY",
    filterTable: "collectionEvents",
    conditions: [],
  },
  {
    title: "Data categories",
    refTable: "DataCategories",
    columnName: "dataCategories",
    columnType: "ONTOLOGY",
    filterTable: "collectionEvents",
    conditions: [],
  },
  {
    title: "Population age groups",
    refTable: "AgeGroups",
    columnName: "ageGroups",
    columnType: "ONTOLOGY",
    filterTable: "collectionEvents",
    conditions: [],
  },
  {
    title: "Sample categories",
    refTable: "SampleCategories",
    columnName: "sampleCategories",
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
  query Cohorts($filter:CohortsFilter, $orderby:Cohortsorderby){
    Cohorts(limit: ${pageSize} offset: ${offset.value} search:"${search.value}" filter:$filter  orderby:$orderby) {
      pid
      name
      acronym
      description
      keywords
      type {
          name
      }
      design {
          name
      }
      institution {
          name
          acronym
      }
    }
    Cohorts_agg (filter:$filter, search:"${search.value}"){
        count
    }
  }
  `;
});

const orderby = { name: "ASC" };

function buildFilterVariables() {
  const filtersVariables = filters.reduce<
    Record<string, Record<string, object | string>>
  >((accum, filter) => {
    if (filter.filterTable && filter?.conditions?.length) {
      if (!accum[filter.filterTable]) {
        accum[filter.filterTable] = {};
      }
      accum[filter.filterTable][filter.columnName] = {
        equals: filter.conditions,
      };
    }

    return accum;
  }, {});

  return filtersVariables;
}

const filter = computed(() => {
  // build the active filters
  const filterVariables = buildFilterVariables();

  // append search to the sub tables if set
  const searchTables = filters.find((f) => f.columnType === "_SEARCH")
    ?.searchTables;

  if (searchTables) {
    searchTables.forEach((searchTable) => {
      if (search.value) {
        if (Object.keys(filterVariables).includes(searchTable)) {
          filterVariables[searchTable]["_search"] = search.value;
        } else {
          filterVariables[searchTable] = { _search: search.value };
        }
      }
    });
  }

  return filterVariables;
});

let graphqlURL = computed(() => `/${route.params.schema}/catalogue/graphql`);
const { data, pending, error, refresh } = await useFetch(graphqlURL.value, {
  key: `cohorts-${offset.value}`,
  baseURL: config.public.apiBase,
  method: "POST",
  body: {
    query,
    variables: { orderby, filter },
  },
});

function setCurrentPage(pageNumber: number) {
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
      <SearchFilter title="Filters" :filters="filters" />
    </template>
    <template #main>
      <SearchResults>
        <template #header>
          <NavigationIconsMobile />
          <PageHeader title="Cohorts" description="Group of individuals sharing a defining demographic characteristic."
            icon="image-link">
            <template #suffix>
              <SearchResultsViewTabs class="hidden xl:flex" buttonLeftLabel="Detailed" buttonLeftName="detailed"
                buttonLeftIcon="view-normal" buttonRightLabel="Compact" buttonRightName="compact"
                buttonRightIcon="view-compact" v-model:activeName="activeName" />
              <SearchResultsViewTabsMobile class="flex xl:hidden" v-model:activeName="activeName">
                <SearchFilter title="Filters" :filters="filters" />
              </SearchResultsViewTabsMobile>
            </template>
          </PageHeader>
        </template>

        <template #search-results>
          <FilterWell :filters="filters"></FilterWell>
          <SearchResultsList>
            <CardList v-if="data?.data?.Cohorts?.length > 0">
              <CardListItem v-for="cohort in data?.data?.Cohorts" :key="cohort.name">
                <CohortCard :cohort="cohort" :schema="route.params.schema" :compact="activeName !== 'detailed'" />
              </CardListItem>
            </CardList>
            <div v-else class="flex justify-center pt-3">
              <span class="py-15 text-blue-500">
                No Cohorts found with current filters
              </span>
            </div>
          </SearchResultsList>
        </template>

        <template #pagination>
          <Pagination :current-page="currentPage" :totalPages="Math.ceil(data?.data?.Cohorts_agg.count / pageSize)"
            @update="setCurrentPage($event)" />
        </template>
      </SearchResults>
    </template>
  </LayoutsSearchPage>
</template>

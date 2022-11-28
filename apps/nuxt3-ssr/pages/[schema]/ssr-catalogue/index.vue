<script setup lang="ts">
const route = useRoute();
const config = useRuntimeConfig();
const pageSize = 5;
const currentPage = ref(1);
let offset = computed(() => (currentPage.value - 1) * pageSize);

let filters = reactive([
  {
    title: "Areas of information",
    refTable: "AreasOfInformation",
    columnName: "areasOfInformation",
    columnType: "ONTOLOGY",
    conditions: []
  },
  {
    title: "Data categories",
    refTable: "DataCategories",
    columnName: "dataCategories",
    columnType: "ONTOLOGY",
    conditions: []
  },
  {
    title: "Population age groups",
    refTable: "AgeGroups",
    columnName: "ageGroups",
    columnType: "ONTOLOGY",
    conditions: []
  },
  {
    title: "Sample categories",
    refTable: "SampleCategories",
    columnName: "sampleCategories",
    columnType: "ONTOLOGY",
    conditions: []
  },
])

const query = computed(() => {
  return `
  query Cohorts($filter:CohortsFilter, $orderby:Cohortsorderby){
    Cohorts(limit: ${pageSize} offset: ${offset.value} filter:$filter  orderby:$orderby) {
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
    Cohorts_agg (filter:$filter){
        count
    }
  }
  `;
});

const orderby = { "name": "ASC" }

function buildFilterVariables() {
  return filters.reduce<Record<string, object>>((accum, filter) => {
    if (filter.conditions.length) {
      accum[filter.columnName] = { equals: filter.conditions }
    }
    return accum
  }, {})
}

const filter = computed(() => {
  return { collectionEvents: buildFilterVariables() }
})

let graphqlURL = computed(() => `/${route.params.schema}/catalogue/graphql`);
const { data, pending, error, refresh } = await useFetch(
  graphqlURL.value,
  {
    key: `cohorts-${offset.value}`,
    baseURL: config.public.apiBase,
    method: "POST",
    body: {
      query,
      variables: { orderby, filter }
    },
  }
);

function setCurrentPage(pageNumber: number) {
  currentPage.value = pageNumber;
}

watch(filters, () => {
  setCurrentPage(1)
})
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
                buttonRightIcon="view-compact" activeName="detailed" />
              <SearchResultsViewTabsMobile class="flex xl:hidden" />
            </template>
          </PageHeader>
        </template>

        <template #search-results>
          <SearchResultsList>
            <CardList>
              <CardListItem v-for="cohort in data?.data?.Cohorts" :key="cohort.name">
                <CohortCard :cohort="cohort" :schema="route.params.schema" />
              </CardListItem>
            </CardList>
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
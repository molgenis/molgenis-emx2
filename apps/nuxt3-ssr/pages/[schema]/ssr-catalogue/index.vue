<script setup lang="ts">
const route = useRoute();
const config = useRuntimeConfig();
const pageSize = 5;
const currentPage = ref(1);
let offset = computed(() => (currentPage.value - 1) * pageSize);

const query = computed(() => {
  return `
  {
      Cohorts(limit: ${pageSize} offset: ${offset.value}) {
      pid
      name
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
      Cohorts_agg {
      count
      }
  }
  `;
});

let graphqlURL = computed(() => `/${route.params.schema}/catalogue/graphql`);

const { data, pending, error, refresh } = await useFetch(
  graphqlURL.value,
  {
    key: `cohorts-${offset.value}`,
    baseURL: config.public.apiBase,
    method: "POST",
    body: {
      query,
    },
  }
);

async function handlePagination(pageNumber: number) {
  currentPage.value = pageNumber;
}

let filters = reactive([
  {
    title: "Areas of information",
    refTable: "AreasOfInformation",
    columnType: "STRING",
    conditions: []
  },
  {
    title: "Data categories",
    refTable: "DataCategories",
    columnType: "STRING",
    conditions: []
  },
  {
    title: "Population age groups",
    refTable: "AgeGroups",
    columnType: "STRING",
    conditions: []
  },
  {
    title: "Sample categories",
    refTable: "SampleCategories",
    columnType: "STRING",
    conditions: []
  },
])

watch( filters, () => {
  console.log('conditions: ' + JSON.stringify(filters)),
  {immediate: false, deep: true}
})
</script>

<template>
  <LayoutsSearchPage>
    <template #side>
      <SearchFilter title="Filters">
        <!-- <SearchFilterGroup title="Search in networks" /> -->
        <SearchFilterGroup v-for="filter in filters" :title="filter.title" :table-name="filter.refTable"
          v-model="filter.conditions" />
      </SearchFilter>
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
            @update="handlePagination($event)" />
        </template>
      </SearchResults>
    </template>
  </LayoutsSearchPage>
</template>
<script setup lang="ts">
const route = useRoute()
const config = useRuntimeConfig()
const pageSize = 5
const currentPage = ref(1)
let offset = computed(() => currentPage.value * pageSize)

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
  `
})

const { data, pending, error, refresh } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    key: `cohorts-${offset.value}`,
    baseURL: config.public.apiBase,
    method: "POST",
    body: {
      query
    }
  }
);

async function handlePagination(pageNumber: number) {
  currentPage.value = pageNumber
}

</script>

<template>
  <LayoutsSearchPage>
    <template #side>
      <SearchFilter title="Filters">
        <SearchFilterGroup title="Search in networks" />
        <SearchFilterGroup title="Countries" />
        <SearchFilterGroup title="Institutions" />
      </SearchFilter>
    </template>
    <template #main>
      <SearchResults>
        <template #header>
          <PageHeader title="Cohorts" description="Group of individuals sharing a defining demographic characteristic."
            icon="image-link">
            <template #suffix>
              <SearchResultsViewTabs buttonLeftLabel="Detailed" buttonLeftName="detailed" buttonLeftIcon="view-normal"
                buttonRightLabel="Compact" buttonRightName="compact" buttonRightIcon="view-compact"
                activeName="compact" />
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
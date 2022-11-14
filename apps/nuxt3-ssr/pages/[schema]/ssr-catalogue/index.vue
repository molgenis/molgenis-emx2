<script setup lang="ts">
const route = useRoute()
const config = useRuntimeConfig()
const catalogueQuery = `
  {
    Cohorts {
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

const { data, pending, error, refresh } = await useFetch(
    `/${route.params.schema}/catalogue/graphql`,
    {
        baseURL: config.public.apiBase,
        method: "POST",
        body: {
            query: catalogueQuery
        }
    }
);

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
                    <PageHeader title="Cohorts"
                        description="Group of individuals sharing a defining demographic characteristic."
                        icon="image-link">
                        <template #suffix>
                            <SearchResultsViewTabs buttonLeftLabel="Detailed" buttonLeftName="detailed"
                                buttonLeftIcon="view-normal" buttonRightLabel="Compact" buttonRightName="compact"
                                buttonRightIcon="view-compact" activeName="compact" />
                        </template>
                    </PageHeader>
                </template>

                <template #search-results>
                    <SearchResultsList>
                        <CardList>
                            <CardListItem v-for="cohort in data.data.Cohorts" :key="cohort.name">
                                <CohortCard :cohort="cohort" :schema="route.params.schema" />
                            </CardListItem>
                        </CardList>
                    </SearchResultsList>
                </template>

                <template #pagination>
                    <Pagination />
                </template>
            </SearchResults>
        </template>
    </LayoutsSearchPage>
</template>
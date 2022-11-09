<script setup>
// const query = `{ Schemas { name description } }`;
// const { data, error } = await fetchGql("apps/graphql", query);
// console.log(data);
// console.log(error);
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
console.log(catalogueQuery)
const catalogueResp = await fetchGql(
  "catalogue/catalogue/graphql",
  catalogueQuery
);
console.log(catalogueResp);
const cohortCount = catalogueResp.data.Cohorts_agg.count;
const cohorts = catalogueResp.data.Cohorts;
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
          <span>{{ cohortCount }}</span>
          <SearchResultsList>
            <CardList>
              <CardListItem v-for="cohort in cohorts" :key="cohort.name">
                <CohortCard :cohort="cohort" />
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
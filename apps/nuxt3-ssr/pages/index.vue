<script setup>
const query = `{ Schemas { name description } }`;
const { data, error } = await fetchGql("apps/graphql", query);
console.log(data);
console.log(error);
const catalogueQuery = `{_schema{name}, Cohorts_agg{count}}`;
const catalogueResp = await fetchGql(
  "catalogue/catalogue/graphql",
  catalogueQuery
);
console.log(catalogueResp);
const cohortCount = catalogueResp.data.Cohorts_agg.count;
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
            <PageHeader
              title="Cohorts"
              description="Group of individuals sharing a defining demographic characteristic."
              icon="image-link"
            >
              <template #suffix>
                <SearchResultsViewTabs
                  buttonLeftLabel="Detailed"
                  buttonLeftName="detailed"
                  buttonLeftIcon="view-normal"
                  buttonRightLabel="Compact"
                  buttonRightName="compact"
                  buttonRightIcon="view-compact"
                  activeName="compact"
                />
              </template>
            </PageHeader>
          </template>

          <template #search-results>
            <SearchResultsList>
              <CardList>
                <CardListItem><CohortCard /></CardListItem>
                <CardListItem><CohortCard /></CardListItem>
                <CardListItem><CohortCard /></CardListItem>
                <CardListItem><CohortCard /></CardListItem>
                <CardListItem><CohortCard /></CardListItem>
                <CardListItem><CohortCard /></CardListItem>
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
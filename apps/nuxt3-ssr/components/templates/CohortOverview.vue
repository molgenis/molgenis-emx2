<script setup>
import AppWrapper from "../AppWrapper.vue";
import AppHeader from "../AppHeader.vue";
import SearchPage from "../layouts/SearchPage.vue";
import SearchFilterGroup from "../SearchFilterGroup.vue";
import SearchFilter from "../SearchFilter.vue";
import SearchResults from "../SearchResults.vue";
import SearchResultsViewTabs from "../SearchResultsViewTabs.vue";
import Pagination from "../Pagination.vue";
import CohortCard from "../CohortCard.vue";
import PageHeader from "../PageHeader.vue";
import SearchResultsList from "../SearchResultsList.vue";
import CardList from "../CardList.vue";
import CardListItem from "../CardListItem.vue";
</script>

<template>
  <AppWrapper>
    <template #header>
      <AppHeader />
    </template>

    <SearchPage>
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
                <SearchResultsViewTabs />
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
    </SearchPage>
  </AppWrapper>
</template>

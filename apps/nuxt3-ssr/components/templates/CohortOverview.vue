<script setup>
import AppWrapper from "../AppWrapper.vue";
import AppHeader from "../AppHeader.vue";
import SearchPage from "../layouts/SearchPage.vue";
import SearchResults from "../SearchResults.vue";
import SearchResultsViewTabs from "../SearchResultsViewTabs.vue";
import SearchResultsViewTabsMobile from "../SearchResultsViewTabsMobile.vue";
import Pagination from "../Pagination.vue";
import CohortCard from "../CohortCard.vue";
import PageHeader from "../PageHeader.vue";
import SearchResultsList from "../SearchResultsList.vue";
import CardList from "../CardList.vue";
import CardListItem from "../CardListItem.vue";
import NavigationIconsMobile from "../NavigationIconsMobile.vue";
const data = {
  AgeGroups: [
    {
      order: 1,
      name: "Prenatal",
      code: "<birth",
      mg_insertedBy: "admin",
      mg_insertedOn: "2022-05-04T14:39:07.451638",
      mg_updatedBy: "admin",
      mg_updatedOn: "2022-05-04T14:39:07.451638",
    },
    {
      order: 2,
      name: "All ages",
      code: "all ages",
      children: [
        {
          name: "Infant (0-23 months)",
          code: "0-23 months",
          children: [
            {
              order: 4,
              name: "Newborn (0-1 months)",
              code: "0-1 months",
              parent: {
                name: "Infant (0-23 months)",
              },
              mg_insertedBy: "admin",
              mg_insertedOn: "2022-05-04T14:39:07.451638",
              mg_updatedBy: "admin",
              mg_updatedOn: "2022-05-04T14:39:07.451638",
            },
            {
              order: 5,
              name: "Infants and toddlers (2-23 months)",
              code: "2-23 months",
              parent: {
                name: "Infant (0-23 months)",
              },
              mg_insertedBy: "admin",
              mg_insertedOn: "2022-05-04T14:39:07.451638",
              mg_updatedBy: "admin",
              mg_updatedOn: "2022-05-04T14:39:07.451638",
            },
          ],
        },
        {
          order: 6,
          name: "Child (2-12 years)",
          code: "2-12 years",
          parent: {
            name: "All ages",
          },
          mg_insertedBy: "admin",
          mg_insertedOn: "2022-05-04T14:39:07.451638",
          mg_updatedBy: "admin",
          mg_updatedOn: "2022-05-04T14:39:07.451638",
        },
        {
          order: 7,
          name: "Adolescent (13-17 years)",
          code: "13-17 years",
          parent: {
            name: "All ages",
          },
          mg_insertedBy: "admin",
          mg_insertedOn: "2022-05-04T14:39:07.451638",
          mg_updatedBy: "admin",
          mg_updatedOn: "2022-05-04T14:39:07.451638",
        },
        {
          order: 8,
          name: "Adult (18+ years)",
          code: "18+ years",
          parent: {
            name: "All ages",
          },
          children: [
            {
              order: 9,
              name: "Young adult (18-24 years)",
              code: "18-24 years",
              parent: {
                name: "Adult (18+ years)",
              },
              mg_insertedBy: "admin",
              mg_insertedOn: "2022-05-04T14:39:07.451638",
              mg_updatedBy: "admin",
              mg_updatedOn: "2022-05-04T14:39:07.451638",
            },
            {
              order: 10,
              name: "Adult (25-44 years)",
              code: "25-44 years",
              parent: {
                name: "Adult (18+ years)",
              },
              mg_insertedBy: "admin",
              mg_insertedOn: "2022-05-04T14:39:07.451638",
              mg_updatedBy: "admin",
              mg_updatedOn: "2022-05-04T14:39:07.451638",
            },
            {
              order: 11,
              name: "Middle-aged (45-64 years)",
              code: "45-64 years",
              parent: {
                name: "Adult (18+ years)",
              },
              mg_insertedBy: "admin",
              mg_insertedOn: "2022-05-04T14:39:07.451638",
              mg_updatedBy: "admin",
              mg_updatedOn: "2022-05-04T14:39:07.451638",
            },
            {
              order: 12,
              name: "Aged (65+ years)",
              code: "65+ years",
              parent: {
                name: "Adult (18+ years)",
              },
              children: [
                {
                  order: 13,
                  name: "Aged (65-79 years)",
                  code: "65-79 years",
                  parent: {
                    name: "Aged (65+ years)",
                  },
                  mg_insertedBy: "admin",
                  mg_insertedOn: "2022-05-04T14:39:07.451638",
                  mg_updatedBy: "admin",
                  mg_updatedOn: "2022-05-04T14:39:07.451638",
                },
                {
                  order: 14,
                  name: "Aged (80+ years)",
                  code: "80+ years",
                  parent: {
                    name: "Aged (65+ years)",
                  },
                  mg_insertedBy: "admin",
                  mg_insertedOn: "2022-05-04T14:39:07.451638",
                  mg_updatedBy: "admin",
                  mg_updatedOn: "2022-05-04T14:39:07.451638",
                },
              ],
              mg_insertedBy: "admin",
              mg_insertedOn: "2022-05-04T14:39:07.451638",
              mg_updatedBy: "admin",
              mg_updatedOn: "2022-05-04T14:39:07.451638",
            },
          ],
          mg_insertedBy: "admin",
          mg_insertedOn: "2022-05-04T14:39:07.451638",
          mg_updatedBy: "admin",
          mg_updatedOn: "2022-05-04T14:39:07.451638",
        },
      ],
      mg_insertedBy: "admin",
      mg_insertedOn: "2022-05-04T14:39:07.451638",
      mg_updatedBy: "admin",
      mg_updatedOn: "2022-05-04T14:39:07.451638",
    },
  ],
};

const json = Object.values(data)[0];
</script>

<template>
  <AppWrapper>
    <template #header>
      <AppHeader />
    </template>

    <SearchPage>
      <template #side>
        <FilterSidebar title="Filters">
          <FilterOntology title="Search in networks" :json="json" />
          <FilterOntology title="Countries" :json="json" />
          <FilterOntology title="Institutions" :json="json" />
          <FilterOntology title="Topics" :json="json" />
        </FilterSidebar>
      </template>
      <template #main>
        <SearchResults>
          <template #header>
            <NavigationIconsMobile />
            <PageHeader
              title="Cohorts"
              description="Group of individuals sharing a defining demographic characteristic."
              icon="image-link">
              <template #suffix>
                <SearchResultsViewTabs
                  class="hidden xl:flex"
                  buttonLeftLabel="Detailed"
                  buttonLeftName="detailed"
                  buttonLeftIcon="view-normal"
                  buttonRightLabel="Compact"
                  buttonRightName="compact"
                  buttonRightIcon="view-compact"
                  activeName="detailed" />
                <SearchResultsViewTabsMobile class="flex xl:hidden" />
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
            <Pagination type="white" :currentPage="45" :totalPages="55" />
          </template>
        </SearchResults>
      </template>
    </SearchPage>
  </AppWrapper>
</template>

<script setup lang="ts">
import type { IFilter, IMgError } from "~/interfaces/types";

const route = useRoute();
const router = useRouter();
const config = useRuntimeConfig();
const pageSize = 10;

const titlePrefix =
  route.params.catalogue === "all" ? "" : route.params.catalogue + " ";
useHead({ title: titlePrefix + "Cohorts" });

const currentPage = ref(1);
if (route.query?.page) {
  const queryPageNumber = Number(route.query?.page);
  currentPage.value =
    typeof queryPageNumber === "number" ? Math.round(queryPageNumber) : 1;
}
let offset = computed(() => (currentPage.value - 1) * pageSize);

let filters: IFilter[] = reactive([
  {
    title: "Search in cohorts",
    columnType: "_SEARCH",
    search: "",
    searchTables: ["collectionEvents", "subcohorts"],
    initialCollapsed: false,
  },
  {
    title: "Areas of information",
    refTableId: "AreasOfInformationCohorts",
    columnId: "areasOfInformation",
    columnType: "ONTOLOGY",
    filterTable: "collectionEvents",
    conditions: [],
  },
  {
    title: "Data categories",
    refTableId: "DataCategories",
    columnId: "dataCategories",
    columnType: "ONTOLOGY",
    filterTable: "collectionEvents",
    conditions: [],
  },
  {
    title: "Population age groups",
    refTableId: "AgeGroups",
    columnId: "ageGroups",
    columnType: "ONTOLOGY",
    filterTable: "collectionEvents",
    conditions: [],
  },
  {
    title: "Sample categories",
    refTableId: "SampleCategories",
    columnId: "sampleCategories",
    columnType: "ONTOLOGY",
    filterTable: "collectionEvents",
    conditions: [],
  },
  {
    title: "Cohort Types",
    refTableId: "ResourceTypes",
    columnId: "type",
    columnType: "ONTOLOGY",
    conditions: [],
  },
  {
    title: "Design",
    refTableId: "CohortDesigns",
    columnId: "design",
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
  query Cohorts($filter:CohortsFilter, $orderby:Cohortsorderby){
    Cohorts(limit: ${pageSize} offset: ${offset.value} filter:$filter  orderby:$orderby) {
      id
      name
      acronym
      description
      keywords
      numberOfParticipants
      startYear
      endYear
      type {
          name
      }
      design {
          name
      }
      leadOrganisation {
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

const orderby = { acronym: "ASC" };

const filter = computed(() => {
  let result = buildQueryFilter(filters, search.value);
  if ("all" !== route.params.catalogue) {
    result["networks"] = { id: { equals: route.params.catalogue } };
  }
  return result;
});

const { data } = await useFetch<any, IMgError>(
  `/${useRoute().params.schema}/graphql`,
  {
    method: "POST",
    body: {
      query: query,
      variables: { filter, orderby },
    },
    onResponseError(_ctx) {
      logError({
        message: "onResponseError fetching data from GraphQL endpoint",
        statusCode: _ctx.response.status,
        data: _ctx.response._data,
      });
    },
  }
);

const cohorts = computed(() => data.value.data.Cohorts || []);
const numberOfCohorts = computed(() => data.value.data.Cohorts_agg.count || 0);

function setCurrentPage(pageNumber: number) {
  router.push({ path: route.path, query: { page: pageNumber } });
  currentPage.value = pageNumber;
}

watch(filters, () => {
  setCurrentPage(1);
});

let activeName = ref("detailed");

const underConstructionNotice = ref();

const cohortOnly = computed(() => {
  const routeSetting = route.query["cohort-only"] as string;
  return routeSetting === "true" || config.public.cohortOnly;
});
const crumbs: any = {};
crumbs[
  cohortOnly.value ? "home" : (route.params.catalogue as string)
] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}`;
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
            title="Cohorts"
            description="Group of individuals sharing a defining demographic characteristic."
            icon="image-link"
          >
            <template #prefix>
              <BreadCrumbs :crumbs="crumbs" current="cohorts" />
            </template>
            <template #suffix>
              <SearchResultsViewTabs
                class="hidden xl:flex"
                buttonLeftLabel="Detailed"
                buttonLeftName="detailed"
                buttonLeftIcon="view-normal"
                buttonRightLabel="Compact"
                buttonRightName="compact"
                buttonRightIcon="view-compact"
                v-model:activeName="activeName"
              />
              <SearchResultsViewTabsMobile class="flex xl:hidden">
                <FilterSidebar
                  title="Filters"
                  :filters="filters"
                  :mobileDisplay="true"
                />
              </SearchResultsViewTabsMobile>
            </template>
          </PageHeader>
        </template>

        <template #search-results>
          <SearchResultsCount label="cohort" :value="numberOfCohorts" />
          <FilterWell :filters="filters"></FilterWell>
          <SearchResultsList>
            <CardList v-if="cohorts.length > 0">
              <CardListItem v-for="cohort in cohorts" :key="cohort.name">
                <CohortCard
                  :cohort="cohort"
                  :schema="route.params.schema"
                  :catalogue="route.params.catalogue"
                  :compact="activeName !== 'detailed'"
                />
              </CardListItem>
            </CardList>
            <div v-else class="flex justify-center pt-3">
              <span class="py-15 text-blue-500">
                No Cohorts found with current filters
              </span>
            </div>
          </SearchResultsList>
        </template>

        <template v-if="cohorts.length > 0" #pagination>
          <Pagination
            :current-page="currentPage"
            :totalPages="Math.ceil(data?.data?.Cohorts_agg.count / pageSize)"
            @update="setCurrentPage($event)"
          />
        </template>
      </SearchResults>
    </template>
  </LayoutsSearchPage>
</template>

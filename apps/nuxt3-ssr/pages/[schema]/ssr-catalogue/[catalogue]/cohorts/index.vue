<script setup lang="ts">
import type { ICohort, IMgError, IFilter } from "~/interfaces/types";

const route = useRoute();
const router = useRouter();
const config = useRuntimeConfig();
const pageSize = 10;

const titlePrefix =
  route.params.catalogue === "all" ? "" : route.params.catalogue + " ";
useHead({ title: titlePrefix + "Cohorts" });

const queryPart = computed(() => {
  route.query?.conditions ? route.query.conditions : "";
});

const currentPage = ref(1);
if (route.query?.page) {
  const queryPageNumber = Number(route.query?.page);
  currentPage.value =
    typeof queryPageNumber === "number" ? Math.round(queryPageNumber) : 1;
}
let offset = computed(() => (currentPage.value - 1) * pageSize);

let filters: IFilter[] = reactive([
  {
    id: "search",
    config: {
      label: "Search in cohorts",
      type: "SEARCH",
      searchTables: ["collectionEvents", "subcohorts"],
      initialCollapsed: false,
    },
    search: "",
  },
  {
    id: "areasOfInformation",
    config: {
      label: "Areas of information",
      type: "ONTOLOGY",
      ontologyTableId: "AreasOfInformationCohorts",
      ontologySchema: "CatalogueOntologies",
      columnId: "areasOfInformation",
      filterTable: "collectionEvents",
    },
    conditions: [],
  },
  {
    id: "dataCategories",
    config: {
      label: "Data categories",
      type: "ONTOLOGY",
      ontologyTableId: "DataCategories",
      ontologySchema: "CatalogueOntologies",
      columnId: "dataCategories",
      filterTable: "collectionEvents",
    },
    conditions: [],
  },
  {
    id: "populationAgeGroups",
    config: {
      label: "Population age groups",
      type: "ONTOLOGY",
      ontologyTableId: "AgeGroups",
      ontologySchema: "CatalogueOntologies",
      columnId: "ageGroups",
      filterTable: "collectionEvents",
    },
    conditions: [],
  },
  {
    id: "sampleCategories",
    config: {
      label: "Sample categories",
      type: "ONTOLOGY",
      ontologyTableId: "SampleCategories",
      ontologySchema: "CatalogueOntologies",
      columnId: "sampleCategories",
      filterTable: "collectionEvents",
    },
    conditions: [],
  },
  {
    id: "cohortTypes",
    config: {
      label: "Cohort types",
      type: "ONTOLOGY",
      ontologyTableId: "CohortTypes",
      ontologySchema: "CatalogueOntologies",
      columnId: "type",
      filterTable: "Cohorts",
    },
    conditions: [],
  },
  {
    id: "cohortDesigns",
    config: {
      label: "Design",
      type: "ONTOLOGY",
      ontologyTableId: "CohortDesigns",
      ontologySchema: "CatalogueOntologies",
      columnId: "design",
    },
    conditions: [],
  },
]);

const applyConditions = (filters: IFilter[], conditions: any) => { 

}

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

const gqlFilter = computed(() => {
  let result = buildQueryFilter(filters);
  if ("all" !== route.params.catalogue) {
    result["networks"] = { id: { equals: route.params.catalogue } };
  }
  return result;
});

const { data, error } = await useGqlFetch<
  { data: { Cohorts: ICohort[] } },
  IMgError
>(query, {
  variables: { filter: gqlFilter, orderby },
});

if (error.value) {
  throw new Error("Error on cohorts-page data fetch");
}

const cohorts = computed(() => {
  return data.value.data.Cohorts;
});

const resultSize = computed(() => {
  return data.value.data.Cohorts_agg.count;
});

function setCurrentPage(pageNumber: number) {
  router.push({ path: route.path, query: { page: pageNumber } });
  currentPage.value = pageNumber;
}

function onFilterChange(filters: IFilter[]) {

  const conditions = toPathQuery(filters);
 
  router.push({
    path: route.path,
    query: { ...route.query, page: 1, conditions: conditions },
  });
}

watch(filters, () => {
  onFilterChange(filters);
});

let activeName = ref("detailed");

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
          <FilterWell :filters="filters"></FilterWell>
          <SearchResultsList>
            <!-- {{ cohorts }} -->
            <CardList v-if="cohorts?.length > 0">
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

        <template v-if="resultSize" #pagination>
          <Pagination
            :current-page="currentPage"
            :totalPages="Math.ceil(resultSize / pageSize)"
            @update="setCurrentPage($event)"
          />
        </template>
      </SearchResults>
    </template>
  </LayoutsSearchPage>
</template>

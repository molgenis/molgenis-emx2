<script setup lang="ts">
import type { IFilter, IMgError } from "~/interfaces/types";
import mappingsFragment from "~~/gql/fragments/mappings";

const route = useRoute();
const router = useRouter();
const config = useRuntimeConfig();
const pageSize = 30;

const titlePrefix =
  route.params.catalogue === "all" ? "" : route.params.catalogue + " ";
useHead({ title: titlePrefix + "Variables" });

type view = "list" | "harmonization";

const scoped = route.params.catalogue !== "all";
const catalogueRouteParam = route.params.catalogue as string;

const currentPage = ref(1);
const activeName = ref((route.query.view as view | undefined) || "list");

watch([currentPage, activeName], () => {
  router.push({
    path: route.path,
    query: { page: currentPage.value, view: activeName.value },
  });
});

function setCurrentPage(pageNumber: number) {
  currentPage.value = pageNumber;
}

if (route.query?.page) {
  const queryPageNumber = Number(route.query?.page);
  currentPage.value =
    typeof queryPageNumber === "number" ? Math.round(queryPageNumber) : 1;
}

let pageIcon = computed(() => {
  switch (activeName.value) {
    case "list":
      return "image-diagram-2";
    case "harmonization":
      return "image-table";
  }
});

let offset = computed(() => (currentPage.value - 1) * pageSize);

let filters: IFilter[] = reactive([
  {
    title: "Search in variables",
    columnType: "_SEARCH",
    search: "",
    initialCollapsed: false,
  },
  {
    title: "Topics",
    refTableId: "Keywords",
    columnId: "keywords",
    columnType: "ONTOLOGY",
    conditions: [],
    initialCollapsed: false,
  },
]);

const query = computed(() => {
  return `
  query VariablesPage(
    $variablesFilter:VariablesFilter,
    $cohortsFilter:CohortsFilter,
    $orderby:Variablesorderby
  ){
    Variables(limit: ${pageSize} offset: ${
    offset.value
  } filter:$variablesFilter  orderby:$orderby) {
      name
      resource {
        id
      }
      dataset {
        name
        resource {
          id
        }
      }
      label
      description
      mappings ${moduleToString(mappingsFragment)}
      repeats {
        name
        mappings ${moduleToString(mappingsFragment)}
      }
    }
    Cohorts(filter: $cohortsFilter, orderby: { id: ASC }) {
      id
      networks {
        id
      }
    }
    Variables_agg (filter:$variablesFilter){
      count
    }
  }
  `;
});

const numberOfVariables = computed(() => data?.value.data?.Variables_agg.count || 0);
const numberOfCohorts = computed(() => {
  if (data?.value.data?.Cohorts) {
    return data?.value.data?.Cohorts.length;
  }
  return false;
})

let search = computed(() => {
  // @ts-ignore
  return filters.find((f) => f.columnType === "_SEARCH").search;
});

let graphqlURL = computed(() => `/${route.params.schema}/graphql`);

const orderby = { label: "ASC" };
const typeFilter = { resource: { mg_tableclass: { like: ["Models"] } } };

const filter = computed(() => {
  return {
    ...buildQueryFilter(filters, search.value),
    ...typeFilter,
  };
});

async function loadPageData() {
  const { data, error } = await useAsyncData<any, IMgError>(
    `variables-page-${catalogueRouteParam}`,
    async () => {
      let resourceCondition = {};
      if (scoped) {
        const { data, error } = await $fetch(
          `/${route.params.schema}/graphql`,
          {
            method: "POST",
            body: {
              query: `
            query Networks($filter:NetworksFilter) {
              Networks(filter:$filter){
                 models {
                  id
                 }
              }
            }`,
              variables: { filter: { id: { equals: catalogueRouteParam } } },
            },
          }
        );

        if (error) {
          console.log("models error: ", error);
          return { error };
        }

        const modelIds = data.Networks[0].models.map(
          (m: { id: string }) => m.id
        );

        resourceCondition = {
          resource: {
            id: {
              equals: modelIds,
            },
          },
        };
      }

      const variables = {
        orderby,
        variablesFilter: scoped
          ? { ...filter.value, ...resourceCondition }
          : filter.value,
        cohortsFilter: { networks: { equals: [{ id: catalogueRouteParam }] } },
      };

      return $fetch(graphqlURL.value, {
        key: `variables-${offset.value}`,
        method: "POST",
        body: {
          query: query.value,
          variables,
        },
      });
    }
  );

  if (error.value) {
    const contextMsg = "Error on fetching variable data";
    logError(error.value, contextMsg);
    throw new Error(contextMsg);
  }

  return data;
}

watch(filters, () => {
  setCurrentPage(1);
  loadPageData();
});

let crumbs: any = {};
crumbs[
  `${route.params.catalogue}`
] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}`;

const data = await loadPageData();
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
            title="Variables"
            description="A complete overview of available variables."
            :icon="pageIcon"
          >
            <template #prefix>
              <BreadCrumbs :crumbs="crumbs" current="variables" />
            </template>
            <template #suffix>
              <SearchResultsViewTabs
                class="hidden xl:flex"
                buttonLeftLabel="List of variables"
                buttonLeftName="list"
                buttonLeftIcon="view-compact"
                buttonRightLabel="Harmonizations"
                buttonRightName="harmonization"
                buttonRightIcon="view-table"
                v-model:activeName="activeName"
              />
              <SearchResultsViewTabsMobile
                class="flex xl:hidden"
                v-model:activeName="activeName"
              >
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
          <div class="flex align-start gap-1">
            <SearchResultsCount :value="numberOfVariables" label="variable" />
            <SearchResultsCount
              v-if="numberOfCohorts"
              :value="numberOfCohorts"
              value-prefix="in"
              label="cohort"
            />
          </div>
          <FilterWell :filters="filters"></FilterWell>

          <SearchResultsList>
            <div
              v-if="data?.data?.Variables_agg.count === 0"
              class="flex justify-center pt-3"
            >
              <span class="py-15 text-blue-500">
                No variables found with current filters
              </span>
            </div>
            <CardList v-else-if="activeName === 'list'">
              <CardListItem
                v-for="variable in data?.data?.Variables"
                :key="variable.name"
              >
                <VariableCard
                  :variable="variable"
                  :schema="route.params.schema"
                  :catalogue="route.params.catalogue"
                />
              </CardListItem>
            </CardList>
            <HarmonizationTable
              v-else
              :variables="data?.data?.Variables"
              :cohorts="data?.data?.Cohorts"
            >
            </HarmonizationTable>
          </SearchResultsList>
        </template>

        <template #pagination v-if="data?.data?.Variables?.length > 0">
          <Pagination
            :current-page="currentPage"
            :totalPages="Math.ceil(data?.data?.Variables_agg.count / pageSize)"
            @update="setCurrentPage($event)"
          />
        </template>
      </SearchResults>
    </template>
  </LayoutsSearchPage>
</template>

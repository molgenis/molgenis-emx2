<script setup lang="ts">
import type { IFilter, IMgError, IFilterCondition } from "~/interfaces/types";
import mappingsFragment from "~~/gql/fragments/mappings";
import type { INode } from "../../../../../../tailwind-components/types/types";

const route = useRoute();
const router = useRouter();
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
    query: { ...route.query, page: currentPage.value, view: activeName.value },
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

const pageFilterTemplate: IFilter[] = [
  {
    id: "search",
    config: {
      label: "Search in variables",
      type: "SEARCH",
      initialCollapsed: false,
    },
    search: "",
  },
  {
    id: "topics",
    config: {
      label: "Topics",
      type: "ONTOLOGY",
      ontologyTableId: "Keywords",
      ontologySchema: "CatalogueOntologies",
      columnId: "keywords",
      initialCollapsed: true,
    },
    conditions: [],
  },
  {
    id: "cohorts",
    config: {
      label: "Cohorts",
      type: "REF_ARRAY",
      refTableId: "Cohorts",
      columnId: "mappings",
      buildFilterFunction: (conditions: IFilterCondition[]) => {
        // convert generic conditions to specific filter form
        return { source: { equals: conditions.map((c) => ({ id: c.name })) } };
      },
      refFields: {
        name: "id",
        description: "name",
      },
    },
    options: fetchCohortOptions,
    conditions: [],
  },
];

async function fetchCohortOptions(): Promise<INode[]> {
  const variables = scoped
    ? { variablesFilter: await buildScopedModelFilter() }
    : { resource: { mg_tableclass: { like: ["Models"] } } };
  const { data, error } = await $fetch(`/${route.params.schema}/graphql`, {
    method: "POST",
    body: {
      query: `
            query CohortsWithVariableMapping($variablesFilter: VariablesFilter) {
              Variables_groupBy(filter: $variablesFilter) {
                count
                mappings {
                  source {
                    id
                    name
                  }
                } 
              }
            }
          `,
      variables,
    },
  });

  return data.Variables_groupBy.filter(
    (respRow: any) => respRow.mappings // filter out rows without mappings, i.e. the count all row
  ).map(
    (variableGroupBy: {
      count: number;
      mappings: { source: { id: string; name: string } };
    }) => {
      return {
        name: variableGroupBy.mappings.source.id,
        description: variableGroupBy.mappings.source.name,
      } as INode;
    }
  );
}

const filters = computed(() => {
  // if there are not query conditions just use the page defaults
  if (!route.query?.conditions) {
    return [...pageFilterTemplate];
  }

  // get conditions from query
  const conditions = conditionsFromPathQuery(route.query.conditions as string);
  // merge with page defaults
  const filters = mergeWithPageDefaults(pageFilterTemplate, conditions);

  return filters;
});

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

const numberOfVariables = computed(
  () => data?.value.data?.Variables_agg.count || 0
);

const numberOfCohorts = computed(() => {
  return data?.value.data?.Cohorts ? data?.value.data?.Cohorts.length : 0;
});

const graphqlURL = computed(() => `/${route.params.schema}/graphql`);

const orderby = { label: "ASC" };

const filter = computed(() => {
  return buildQueryFilter(filters.value);
});

const cachedScopedResouceFilter = ref();

async function buildScopedModelFilter() {
  if (cachedScopedResouceFilter.value) {
    return cachedScopedResouceFilter.value;
  }
  const { data, error } = await $fetch(`/${route.params.schema}/graphql`, {
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
  });

  if (error) {
    console.log("models error: ", error);
    return { error };
  }

  const modelIds = data.Networks[0].models.map((m: { id: string }) => m.id);

  const scopedResourceFilter = {
    resource: {
      mg_tableclass: { like: ["Models"] },
      id: {
        equals: modelIds,
      },
    },
  };

  cachedScopedResouceFilter.value = scopedResourceFilter;

  return scopedResourceFilter;
}

const fetchData = async () => {
  let cohortsFilter: any = {};
  if (scoped) {
    cohortsFilter.networks = { equals: [{ id: catalogueRouteParam }] };
  }

  // if the cohort filter is active, also filter the columns (the cohorts)
  // we know this fiter is not perfect but its the best we can do given the current datamodel
  if (filter.value.mappings) {
    cohortsFilter = { ...cohortsFilter, ...filter.value.mappings.source };
  }

  const variables = scoped
    ? {
        orderby,
        variablesFilter: {
          ...filter.value,
          ...(await buildScopedModelFilter()),
        },
        cohortsFilter,
      }
    : {
        orderby,
        variablesFilter: {
          ...filter.value,
          resource: { mg_tableclass: { like: ["Models"] } },
        },
        cohortsFilter,
      };

  return $fetch(graphqlURL.value, {
    key: `variables-${offset.value}`,
    method: "POST",
    body: {
      query: query.value,
      variables,
    },
  });
};

// We need to use the useAsyncData hook to fetch the data because sadly multiple backendend calls need to be synchronized to create the final query
// todo: update datamodel to allow for single fetch from single indexed table
const { data, error } = await useAsyncData<any, IMgError>(
  `variables-page-${catalogueRouteParam}-${route.query}`,
  fetchData,
  { watch: [filters, offset] }
);

function onFilterChange(filters: IFilter[]) {
  const conditions = toPathQueryConditions(filters) || undefined; // undefined is used to remove the query param from the URL;

  router.push({
    path: route.path,
    query: { ...route.query, page: 1, conditions: conditions },
  });
}

let crumbs: any = {};
crumbs[
  `${route.params.catalogue}`
] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}`;
</script>

<template>
  <LayoutsSearchPage>
    <template #side>
      <FilterSidebar
        title="Filters"
        :filters="filters"
        @update:filters="onFilterChange"
      />
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
                  @update:filters="onFilterChange"
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
              v-if="numberOfCohorts > 0"
              :value="numberOfCohorts"
              value-prefix="in"
              label="cohort"
            />
          </div>
          <FilterWell
            :filters="filters"
            @update:filters="onFilterChange"
          ></FilterWell>

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
                  :schema="route.params.schema as string"
                  :catalogue="route.params.catalogue as string"
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

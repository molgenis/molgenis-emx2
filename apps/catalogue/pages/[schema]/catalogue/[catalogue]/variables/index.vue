<script setup lang="ts">
import type {
  IFilter,
  IMgError,
  IFilterCondition,
  IRefArrayFilter,
} from "../../../../../interfaces/types";
import mappingsFragment from "../../../../../gql/fragments/mappings";
import type { INode } from "../../../../../../tailwind-components/types/types";
import { useRoute, useRouter, useHead, navigateTo, useAsyncData } from "#app";
import {
  conditionsFromPathQuery,
  mergeWithPageDefaults,
  moduleToString,
  buildQueryFilter,
  toPathQueryConditions,
} from "#imports";
import { computed } from "vue";

const route = useRoute();
const router = useRouter();
const pageSize = 30;

const titlePrefix =
  route.params.catalogue === "all" ? "" : route.params.catalogue + " ";
useHead({
  title: titlePrefix + "Variables",
  meta: [
    {
      name: "description",
      content: `A complete overview of ${titlePrefix.trim()} harmonised variables`,
    },
  ],
});

type view = "list" | "harmonisation";

const scoped = route.params.catalogue !== "all";
const catalogueRouteParam = route.params.catalogue as string;

const activeName = computed(() => {
  return (route.query.view as view | undefined) || "list";
});
const currentPage = computed(() => {
  const queryPageNumber = Number(route?.query?.page);
  return Number.isNaN(queryPageNumber) ? 1 : Math.round(queryPageNumber);
});

function onViewChange(view: view) {
  router.push({
    path: route.path,
    query: { ...route.query, view },
  });
}

async function setCurrentPage(pageNumber: number) {
  await navigateTo({ query: { ...route.query, page: pageNumber } });
  window.scrollTo({ top: 0 });
}

const pageIcon = computed(() => {
  switch (activeName.value) {
    case "list":
      return "image-diagram-2";
    case "harmonisation":
      return "image-table";
  }
});

const offset = computed(() => (currentPage.value - 1) * pageSize);

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
    id: "resources",
    config: {
      label: "Sources",
      type: "REF_ARRAY",
      refTableId: "Resources",
      initialCollapsed: false,
      buildFilterFunction: (
        filterBuilder: Record<string, Record<string, any>>,
        conditions: IFilterCondition[]
      ) => {
        return {
          ...filterBuilder,
          ...{
            mappings: {
              source: { equals: conditions.map((c) => ({ id: c.name })) },
              match: { name: { equals: ["complete", "partial"] } },
            },
          },
        };
      },
      refFields: {
        name: "id",
        description: "name",
      },
    },
    options: fetchResourceOptions,
    conditions: [],
  },
];

async function fetchResourceOptions(): Promise<INode[]> {
  const { data, error } = await $fetch(`/${route.params.schema}/graphql`, {
    method: "POST",
    body: {
      query: `
            query Resources($resourcesFilter: ResourcesFilter) {
              Resources(filter: $resourcesFilter, orderby: { id: ASC }) {
                id
                name
              }
            }
          `,
      variables: scoped
        ? {
            resourcesFilter: {
              _or: [
                {
                  partOfResources: { equals: [{ id: catalogueRouteParam }] },
                },
                {
                  partOfResources: {
                    type: { name: { equals: "Network" } },
                    partOfResources: {
                      equals: [{ id: catalogueRouteParam }],
                    },
                  },
                },
              ],
            },
          }
        : { resource: { type: { name: { equals: "Network" } } } },
    },
  });

  return data.Resources.map((option: { id: string; name?: string }) => {
    return {
      name: option.id,
      description: option.name,
    } as INode;
  });
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
    $resourcesFilter:ResourcesFilter,
  ){
    Variables(limit: ${pageSize} offset: ${
    offset.value
  } filter:$variablesFilter  orderby: { name: ASC }) {
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
      repeatUnit {name}
      repeatMin
      repeatMax
      label
      description
      mappings(filter:{match:{name:{equals:["complete","partial"]}}}) ${moduleToString(
        mappingsFragment
      )}
    }
    Resources(filter: $resourcesFilter, orderby: { id: ASC }) {
      id
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

const graphqlURL = computed(() => `/${route.params.schema}/graphql`);

const filter = computed(() => {
  return buildQueryFilter(filters.value);
});

const fetchData = async () => {
  let resourcesFilter: any = {};
  if (scoped) {
    resourcesFilter.partOfResources = {
      _or: [
        { equals: [{ id: catalogueRouteParam }] },
        { partOfResources: { equals: [{ id: catalogueRouteParam }] } },
      ],
    };
  }

  // add 'special' filter for harmonisation x-axis if 'resources' filter is set
  const resourceConditions = (
    filters.value.find((f) => f.id === "resources") as IRefArrayFilter
  )?.conditions;
  if (resourceConditions.length) {
    resourcesFilter = {
      ...resourcesFilter,
      equals: resourceConditions.map((c) => ({ id: c.name })),
    };
  }
  const variableResourceFilter = resourceConditions.length
    ? {
        mappings: {
          source: { id: { equals: resourceConditions.map((c) => c.name) } },
          match: { name: { equals: ["complete", "partial"] } },
        },
      }
    : undefined;
  const variables = scoped
    ? {
        variablesFilter: {
          ...filter.value,
          ...variableResourceFilter,
          ...{
            _or: [
              { resource: { id: { equals: catalogueRouteParam } } },
              {
                resource: {
                  type: { name: { equals: "Network" } },
                  partOfResources: { id: { equals: catalogueRouteParam } },
                },
              },
              {
                reusedInResources: {
                  _or: [
                    { resource: { id: { equals: catalogueRouteParam } } },
                    {
                      resource: {
                        partOfResources: {
                          id: { equals: catalogueRouteParam },
                        },
                      },
                    },
                  ],
                },
              },
            ],
          },
        },
        resourcesFilter,
      }
    : {
        variablesFilter: {
          ...filter.value,
          ...variableResourceFilter,
          ...{ resource: { type: { name: { equals: "Network" } } } },
        },
        resourcesFilter,
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
const { data, error, pending } = await useAsyncData<any, IMgError>(
  `variables-page-${catalogueRouteParam}-${route.query}`,
  fetchData,
  { watch: [computed(() => route.query.conditions), offset] }
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
] = `/${route.params.schema}/catalogue/${route.params.catalogue}`;
crumbs["variables"] = "";
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
            description="A complete overview of harmonised variables"
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
                buttonRightLabel="Harmonisations"
                buttonRightName="harmonisation"
                buttonRightIcon="view-table"
                :activeName="activeName"
                @update:activeName="onViewChange"
              />
              <SearchResultsViewTabsMobile
                class="flex xl:hidden"
                button-top-label="Harmonisation"
                button-top-name="list"
                button-top-icon="view-table"
                button-bottom-label="Variables"
                button-bottom-name="harmonisation"
                button-bottom-icon="view-compact"
                :activeName="activeName"
                @update:activeName="onViewChange"
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
            <SearchResultsCount :value="numberOfVariables" label="variables" />
            <div
              v-if="pending"
              class="mt-2 mb-0 lg:mb-3 text-body-lg flex flex-col text-title"
            >
              <BaseIcon name="progress-activity" class="animate-spin" />
            </div>
          </div>
          <FilterWell
            class="transition-opacity duration-700 ease-in opacity-100"
            :class="{ 'opacity-25 ease-out': pending }"
            :filters="filters"
            @update:filters="onFilterChange"
          ></FilterWell>

          <SearchResultsList
            class="transition-opacity duration-700 ease-in opacity-100"
            :class="{ 'opacity-25 ease-out': pending }"
          >
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
            <HarmonisationTable
              v-else
              :variables="data?.data?.Variables"
              :resources="data?.data?.Resources"
            >
            </HarmonisationTable>
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

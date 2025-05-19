<script setup lang="ts">
import {
  useRoute,
  useRouter,
  useRuntimeConfig,
  useHead,
  useFetch,
  navigateTo,
} from "#app";
import {
  conditionsFromPathQuery,
  mergeWithPageDefaults,
  buildQueryFilter,
  logError,
  toPathQueryConditions,
} from "#imports";
import { computed, ref } from "vue";
import type { IFilter, IMgError, activeTabType } from "~/interfaces/types";

const route = useRoute();
const router = useRouter();
const config = useRuntimeConfig();
const pageSize = 10;

const titlePrefix =
  route.params.catalogue === "all" ? "" : route.params.catalogue + " ";

const descriptionMap: Record<string, string> = {
  collections: "Data & sample collections",
  networks: "(Sub)projects & harmonisation",
};

const imageMap: Record<string, string> = {
  collections: "image-diagram",
  networks: "image-network",
};

const title = route.params.resourceType as string;
const description: string | undefined =
  descriptionMap[route.params.resourceType as string];
const image: string | undefined = imageMap[route.params.resourceType as string];

useHead({
  title: titlePrefix + title,
  meta: [{ name: "description", content: description }],
});

const currentPage = computed(() => {
  const queryPageNumber = Number(route.query?.page);
  return !isNaN(queryPageNumber) && typeof queryPageNumber === "number"
    ? Math.round(queryPageNumber)
    : 1;
});
const offset = computed(() => (currentPage.value - 1) * pageSize);

let pageFilterTemplate: IFilter[] = [
  {
    id: "search",
    config: {
      label: `Search in ${title}`,
      type: "SEARCH",
      searchTables: ["collectionEvents", "subpopulations"],
      initialCollapsed: false,
    },
    search: "",
  },
];

if (route.params.resourceType === "collections") {
  pageFilterTemplate.push({
    id: "type",
    config: {
      label: "Collection type",
      type: "ONTOLOGY",
      ontologyTableId: "ResourceTypes",
      ontologySchema: "CatalogueOntologies",
      // @ts-ignore
      filter: { tags: { equals: "collection" } },
      columnId: "type",
      initialCollapsed: false,
    },
    conditions: [],
  });
}

pageFilterTemplate = pageFilterTemplate.concat([
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
      ontologyTableId: "CohortStudyTypes",
      ontologySchema: "CatalogueOntologies",
      columnId: "cohortType",
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
  {
    id: "disease",
    config: {
      label: "Diseases",
      type: "ONTOLOGY",
      ontologyTableId: "Diseases",
      ontologySchema: "CatalogueOntologies",
      columnId: "mainMedicalCondition",
      filterTable: "subpopulations",
    },
    conditions: [],
  },
]);

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
  query Resources($filter:ResourcesFilter, $orderby:Resourcesorderby){
    Resources(limit: ${pageSize} offset: ${offset.value} filter:$filter  orderby:$orderby) {
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
      datasets {
        name
        label
      }
    }
    Resources_agg (filter:$filter){
        count
    }
  }
  `;
});

const orderby = { acronym: "ASC" };

const gqlFilter = computed(() => {
  let result: any = {};

  result = buildQueryFilter(filters.value);

  if (!result.type) {
    if (route.params.resourceType == "collections") {
      result.type = { tags: { equals: "collection" } };
    }
    if (route.params.resourceType == "networks") {
      result.type = { tags: { equals: "network" } };
    }
  }

  // add hard coded page specific filters
  if ("all" !== route.params.catalogue) {
    result = {
      _and: [
        result,
        {
          _or: [
            { partOfResources: { id: { equals: route.params.catalogue } } },
            {
              partOfResources: {
                partOfResources: { id: { equals: route.params.catalogue } },
              },
            },
          ],
        },
      ],
    };
  }
  return result;
});

const { data } = await useFetch<any, IMgError>(
  `/${useRoute().params.schema}/graphql`,
  {
    method: "POST",
    body: {
      query: query,
      variables: { filter: gqlFilter, orderby },
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

const resources = computed(() => data.value?.data.Resources || []);
const numberOfResources = computed(
  () => data.value?.data.Resources_agg.count || 0
);

async function setCurrentPage(pageNumber: number) {
  await navigateTo({
    query: { ...route.query, page: pageNumber },
  });
  window.scrollTo({ top: 0 });
}

function onFilterChange(filters: IFilter[]) {
  const conditions = toPathQueryConditions(filters) || undefined; // undefined is used to remove the query param from the URL;

  router.push({
    path: route.path,
    query: { ...route.query, page: 1, conditions: conditions },
  });
}

const activeTabName = ref((route.query.view as string) || "detailed");

function onActiveTabChange(tabName: activeTabType) {
  activeTabName.value = tabName;
  router.push({
    path: route.path,
    query: { ...route.query, view: tabName },
  });
}

const cohortOnly = computed(() => {
  const routeSetting = route.query["cohort-only"] as string;
  return routeSetting === "true" || config.public.cohortOnly;
});

const crumbs: any = {};
crumbs[
  cohortOnly.value ? "home" : (route.params.catalogue as string)
] = `/${route.params.schema}/catalogue/${route.params.catalogue}`;
crumbs[route.params.resourceType as string] = "";
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
          <PageHeader :title="title" :description="description" :icon="image">
            <template #prefix>
              <BreadCrumbs :crumbs="crumbs" />
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
                :activeName="activeTabName"
                @update:activeName="onActiveTabChange"
              />
              <SearchResultsViewTabsMobile
                class="flex xl:hidden"
                button-top-label="View"
                button-top-name="detailed"
                button-top-icon="view-normal"
                button-bottom-label="View"
                button-bottom-name="compact"
                button-bottom-icon="view-compact"
                :activeName="activeTabName"
                @update:active-name="onActiveTabChange"
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
          <SearchResultsCount
            :label="title?.toLocaleLowerCase()"
            :value="numberOfResources"
          />
          <FilterWell
            :filters="filters"
            @update:filters="onFilterChange"
          ></FilterWell>
          <SearchResultsList>
            <CardList v-if="resources.length > 0">
              <CardListItem v-for="resource in resources" :key="resource.name">
                <ResourceCard
                  :resource="resource"
                  :schema="route.params.schema as string"
                  :catalogue="route.params.catalogue as string"
                  :compact="activeTabName !== 'detailed'"
                />
              </CardListItem>
            </CardList>
            <div v-else class="flex justify-center pt-3">
              <span class="py-15 text-blue-500">
                No resources found with current filters
              </span>
            </div>
          </SearchResultsList>
        </template>

        <template v-if="resources.length > 0" #pagination>
          <Pagination
            :current-page="currentPage"
            :totalPages="Math.ceil(numberOfResources / pageSize)"
            @update="setCurrentPage($event)"
          />
        </template>
      </SearchResults>
    </template>
  </LayoutsSearchPage>
</template>

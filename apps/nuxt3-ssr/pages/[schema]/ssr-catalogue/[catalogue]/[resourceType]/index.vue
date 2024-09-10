<script setup lang="ts">
import type { IFilter, IMgError, activeTabType } from "~/interfaces/types";

const route = useRoute();
const router = useRouter();
const config = useRuntimeConfig();
const pageSize = 10;

const titlePrefix =
  route.params.catalogue === "all" ? "" : route.params.catalogue + " ";
const resourceType = computed(() =>
  getResourceMetadataForPath(route.params.resourceType as string)
);

useHead({ title: titlePrefix + resourceType.value.plural });

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
      label: "Search in resources",
      type: "SEARCH",
      searchTables: ["collectionEvents", "subpopulations"],
      initialCollapsed: false,
    },
    search: "",
  },
];

if (resourceType.value.path === "resources") {
  pageFilterTemplate.push({
    id: "type",
    config: {
      label: "Type",
      type: "ONTOLOGY",
      ontologyTableId: "CollectionTypesFLAT",
      ontologySchema: "CatalogueOntologies",
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
      columnId: "designType",
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
      startDataCollection
      endDataCollection
      type {
          name
      }
      designType {
          name
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

  if (resourceType.value.path != "resources") {
    result.type = { name: { equals: resourceType.value.type } };
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

const resources = computed(() => data.value.data.Resources || []);
const numberOfResources = computed(
  () => data.value.data.Resources_agg.count || 0
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
            :title="resourceType.plural"
            :description="resourceType.description"
            :icon="resourceType.image"
          >
            <template #prefix>
              <BreadCrumbs :crumbs="crumbs" :current="resourceType.plural" />
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
            :label="resourceType.plural?.toLocaleLowerCase()"
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

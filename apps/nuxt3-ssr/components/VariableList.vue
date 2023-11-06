<script setup lang="ts">
const route = useRoute();
const router = useRouter();
const config = useRuntimeConfig();
const pageSize = 30;

useHead({ title: "Variables" });

const currentPage = ref(1);
if (route.query?.page) {
  const queryPageNumber = Number(route.query?.page);
  currentPage.value =
    typeof queryPageNumber === "number" ? Math.round(queryPageNumber) : 1;
}
let offset = computed(() => (currentPage.value - 1) * pageSize);

let filters: IFilter[] = reactive([
  {
    title: "Search in variables",
    columnType: "_SEARCH",
    search: "",
    initialCollapsed: false,
  },
  {
    title: "Resource",
    refTable: "Resources",
    columnName: "resource",
    columnType: "REF_ARRAY",
    conditions: [],
  },
  {
    title: "Dataset",
    refTable: "Datasets",
    columnName: "dataset",
    columnType: "REF_ARRAY",
    conditions: [],
  },
  {
    title: "Formats",
    refTable: "Formats",
    columnName: "format",
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
  query Variables($filter:VariablesFilter, $orderby:Variablesorderby){
    Variables(limit: ${pageSize} offset: ${offset.value} filter:$filter  orderby:$orderby) {
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
      mappings {
        sourceDataset {
          resource {
            id
          }
          name
        }
        targetVariable {
          dataset {
            resource {
              id
            }
            name
          }
          name
        }
        match {
          name
        }
      }
      repeats {
        name
        mappings {
          match {
            name
          }
          source {
            id
          }
          sourceVariables {
            name
          }
          sourceDataset {
            resource {
              id
            }
            name
          }
        }
      }
    }
    Cohorts(orderby: { id: ASC }) {
      id
      networks {
        id
      }
    }
    Variables_agg (filter:$filter){
      count
    }
  }
  `;
});

const orderby = { label: "ASC" };
const typeFilter = { resource: { mg_tableclass: { like: ["Models"] } } };

const filter = computed(() => {
  let result = {
    ...buildQueryFilter(filters, search.value),
    ...typeFilter,
  }
  if (route.params.catalogue) {
    result._and["networks"] = { resource: { equals: route.params.catalogue } };
  }
  return result;
});

let graphqlURL = computed(() => `/${route.params.schema}/catalogue/graphql`);
const { data, pending, error, refresh } = await useFetch(graphqlURL.value, {
  key: `variables-${offset.value}`,
  baseURL: config.public.apiBase,
  method: "POST",
  body: {
    query,
    variables: { orderby, filter },
  },
});

function setCurrentPage(pageNumber: number) {
  router.push({ path: route.path, query: { page: pageNumber } });
  currentPage.value = pageNumber;
}

watch(filters, () => {
  setCurrentPage(1);
});

<<<<<<< HEAD:apps/nuxt3-ssr/components/VariableList.vue
let activeName = ref("detailed");

let crumbs: any = {};
if (route.params.catalogue) {
  crumbs[
    `${route.params.catalogue}`
  ] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}`;
} else {
  crumbs = {
    Home: `/${route.params.schema}/ssr-catalogue`,
  };
}
=======
let activeName = ref("list");
let pageIcon = computed(() => {
  switch (activeName.value) {
    case "list":
      return "image-diagram-2";
    case "harmonization":
      return "image-table";
  }
});
>>>>>>> aa7f6a1088dfbd33b71efc4c403b3e1479d918f9:apps/nuxt3-ssr/pages/[schema]/ssr-catalogue/variables/index.vue
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
                <FilterSidebar title="Filters" :filters="filters" />
              </SearchResultsViewTabsMobile>
            </template>
          </PageHeader>
        </template>

        <template #search-results>
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

        <template
          #pagination
          v-if="activeName === 'list' && data?.data?.Variables?.length > 0"
        >
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

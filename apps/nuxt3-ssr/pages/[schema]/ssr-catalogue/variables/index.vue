<script setup lang="ts">
import type { IFilter } from "../../../../interfaces/types";

const route = useRoute();
const router = useRouter();
const config = useRuntimeConfig();
const pageSize = 30;

useHead({ title: "Variables" });

type view = "list" | "harmonization";

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
  },
  {
    title: "Networks",
    columnId: "networks",
    columnType: "REF_ARRAY",
    refTableId: "Networks",
    refFields: {
      key: "id",
      name: "id",
      description: "name",
    },
    conditions: [],
  },
  {
    title: "Cohorts",
    refTableId: "Cohorts",
    columnNId: "cohorts",
    columnType: "REF_ARRAY",
    refFields: {
      key: "id",
      name: "id",
      description: "name",
    },
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
  return {
    ...buildQueryFilter(filters, search.value),
    ...typeFilter,
  };
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

watch(filters, () => {
  setCurrentPage(1);
});
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

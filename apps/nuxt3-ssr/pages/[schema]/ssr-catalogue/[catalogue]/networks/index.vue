<script setup lang="ts">
import type { IFilter, IMgError } from "~/interfaces/types";

const route = useRoute();
const router = useRouter();
const config = useRuntimeConfig();
const pageSize = 10;

const scoped = route.params.catalogue !== "all";
const catalogue = scoped ? route.params.catalogue : undefined;

useHead({ title: scoped ? `${catalogue} networks` : "Networks" });

const currentPage = ref(1);
if (route.query?.page) {
  const queryPageNumber = Number(route.query?.page);
  currentPage.value =
    typeof queryPageNumber === "number" ? Math.round(queryPageNumber) : 1;
}
let offset = computed(() => (currentPage.value - 1) * pageSize);

let filters: IFilter[] = reactive([
  {
    title: "Search in networks",
    columnType: "_SEARCH",
    search: "",
    initialCollapsed: false,
  },
  {
    title: "Type",
    columnId: "type",
    columnType: "ONTOLOGY",
    refTableId: "NetworkTypes",
    refFields: {
      key: "name",
      name: "name",
      description: "name",
    },
    conditions: [],
    initialCollapsed: false,
  },
]);

let search = computed(() => {
  // @ts-ignore
  return filters.find((f) => f.columnType === "_SEARCH").search;
});

const cardFields = `id
    name
    acronym
    description
    logo {
      id
      size
      extension
      url
    }`;

const query = computed(() => {
  if (scoped) {
    return `
query items($catalogueFilter: NetworksFilter, $filter: NetworksFilter $orderby: Networksorderby) {
  Networks(filter: $catalogueFilter) {
    id
    networks(limit: ${pageSize} offset: ${offset.value} orderby:$orderby filter:$filter) {
      ${cardFields}
    }
    networks_agg(filter:$filter) {
      count
    }
  }
}
    `;
  } else {
    return `
query items($filter:NetworksFilter, $orderby:Networksorderby){
  Networks(limit: ${pageSize} offset: ${offset.value} filter:$filter  orderby:$orderby) {
    ${cardFields}
  }
  Networks_agg (filter:$filter){
    count
  }
}
    `;
  }
});

const orderby = { acronym: "ASC" };

const filter = computed(() => {
  return buildQueryFilter(filters, search.value);
});

const catalogueFilter = scoped ? { id: { equals: catalogue } } : undefined;

const { data, error } = await useGqlFetch<any, IMgError>(query, {
  variables: {
    orderby,
    filter,
    catalogueFilter,
  },
});

if (error.value) {
  throw new Error("Error on networks page data-fetch");
}

function setCurrentPage(pageNumber: number) {
  router.push({ path: route.path, query: { page: pageNumber } });
  currentPage.value = pageNumber;
}

watch(filters, () => {
  setCurrentPage(1);
});

let activeName = ref("detailed");

const numberOfNetworks = computed(() => {
  return scoped
    ? data?.value?.data?.Networks[0]?.networks_agg.count
    : data?.value?.data?.Networks_agg?.count;
});

const networks = computed(() => {
  return scoped
    ? data.value?.data?.Networks[0]?.networks
    : data.value?.data?.Networks;
});

const crumbs: any = {};
crumbs[
  route.params.catalogue
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
            title="Networks"
            description="Collaborations of multiple institutions and/or cohorts with a common objective."
            icon="image-diagram"
          >
            <template #prefix>
              <BreadCrumbs :crumbs="crumbs" current="networks" />
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
          <FilterWell :filters="filters"></FilterWell>
          <SearchResultsList>
            <CardList v-if="networks?.length > 0">
              <CardListItem v-for="network in networks" :key="network.name">
                <NetworkCard
                  :network="network"
                  :schema="route.params.schema"
                  :compact="activeName !== 'detailed'"
                />
              </CardListItem>
            </CardList>
            <div v-else class="flex justify-center pt-3">
              <span class="py-15 text-blue-500">
                No Networks found with current filters
              </span>
            </div>
          </SearchResultsList>
        </template>

        <template v-if="networks?.length > 0" #pagination>
          <Pagination
            :current-page="currentPage"
            :totalPages="Math.ceil(numberOfNetworks / pageSize)"
            @update="setCurrentPage($event)"
          />
        </template>
      </SearchResults>
    </template>
  </LayoutsSearchPage>
</template>

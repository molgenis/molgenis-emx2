<script setup lang="ts">
import type { IFilter, IMgError } from "~/interfaces/types";

const route = useRoute();
const router = useRouter();
const config = useRuntimeConfig();
const pageSize = 10;

const titlePrefix =
  route.params.catalogue === "all" ? "" : route.params.catalogue + " ";
useHead({ title: titlePrefix + "Data sources" });

const currentPage = ref(1);
if (route.query?.page) {
  const queryPageNumber = Number(route.query?.page);
  currentPage.value =
    typeof queryPageNumber === "number" ? Math.round(queryPageNumber) : 1;
}
let offset = computed(() => (currentPage.value - 1) * pageSize);

let filters: IFilter[] = reactive([
  {
    title: "Search in datasources",
    columnType: "_SEARCH",
    search: "",
    initialCollapsed: false,
  },
  {
    title: "Areas of information",
    refTableId: "AreasOfInformationDs",
    columnId: "areasOfInformation",
    columnType: "ONTOLOGY",
    conditions: [],
  },
  {
    title: "Datasource Types",
    refTableId: "DatasourceTypes",
    columnId: "type",
    columnType: "ONTOLOGY",
    conditions: [],
  },
]);

if ("all" === route.params.catalogue) {
  filters.push({
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
  });
}

let search = computed(() => {
  // @ts-ignore
  return filters.find((f) => f.columnType === "_SEARCH").search;
});

const query = computed(() => {
  return `
  query DataSources($filter:DataSourcesFilter, $orderby:DataSourcesorderby){
    DataSources(limit: ${pageSize} offset: ${offset.value} filter:$filter  orderby:$orderby) {
      id
      name
      acronym
      description
      keywords
      numberOfParticipants
      type{name}
    }
    DataSources_agg (filter:$filter){
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

const { data, error } = await useGqlFetch<any, IMgError>(query, {
  variables: {
    orderby,
    filter,
  },
});

if (error.value) {
  throw new Error("Error on datasources data fetch");
}

const numberOfDataSources = computed(
  () => data?.value.data.DataSources_agg?.count || 0
);

function setCurrentPage(pageNumber: number) {
  router.push({ path: route.path, query: { page: pageNumber } });
  currentPage.value = pageNumber;
}

watch(filters, () => {
  setCurrentPage(1);
});

let activeName = ref("detailed");

const underConstructionNotice = ref();

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
            title="Data sources"
            description="Group of individuals sharing a defining demographic characteristic."
            icon="image-data-warehouse"
          >
            <template #prefix>
              <BreadCrumbs :crumbs="crumbs" current="data sources" />
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
          <SearchResultsCount
            :value="numberOfDataSources"
            label="data source"
          />
          <FilterWell :filters="filters"></FilterWell>
          <SearchResultsList>
            <CardList v-if="data?.data?.DataSources?.length > 0">
              <CardListItem
                v-for="datasource in data?.data?.DataSources"
                :key="datasource.name"
              >
                <DataSourceCard
                  :datasource="datasource"
                  :schema="route.params.schema"
                  :compact="activeName !== 'detailed'"
                  :catalogue="route.params.catalogue"
                />
              </CardListItem>
            </CardList>
            <div v-else class="flex justify-center pt-3">
              <span class="py-15 text-blue-500">
                No Datasources found with current filters
              </span>
            </div>
          </SearchResultsList>
        </template>

        <template v-if="data?.data?.Cohorts?.length > 0" #pagination>
          <Pagination
            :current-page="currentPage"
            :totalPages="
              Math.ceil(data?.data?.DataSources_agg.count / pageSize)
            "
            @update="setCurrentPage($event)"
          />
        </template>
      </SearchResults>
    </template>
  </LayoutsSearchPage>
</template>

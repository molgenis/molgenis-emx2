<script setup lang="ts">
const route = useRoute();
const router = useRouter();
const config = useRuntimeConfig();
const pageSize = 10;

useHead({ title: "Data sources" });

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
    result._and["networks"] = { id: { equals: route.params.catalogue } };
  }
  return result;
});

let graphqlURL = computed(() => `/${route.params.schema}/catalogue/graphql`);
const { data, pending, error, refresh } = await useFetch(graphqlURL.value, {
  key: `cohorts-${offset.value}`,
  baseURL: config.public.apiBase,
  method: "POST",
  body: {
    query,
    variables: { orderby, filter },
  },
});
if (error) {
  console.log(error);
  console.log(query);
}

function setCurrentPage(pageNumber: number) {
  router.push({ path: route.path, query: { page: pageNumber } });
  currentPage.value = pageNumber;
}

watch(filters, () => {
  setCurrentPage(1);
});

let activeName = ref("detailed");

const NOTICE_SETTING_KEY = "CATALOGUE_NOTICE";
const underConstructionNotice = ref();

fetchSetting(NOTICE_SETTING_KEY).then((resp) => {
  const setting = resp.data["_settings"].find(
    (setting: { key: string; value: string }) => {
      return setting.key === NOTICE_SETTING_KEY;
    }
  );

  if (setting) {
    underConstructionNotice.value = setting.value;
  }
});
const crumbs: any = {};
if (route.params.catalogue) {
  crumbs[
    route.params.catalogue
  ] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}`;
} else {
  crumbs["Home"] = `/${route.params.schema}/ssr-catalogue`;
}
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
          >
            <template #prefix>
              <BreadCrumbs :crumbs="crumbs" current="cohorts" />
            </template>
            <template #suffix>
              <div
                v-if="underConstructionNotice"
                class="mt-1 mb-5 text-left bg-yellow-200 rounded-lg text-black py-5 px-5 flex"
              >
                <BaseIcon
                  name="info"
                  :width="55"
                  class="hidden md:block mr-3"
                />
                <div class="inline-block">{{ underConstructionNotice }}</div>
              </div>

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

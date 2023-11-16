<script setup lang="ts">
import type {IFilter} from "~/interfaces/types";

const route = useRoute();
const router = useRouter();
const config = useRuntimeConfig();
const pageSize = 20;

useHead({ title: "Catalogues" });

let filters: IFilter[] = reactive([
  {
    title: "Search in catalogues",
    columnType: "_SEARCH",
    search: "",
    initialCollapsed: false,
  },
]);

let search = computed(() => {
  // @ts-ignore
  return filters.find((f) => f.columnType === "_SEARCH").search;
});

const query = computed(() => {
  return `
  query Networks($filter:NetworksFilter, $orderby:Networksorderby){
    Networks(filter:$filter  orderby:$orderby) {
      id
      name
      acronym
      description
      logo {
        id
        size
        extension
        url
      }
    }
    Networks_agg (filter:$filter){
      count
    }
  }
  `;
});

const orderby = { acronym: "ASC" };

const filter = computed(() => buildQueryFilter(filters, search.value));

let graphqlURL = computed(() => `/${route.params.schema}/api/graphql`);
const { data, pending, error, refresh } = await useFetch(graphqlURL.value, {
  key: `networks`,
  baseURL: config.public.apiBase,
  method: "POST",
  body: {
    query,
    variables: { orderby, filter },
  },
});

console.log(data)

let activeName = ref("compact");
</script>

<template>
  <LayoutsLandingPage>
    <PageHeader
      title="health research data and sample catalogue"
      description="The MOLGENIS catalogue community unites health research data and sample catalogues from diverse research projects and topic networks enabling integrated search while improving sustainability and reducing duplicated efforts"
    >
      <template #suffix>
        <div class="relative justify-center flex flex-col md:flex-row">
          <div class="flex flex-col items-center max-w-sm">
            <NuxtLink :to="`/${route.params.schema}/ssr-catalogue/all`">
              <Button label="Browse all catalogues" />
            </NuxtLink>
          </div>
          <div class="flex flex-col items-center max-w-sm ml-3">
            <NuxtLink href="#subcatalogues">
              <Button
                type="outline"
                label="Browse a specific catalogue"/>
            </NuxtLink>
          </div>
        </div>
      </template>
    </PageHeader>
    <PageHeader
      title=""
      description="Select a catalogue below:"
      id="subcatalogues"
    >
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
          <FilterSidebar title="Filters" :filters="filters" />
        </SearchResultsViewTabsMobile>
      </template>
    </PageHeader>
    <SearchResultsList>
      <CardList>
        <CardListItem
          v-for="network in data?.data?.Networks"
          :key="network.name"
        >
          <CatalogueCard
            :network="network"
            :schema="route.params.schema"
            :compact="activeName !== 'detailed'"
          />
        </CardListItem>
        <CardListItem key="all">
          <CatalogueCard
            :network="{
              id: 'all',
              acronym: 'all',
              name: 'browse all catalogues',
            }"
            :schema="route.params.schema"
            :compact="activeName !== 'detailed'"
          />
        </CardListItem>
      </CardList>
    </SearchResultsList>
  </LayoutsLandingPage>
</template>

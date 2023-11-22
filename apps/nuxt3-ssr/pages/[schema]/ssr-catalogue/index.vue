<script setup lang="ts">
import type { IFilter } from "~/interfaces/types";

const route = useRoute();
const router = useRouter();
const config = useRuntimeConfig();
const pageSize = 20;

const cohortOnly = computed(() => {
  const routeSetting = route.query["cohort-only"] as string;
  return routeSetting === "true" || config.public.cohortOnly;
});
if (cohortOnly) {
  await navigateTo(`/${route.params.schema}/ssr-catalogue/all`);
}

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
let activeName = ref("compact");
</script>

<template>
  <LayoutsLandingPage>
    <PageHeader
      title="health research data and sample catalogue"
      description="MOLGENIS catalogue unites European research projects and networks cataloguing efforts."
      :truncate="false"
    >
      <template #suffix>
        <div
          class="relative justify-center flex flex-col md:flex-row text-title"
        >
          <div class="flex flex-col items-center max-w-sm lg:mt-5">
            <NuxtLink :to="`/${route.params.schema}/ssr-catalogue/all`">
              <Button label="Search all catalogues" />
            </NuxtLink>
            <p class="mt-1 mb-0 text-center lg:mt-10 text-body-lg">
              or select a specific catalogue below:
            </p>
          </div>
        </div>
      </template>
    </PageHeader>
    <SearchResultsList>
      <CardList v-if="data?.data?.Networks?.length > 0">
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
      </CardList>
      <div v-else class="flex justify-center pt-3">
        <span class="py-15 text-blue-500">
          No Networks found with current filters
        </span>
      </div>
    </SearchResultsList>
  </LayoutsLandingPage>
</template>

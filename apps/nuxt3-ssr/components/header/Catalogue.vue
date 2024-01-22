<script setup lang="ts">
const route = useRoute();
const config = useRuntimeConfig();

const cohortOnly = computed(() => {
  const routeSetting = route.query["cohort-only"] as string;
  return routeSetting === "true" || config.public.cohortOnly;
});

const catalogueRouteParam = route.params.catalogue as string;
const scoped = route.params.catalogue !== "all";
const modelFilter = scoped ? { id: { equals: catalogueRouteParam } } : {};
const networksFilter = scoped
  ? { id: { equals: catalogueRouteParam } }
  : undefined;

const models = await $fetch(`/${route.params.schema}/catalogue/graphql`, {
  baseURL: config.public.apiBase,
  method: "POST",
  body: {
    query: `
            query Networks($networksFilter:NetworksFilter) {
              Networks(filter:$networksFilter){models{id}}
            }`,
    variables: { networksFilter: modelFilter },
  },
});

const variablesFilter = scoped
  ? {
      resource: {
        id: {
          equals: models.data.Networks[0].models
            ? models.data.Networks[0].models.map((m: { id: string }) => m.id)
            : "cannot find any",
        },
      },
    }
  : undefined;

const query = `
      query MyQuery($networksFilter:NetworksFilter,$variablesFilter:VariablesFilter) {
        Networks(filter:$networksFilter) {
              id,
              dataSources_agg{count}
              cohorts_agg{count}
              networks_agg{count}
              logo{url}
       }
       Variables_agg(filter:$variablesFilter) {
          count
       }
    }`;

const data = await $fetch(`/${route.params.schema}/catalogue/graphql`, {
  baseURL: config.public.apiBase,
  method: "POST",
  body: {
    query,
    variables: {
      networksFilter,
      variablesFilter,
    },
  },
});

const catalogue = catalogueRouteParam === "all" ? {} : data.data?.Networks[0];
const variableCount = data.data?.Variables_agg?.count;

const menu = [
  {
    label: "overview",
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}`,
  },
];
if (catalogueRouteParam === "all" || catalogue.cohorts_agg?.count > 0)
  menu.push({
    label: "Cohorts",
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/cohorts`,
  });
if (
  (!cohortOnly.value && catalogueRouteParam === "all") ||
  catalogue.dataSources_agg?.count > 0
)
  menu.push({
    label: "Data sources",
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/datasources`,
  });
if (!cohortOnly.value && variableCount > 0)
  menu.push({
    label: "Variables",
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/variables`,
  });
if (!cohortOnly.value && catalogue.networks_agg?.count > 0)
  menu.push({
    label: "Networks",
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/networks`,
  });
if (cohortOnly.value) {
  menu.push({
    label: "About",
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/about`,
  });
} else {
  menu.push({
    label: "About",
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/networks/${catalogueRouteParam}`,
  });
}
if (!cohortOnly.value) {
  menu.push({
    label: "Other catalogues",
    link: `/${route.params.schema}/ssr-catalogue`,
  });
}
</script>

<template>
  <header class="antialiased px-5 lg:px-0 xl:bg-white">
    <Container>
      <div class="items-center justify-between hidden xl:flex h-25">
        <Logo
          :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}`"
          :image="
            catalogue?.logo?.url ||
            '/_nuxt-styles/img/molgenis-logo-blue-small.svg'
          "
        />
        <MainNavigation :navigation="menu" :invert="true" />
        <!--  <div class="w-[450px]">
           <SearchBar />
        </div>-->

        <!-- <HeaderButton label="Favorites" icon="star" />
        <HeaderButton label="Account" icon="user" /> -->
      </div>

      <div class="pt-5 xl:hidden">
        <div class="relative flex items-center h-12.5 justify-between mb-4">
          <!-- <HamburgerMenu :navigation="menu" /> -->
          <div class="absolute -translate-x-1/2 left-1/2">
            <LogoMobile
              :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}`"
              :image="catalogue?.logo?.url"
            />
          </div>

          <div class="flex gap-3">
            <!-- <HeaderButton label="Favorites" icon="star" />
            <HeaderButton label="Account" icon="user" /> -->
          </div>
        </div>

        <MainNavigation :navigation="menu" :showMoreButton="false" />
        <div class="w-full pt-6">
          <!-- <SearchBar /> -->
        </div>
      </div>
    </Container>
  </header>
</template>

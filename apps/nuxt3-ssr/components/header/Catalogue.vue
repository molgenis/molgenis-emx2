<script setup lang="ts">
const route = useRoute();
const config = useRuntimeConfig();

const props = defineProps<{
  catalogue: any;
  variableCount: number;
}>();

const cohortOnly = computed(() => {
  const routeSetting = route.query["cohort-only"] as string;
  return routeSetting === "true" || config.public.cohortOnly;
});

const catalogueRouteParam = route.params.catalogue as string;

const menu = [
  {
    label: "overview",
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}`,
  },
];
if (catalogueRouteParam === "all" || props.catalogue.cohorts_agg?.count > 0)
  menu.push({
    label: "Cohorts",
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/cohorts`,
  });
if (
  (!cohortOnly.value && catalogueRouteParam === "all") ||
  (!cohortOnly.value && props.catalogue.dataSources_agg?.count > 0)
)
  menu.push({
    label: "Data sources",
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/datasources`,
  });

if (!cohortOnly.value && props.variableCount > 0)
  menu.push({
    label: "Variables",
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/variables`,
  });

if (!cohortOnly.value && props.catalogue?.networks_agg?.count > 0)
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
    <UIContainer>
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
    </UIContainer>
  </header>
</template>

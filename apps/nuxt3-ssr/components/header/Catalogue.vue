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
if (catalogueRouteParam === "all" || props.catalogue.collections_agg?.count > 0)
  menu.push({
    label: "collections(" + props.catalogue.collections_agg?.count + ")",
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/collections`,
  });
if (!cohortOnly.value && props.variableCount > 0)
  menu.push({
    label: "Variables(" + props.variableCount + ")",
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/variables`,
  });

if (props.catalogue.collections_groupBy?.length) {
  props.catalogue.collections_groupBy.forEach((sub) => {
    const collectionTypeMetadata = getCollectionMetadataForType(sub.type.name);
    menu.push({
      label: collectionTypeMetadata.plural + "(" + sub.count + ")",
      link:
        `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/` +
        collectionTypeMetadata.path,
    });
  });
}

if (cohortOnly.value) {
  menu.push({
    label: "About",
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/about`,
  });
} else if (catalogueRouteParam && catalogueRouteParam !== "all") {
  menu.push({
    label: "About",
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/about-catalogue`,
  });
}

if (!cohortOnly.value) {
  menu.push({
    label: "Other catalogues",
    link: `/${route.params.schema}/ssr-catalogue`,
  });
  menu.push({
    label: "Upload data",
    link: "/apps/central/#/",
  });
}
</script>

<template>
  <header class="antialiased px-5 lg:px-0 xl:bg-white">
    <Container>
      <div class="items-center justify-between hidden xl:flex h-25">
        <Logo
          :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}`"
          :image="catalogueRouteParam === 'all' ? null : catalogue?.logo?.url"
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
          <HamburgerMenu :navigation="menu" />
          <div class="absolute -translate-x-1/2 left-1/2">
            <LogoMobile
              :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}`"
              :image="
                catalogueRouteParam === 'all' ? null : catalogue?.logo?.url
              "
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

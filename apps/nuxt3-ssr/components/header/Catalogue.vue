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

const menu: { label: string; link: string }[] = [];

if (route.params.resourceType) {
  menu.push({
    label: "overview",
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}`,
  });
}

if (props.catalogue.resources_groupBy?.length) {
  props.catalogue.resources_groupBy.forEach(
    (sub: { type: { name: string }; count: string }) => {
      const resourceTypeMetadata = getResourceMetadataForType(sub.type.name);
      menu.push({
        label: resourceTypeMetadata.plural,
        link:
          `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/` +
          resourceTypeMetadata.path,
      });
    }
  );
}

if (!cohortOnly.value && props.variableCount > 0)
  menu.push({
    label: "Variables",
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/variables`,
  });

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
  menu.push({
    label: "Manuals",
    link: "/apps/docs/#/catalogue/",
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

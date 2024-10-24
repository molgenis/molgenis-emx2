<script setup lang="ts">
import type { UIResource, UIResourceType } from "~/interfaces/types";

const route = useRoute();
const config = useRuntimeConfig();

const props = defineProps<{
  catalogue?: UIResource;
  variableCount: number;
  resourceTypes: UIResourceType[];
}>();

const cohortOnly = computed(() => {
  const routeSetting = route.query["cohort-only"] as string;
  return routeSetting === "true" || config.public.cohortOnly;
});

const catalogueRouteParam = route.params.catalogue as string;

const menu: { label: string; link: string }[] = [];

// the variable route does not set the resourceType param, therefore check the route name
if (
  route.params.resourceType ||
  route.name === "schema-ssr-catalogue-catalogue-variables"
) {
  menu.push({
    label: "overview",
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}`,
  });
}

if (props.resourceTypes.length > 0) {
  props.resourceTypes.forEach((resourceType) => {
    const resourceTypeMetadata = getResourceMetadataForType(
      resourceType.type?.name
    );
    menu.push({
      label: resourceTypeMetadata.plural,
      link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/${resourceTypeMetadata.path}`,
    });
  });
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
    link: `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/about/${catalogueRouteParam}`,
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
          :image="
            catalogueRouteParam === 'all' ? undefined : catalogue?.logo?.url
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
          <HamburgerMenu :navigation="menu" />
          <div class="absolute -translate-x-1/2 left-1/2">
            <LogoMobile
              :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}`"
              :image="
                catalogueRouteParam === 'all' ? undefined : catalogue?.logo?.url
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

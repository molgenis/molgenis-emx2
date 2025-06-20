<script setup lang="ts">
import { useRoute, useRuntimeConfig } from "#app";
import { useDatasetStore } from "#imports";
import { computed, ref } from "vue";
import type { UIResource } from "~/interfaces/types";

const route = useRoute();
const config = useRuntimeConfig();
const datasetStore = useDatasetStore();

const props = defineProps<{
  catalogue?: UIResource;
  variableCount: number;
  collectionCount: number;
  networkCount: number;
}>();

const cohortOnly = computed(() => {
  const routeSetting = route.query["cohort-only"] as string;
  return routeSetting === "true" || config.public.cohortOnly;
});

const catalogueRouteParam = route.params.catalogue as string;

const menu: { label: string; link: string }[] = [];

const showCartModal = ref<boolean>(false);

// the variable route does not set the resourceType param, therefore check the route name
if (
  route.params.resourceType ||
  route.name === "schema-catalogue-catalogue-variables"
) {
  menu.push({
    label: "overview",
    link: `/${catalogueRouteParam}`,
  });
}

if (props.collectionCount > 0) {
  menu.push({
    label: "Collections",
    link: `/${catalogueRouteParam}/collections`,
  });
}

if (props.networkCount > 0 && !cohortOnly.value) {
  menu.push({
    label: "Networks",
    link: `/${catalogueRouteParam}/networks`,
  });
}

if (props.variableCount > 0 && !cohortOnly.value)
  menu.push({
    label: "Variables",
    link: `/${catalogueRouteParam}/variables`,
  });

if (cohortOnly.value) {
  menu.push({
    label: "About",
    link: `/${catalogueRouteParam}/about`,
  });
} else if (catalogueRouteParam && catalogueRouteParam !== "all") {
  menu.push({
    label: "About",
    link: `/${catalogueRouteParam}/about/${catalogueRouteParam}`,
  });
}

if (!cohortOnly.value) {
  menu.push({
    label: "Other catalogues",
    link: `/`,
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
          :link="`/${catalogueRouteParam}`"
          :image="
            catalogueRouteParam === 'all' ? undefined : catalogue?.logo?.url
          "
          :inverted="true"
        />
        <MainNavigation :navigation="menu" :invert="true" />
        <!--  <div class="w-[450px]">
           <SearchBar />
        </div>-->

        <StoreHeaderButton
          @click="showCartModal = !showCartModal"
          v-if="datasetStore.isEnabled"
        />
        <!-- <HeaderButton label="Account" icon="user" /> -->
      </div>

      <div class="pt-5 xl:hidden">
        <div class="relative flex items-center h-12.5 justify-between mb-4">
          <HamburgerMenu :navigation="menu" />
          <div class="absolute -translate-x-1/2 left-1/2">
            <LogoMobile
              :link="`/${catalogueRouteParam}`"
              :image="
                catalogueRouteParam === 'all' ? undefined : catalogue?.logo?.url
              "
            />
          </div>

          <div class="flex gap-3">
            <StoreHeaderButton
              @click="showCartModal = !showCartModal"
              v-if="datasetStore.isEnabled"
            />
            <!-- <HeaderButton label="Account" icon="user" /> -->
          </div>
        </div>

        <MainNavigation :navigation="menu" :showMoreButton="false" />
        <div class="w-full pt-6">
          <!-- <SearchBar /> -->
        </div>
      </div>
    </Container>
    <StoreModal :show="showCartModal" @close="showCartModal = false" />
  </header>
</template>

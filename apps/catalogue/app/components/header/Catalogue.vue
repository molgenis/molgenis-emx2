<script setup lang="ts">
import { useRoute, useRuntimeConfig } from "#app";
import { useDatasetStore } from "#imports";
import { computed, ref } from "vue";
import type { UIResource } from "../../../interfaces/types";
import Container from "../../../../tailwind-components/app/components/Container.vue";
import Logo from "../../../../tailwind-components/app/components/Logo.vue";
import LogoMobile from "../../../../tailwind-components/app/components/LogoMobile.vue";
import MainNavigation from "../../components/MainNavigation.vue";
import HamburgerMenu from "../../components/HamburgerMenu.vue";
import StoreHeaderButton from "../../components/store/HeaderButton.vue";
import StoreModal from "../../components/store/Modal.vue";

const route = useRoute();
const config = useRuntimeConfig();
const datasetStore = useDatasetStore();

const props = defineProps<{
  catalogue?: UIResource;
  variableCount: number;
  collectionCount: number;
  networkCount: number;
  logoSrc?: string;
}>();

const cohortOnly = computed(() => {
  const routeSetting = route.query["cohort-only"] as string;
  return routeSetting === "true" || config.public.cohortOnly;
});

const catalogueRouteParam = (route.query.catalogue ||
  route.params.resourceId ||
  (route.path.startsWith("/all") ? "all" : undefined)) as string;
const currentResourceId = (route.params.resourceId ||
  (route.path.startsWith("/all") ? "all" : "")) as string;

const menu: { label: string; link: string }[] = [];

const showCartModal = ref<boolean>(false);

const buildUrl = (path: string) => {
  if (catalogueRouteParam) {
    const pathResourceId = path.split("/")[1]?.split("?")[0];
    if (pathResourceId === catalogueRouteParam) {
      return path;
    }
    return `${path}?catalogue=${catalogueRouteParam}`;
  }
  return path;
};

if (currentResourceId) {
  menu.push({
    label: "overview",
    link: buildUrl(`/${currentResourceId}`),
  });
}

if (props.collectionCount > 0) {
  menu.push({
    label: "Collections",
    link: buildUrl(`/${currentResourceId}/collections`),
  });
}

if (props.networkCount > 0) {
  menu.push({
    label: "Networks",
    link: buildUrl(`/${currentResourceId}/networks`),
  });
}

if (props.variableCount > 0 && !cohortOnly.value)
  menu.push({
    label: "Variables",
    link: buildUrl(`/${currentResourceId}/variables`),
  });

if (currentResourceId && (cohortOnly.value || currentResourceId !== "all")) {
  menu.push({
    label: "About",
    link: buildUrl(`/${currentResourceId}/about`),
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
  <header class="antialiased px-5 lg:px-0 xl:bg-navigation-dynamic">
    <Container>
      <div class="items-center justify-between hidden xl:flex h-25">
        <Logo
          :link="buildUrl(`/${currentResourceId}`)"
          :image="currentResourceId === 'all' ? logoSrc : catalogue?.logo?.url"
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
              :link="buildUrl(`/${currentResourceId}`)"
              :image="
                currentResourceId === 'all' ? logoSrc : catalogue?.logo?.url
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

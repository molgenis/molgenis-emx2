<script setup lang="ts">
import type { UIResource } from "~/interfaces/types";

const datasetStore = useDatasetStore();

const route = useRoute();
const config = useRuntimeConfig();

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
    link: `/${route.params.schema}/catalogue/${catalogueRouteParam}`,
  });
}

if (props.collectionCount > 0) {
  menu.push({
    label: "Collections",
    link: `/${route.params.schema}/catalogue/${catalogueRouteParam}/collections`,
  });
}

if (props.networkCount > 0 && !cohortOnly.value) {
  menu.push({
    label: "Networks",
    link: `/${route.params.schema}/catalogue/${catalogueRouteParam}/networks`,
  });
}

if (props.variableCount > 0 && !cohortOnly.value)
  menu.push({
    label: "Variables",
    link: `/${route.params.schema}/catalogue/${catalogueRouteParam}/variables`,
  });

if (cohortOnly.value) {
  menu.push({
    label: "About",
    link: `/${route.params.schema}/catalogue/${catalogueRouteParam}/about`,
  });
} else if (catalogueRouteParam && catalogueRouteParam !== "all") {
  menu.push({
    label: "About",
    link: `/${route.params.schema}/catalogue/${catalogueRouteParam}/about/${catalogueRouteParam}`,
  });
}

if (!cohortOnly.value) {
  menu.push({
    label: "Other catalogues",
    link: `/${route.params.schema}/catalogue`,
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
          :link="`/${route.params.schema}/catalogue/${catalogueRouteParam}`"
          :image="
            catalogueRouteParam === 'all' ? undefined : catalogue?.logo?.url
          "
        />
        <MainNavigation :navigation="menu" :invert="true" />
        <!--  <div class="w-[450px]">
           <SearchBar />
        </div>-->

        <StoreHeaderButton @click="showCartModal = !showCartModal" />
        <!-- <HeaderButton label="Account" icon="user" /> -->
      </div>

      <div class="pt-5 xl:hidden">
        <div class="relative flex items-center h-12.5 justify-between mb-4">
          <HamburgerMenu :navigation="menu" />
          <div class="absolute -translate-x-1/2 left-1/2">
            <LogoMobile
              :link="`/${route.params.schema}/catalogue/${catalogueRouteParam}`"
              :image="
                catalogueRouteParam === 'all' ? undefined : catalogue?.logo?.url
              "
            />
          </div>

          <div class="flex gap-3">
            <StoreHeaderButton @click="showCartModal = !showCartModal" />
            <!-- <HeaderButton label="Account" icon="user" /> -->
          </div>
        </div>

        <MainNavigation :navigation="menu" :showMoreButton="false" />
        <div class="w-full pt-6">
          <!-- <SearchBar /> -->
        </div>
      </div>
    </Container>
    <SideModal
      :show="showCartModal"
      :slideInRight="true"
      :fullScreen="false"
      :includeFooter="true"
      buttonAlignment="left"
      @close="showCartModal = false"
    >
      <ContentBlockModal title="Datasets">
        <StoreModalResourceList
          v-if="
            datasetStore.datasets.value &&
            Object.keys(datasetStore.datasets.value).length > 0
          "
        />
        <p v-else>Cart is empty</p>
      </ContentBlockModal>
      <template #footer>
        <Button type="primary" size="medium" icon="ShoppingCart">
          Checkout
        </Button>
      </template>
    </SideModal>
  </header>
</template>

<script setup lang="ts">
import { useRuntimeConfig, useRoute } from "#app";

const config = useRuntimeConfig();

const menu = [
  { label: "Home", link: `/catalogue` },
  config.public.cohortOnly
    ? { label: "Cohorts", link: `/catalogue/all/cohorts` }
    : {
        label: "All collections",
        link: `/catalogue/all/collections`,
      },
  config.public.cohortOnly
    ? undefined
    : {
        label: "All variables",
        link: `/catalogue/all/variables`,
      },
  config.public.cohortOnly
    ? undefined
    : {
        label: "All networks",
        link: `/catalogue/all/networks`,
      },
  {
    label: "Upload data",
    link: "/apps/central/#/",
  },
  {
    label: "Manuals",
    link: "/apps/docs/#/catalogue/",
  },

  // { label: "Statistical Methods", link: "#" },
  // { label: "Tables", link: "#" },
  // { label: "Manuals", link: "#" },
  config.public.cohortOnly
    ? { label: "About", link: `/catalogue/all/about` }
    : {
        label: "About",
        link: `/catalogue/about`,
      },
].filter((item) => item !== undefined);
</script>

<template>
  <header class="antialiased px-5 lg:px-0 xl:bg-navigation">
    <Container>
      <div class="items-center justify-between hidden xl:flex h-25">
        <Logo :link="`/catalogue`" />
        <MainNavigation :navigation="menu" />
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
            <LogoMobile :link="`/catalogue`" />
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

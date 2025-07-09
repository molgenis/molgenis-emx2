<script setup lang="ts">
import { useRuntimeConfig } from "#app";

const config = useRuntimeConfig();

const menu = [
  { label: "Home", link: `/` },
  config.public.cohortOnly
    ? { label: "Cohorts", link: `/all/cohorts` }
    : {
        label: "All collections",
        link: `/all/collections`,
      },
  config.public.cohortOnly
    ? undefined
    : {
        label: "All variables",
        link: `/all/variables`,
      },
  config.public.cohortOnly
    ? undefined
    : {
        label: "All networks",
        link: `/all/networks`,
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
    ? { label: "About", link: `/all/about` }
    : {
        label: "About",
        link: `/about`,
      },
].filter((item) => item !== undefined);
</script>

<template>
  <header class="antialiased px-5 lg:px-0 xl:bg-navigation">
    <Container>
      <div class="items-center justify-between hidden xl:flex h-25">
        <Logo :link="`/`" />
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
            <LogoMobile link="/" />
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

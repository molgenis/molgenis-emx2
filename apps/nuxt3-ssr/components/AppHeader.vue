<script setup>
import { computed } from "vue";

const props = defineProps({
  background: {
    type: String,
    default: "default",
    enum: ["default", "white"],
  },
});

const BACKGROUND_MAPPING = {
  default: "xl:bg-black xl:bg-opacity-20",
  white: "xl:bg-white",
};

const backgroundClass = computed(() => {
  return BACKGROUND_MAPPING[props.background];
});

const menu = [
  { label: "Home", link: "#" },
  { label: "Variables", link: "#", highlight: true },
  { label: "Cohorts", link: "#" },
  { label: "Networks", link: "#" },
  { label: "Statistical Methods", link: "#" },
  { label: "Tables", link: "#" },
  { label: "Manuals", link: "#" },
  { label: "About", link: "#" },
];
</script>

<template>
  <header :class="backgroundClass" class="antialiased px-5 lg:px-0">
    <Container>
      <div class="items-center justify-between hidden xl:flex h-25">
        <Logo />
        <MainNavigation :navigation="menu" />
        <div class="w-[450px]">
          <SearchBar />
        </div>

        <HeaderButton label="Favorites" icon="star" />
        <HeaderButton label="Account" icon="user" />
      </div>

      <div class="pt-5 xl:hidden">
        <div class="relative flex items-center h-12.5 justify-between mb-4">
          <HamburgerMenu :navigation="menu" />

          <div class="absolute -translate-x-1/2 left-1/2">
            <LogoMobile />
          </div>

          <div class="flex gap-3">
            <HeaderButton label="Favorites" icon="star" />
            <HeaderButton label="Account" icon="user" />
          </div>
        </div>

        <MainNavigation :navigation="menu" :showMoreButton="false" />
        <div class="w-full pt-6">
          <SearchBar />
        </div>
      </div>
    </Container>
  </header>
</template>

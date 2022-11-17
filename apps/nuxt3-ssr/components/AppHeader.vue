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
</script>

<template>
  <header :class="backgroundClass" class="antialiased px-5 lg:px-0">
    <Container>
      <div class="items-center justify-between hidden xl:flex h-25">
        <Logo />
        <MainNavigation />
        <div class="w-[450px]">
          <SearchBar />
        </div>

        <HeaderButton label="Favorites" icon="star" />
        <HeaderButton label="Account" icon="user" />
      </div>

      <div class="pt-5 xl:hidden">
        <div class="relative flex items-center h-12.5 justify-between mb-4">
          <HamburgerMenu />

          <div class="absolute -translate-x-1/2 left-1/2">
            <LogoMobile />
          </div>

          <div class="flex gap-3">
            <HeaderButton label="Favorites" icon="star" />
            <HeaderButton label="Account" icon="user" />
          </div>
        </div>

        <MainNavigation :showMoreButton="false" />
        <div class="w-full pt-6">
          <SearchBar />
        </div>
      </div>
    </Container>
  </header>
</template>

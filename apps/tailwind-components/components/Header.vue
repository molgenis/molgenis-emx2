<script setup lang="ts">
import { useRuntimeConfig } from "#imports";

const config = useRuntimeConfig();

// only show the theme switch if the emx2Theme is not set to a specific theme ( for example molgenis )
const showThemeSwitch =
  config.public?.emx2Theme === "" ||
  !["light", "dark"].includes(config.public?.emx2Theme as string);
</script>
<template>
  <header
    class="antialiased px-5 lg:px-0 xl:bg-navigation border-b-theme border-color-theme box-border"
  >
    <Container>
      <div class="items-center hidden xl:flex h-20">
        <slot name="logo"></slot>
        <div class="items-center justify-between hidden pl-8 xl:flex xl:grow">
          <slot name="nav"></slot>

          <div class="w-[450px]">
            <!-- <SearchBar /> -->
          </div>

          <slot name="admin" />

          <ThemeSwitch v-if="showThemeSwitch" />

          <slot name="account">
            <!-- <HeaderButton label="Favorites" icon="star" /> -->
            <HeaderButton label="Account" icon="user" />
          </slot>
        </div>
      </div>

      <div class="pt-5 xl:hidden">
        <div class="relative flex items-center h-12.5 justify-between mb-4">
          <!-- <HamburgerMenu :navigation="menu" /> -->

          <div class="absolute -translate-x-1/2 left-1/2">
            <slot name="logo-mobile"></slot>
          </div>

          <div class="flex gap-3">
            <!-- <HeaderButton label="Favorites" icon="star" /> -->
            <HeaderButton label="Account" icon="user" />
          </div>
        </div>

        <slot name="nav-mobile"></slot>
        <div class="w-full pt-6">
          <!-- <SearchBar /> -->
        </div>
      </div>
    </Container>
  </header>
</template>

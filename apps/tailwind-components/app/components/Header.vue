<script setup lang="ts">
import { computed } from "vue";
import { useRuntimeConfig } from "#imports";
import Container from "./Container.vue";
import HeaderButton from "./HeaderButton.vue";
import ThemeSwitch from "./ThemeSwitch.vue";
import { TOGGLEABLE_THEMES, useActiveUiTheme } from "../composables/useUiTheme";

const config = useRuntimeConfig();
const uiTheme = useActiveUiTheme();

// only show the theme switch if the theme is not set to a specific theme ( for example molgenis )
const showThemeSwitch = computed(() => {
  const theme = uiTheme.value || (config.public?.emx2Theme as string) || "";
  return theme === "" || TOGGLEABLE_THEMES.includes(theme);
});

const props = withDefaults(
  defineProps<{
    wide?: boolean;
  }>(),
  {
    wide: false,
  }
);
</script>
<template>
  <header
    class="antialiased px-5 lg:px-0 bg-navigation border-b-theme border-color-theme box-border"
  >
    <Container :wide="props.wide">
      <!-- large: logo, menu, theme switch and account in one row -->
      <div class="items-center hidden xl:flex h-20" data-testid="header-large">
        <slot name="logo"></slot>

        <div class="flex justify-between items-center w-full">
          <div class="items-center hidden pl-8 xl:flex">
            <slot name="nav"></slot>
          </div>
          <slot name="admin" />

          <div class="flex items-center gap-4">
            <ThemeSwitch v-if="showThemeSwitch" />

            <slot name="account" :iconOnly="false">
              <HeaderButton label="Account" icon="user" />
            </slot>
          </div>
        </div>
      </div>

      <!-- medium: logo and actions, with the menu in a second row -->
      <div class="hidden md:block xl:hidden pt-5" data-testid="header-medium">
        <div class="flex items-center justify-between h-12.5 mb-4">
          <slot name="logo"></slot>

          <div class="flex items-center gap-4">
            <ThemeSwitch v-if="showThemeSwitch" compact />
            <slot name="account" :iconOnly="false">
              <HeaderButton label="Account" icon="user" />
            </slot>
          </div>
        </div>

        <div class="pb-5">
          <slot name="nav-mobile"></slot>
        </div>
      </div>

      <!-- small: hamburger, logo icon and icon-only actions, with an optional second row -->
      <div class="md:hidden py-5" data-testid="header-small">
        <div class="relative flex items-center h-12.5 justify-between">
          <slot name="nav-hamburger"></slot>

          <div class="absolute -translate-x-1/2 left-1/2">
            <slot name="logo-mobile"></slot>
          </div>

          <div class="flex items-center gap-3">
            <ThemeSwitch v-if="showThemeSwitch" compact />
            <slot name="account" :iconOnly="true">
              <HeaderButton label="Account" icon="user" iconOnly />
            </slot>
          </div>
        </div>

        <div v-if="$slots['nav-mobile-row']" class="pt-4">
          <slot name="nav-mobile-row"></slot>
        </div>
      </div>
    </Container>
  </header>
</template>

<script setup lang="ts">
import { computed } from "vue";
import BaseIcon from "./BaseIcon.vue";
import NavigationMenuItems from "./NavigationMenuItems.vue";
import NavigationMenuLink from "./NavigationMenuLink.vue";
import type { MenuItem } from "../../types/types";
import { useAppConfig } from "#app";

const appConfig = useAppConfig();
const basePath = String(appConfig.basePath ?? "/");

const props = withDefaults(
  defineProps<{
    navigation: MenuItem[];
    showMoreButton?: boolean;
    invert?: boolean;
    maximumButtonShown?: number;
  }>(),
  {
    showMoreButton: true,
    invert: false,
    maximumButtonShown: 4,
  }
);

const mainNavItems = computed(() =>
  props.navigation.slice(0, props.maximumButtonShown)
);
const overFlowItems = computed(() =>
  props.navigation.slice(props.maximumButtonShown)
);

const linkClass =
  "flex items-center gap-1 tracking-widest transition-colors border border-b-0 border-transparent font-display text-heading-xl hover:underline whitespace-nowrap";

const moreButtonClass =
  "flex items-center gap-1 pt-3 pb-2 pl-4 pr-2 -mt-3 -ml-4 tracking-widest transition-colors duration-300 translate-y-1 border border-b-0 border-transparent rounded-t-3px font-display text-heading-xl hover:border-white";
</script>

<template>
  <nav class="flex items-center justify-between gap-6 xl:justify-center">
    <template
      v-for="mainNavItem in mainNavItems"
      :key="`${mainNavItem.label}-${mainNavItem.link}`"
    >
      <template v-if="!mainNavItem.submenu || mainNavItem.submenu.length === 0">
        <NavigationMenuLink
          :item="mainNavItem"
          :base-path="basePath"
          :link-class="`${linkClass} ${invert ? 'text-sub-menu' : 'text-menu'}`"
        />
      </template>
      <template v-else>
        <VMenu placement="bottom-end" :distance="-1">
          <button
            :class="[linkClass, invert ? 'text-sub-menu' : 'text-menu']"
            :data-test="`main-submenu-${mainNavItem.label}`"
          >
            {{ mainNavItem.label }}
            <BaseIcon name="caret-down" />
          </button>

          <template #popper>
            <NavigationMenuItems
              :items="mainNavItem.submenu"
              :base-path="basePath"
              :invert="invert"
            />
          </template>
        </VMenu>
      </template>
    </template>

    <VMenu
      placement="bottom-end"
      :distance="-1"
      v-if="overFlowItems.length > 0 && showMoreButton"
    >
      <button
        :class="[moreButtonClass, invert ? 'text-sub-menu' : 'text-menu']"
        data-test="more-menu-trigger"
      >
        More
        <BaseIcon name="caret-down" />
      </button>

      <template #popper>
        <NavigationMenuItems
          :items="overFlowItems"
          :base-path="basePath"
          :invert="invert"
        />
      </template>
    </VMenu>
  </nav>
</template>

<style>
.v-popper--theme-menu .v-popper__arrow-container {
  display: none;
}

.v-popper--theme-menu .v-popper__inner {
  border-radius: 0;
  box-shadow: none;
  border: 0;
  background: none;
  padding: 0;
  overflow: unset;
}
</style>

<script setup lang="ts">
import BaseIcon from "./BaseIcon.vue";
import NavigationMenuLink from "./NavigationMenuLink.vue";
import type { MenuItem } from "../../types/types";

defineOptions({
  name: "NavigationMenuItems",
});

defineProps<{
  items: MenuItem[];
  basePath: string;
  invert: boolean;
}>();

const submenuListClass =
  "flex flex-col gap-1.5 rounded-3px rounded-tr-none bg-form p-6 shadow-xl text-body-base";
const submenuLinkClass =
  "font-display text-heading-xl whitespace-nowrap transition-colors hover:underline tracking-widest text-sub-menu hover:text-sub-menu-hover";
const submenuButtonClass =
  "font-display text-heading-xl whitespace-nowrap transition-colors hover:underline tracking-widest text-sub-menu flex items-center gap-1 border border-b-0 border-transparent";
</script>

<template>
  <ol :class="submenuListClass">
    <li v-for="item in items" :key="`${item.label}-${item.link}`">
      <template v-if="!item.submenu || item.submenu.length === 0">
        <NavigationMenuLink
          :item="item"
          :base-path="basePath"
          :link-class="submenuLinkClass"
        />
      </template>
      <template v-else>
        <VMenu placement="right-start" :distance="-1">
          <button
            :class="[
              submenuButtonClass,
              invert ? 'text-sub-menu' : 'text-menu',
            ]"
            :data-test="`submenu-trigger-${item.label}`"
          >
            {{ item.label }}
            <BaseIcon name="caret-right" />
          </button>

          <template #popper>
            <NavigationMenuItems
              :items="item.submenu"
              :base-path="basePath"
              :invert="invert"
            />
          </template>
        </VMenu>
      </template>
    </li>
  </ol>
</template>

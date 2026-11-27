<script setup lang="ts">
import NavigationMenuLink from "./NavigationMenuLink.vue";
import type { MenuItem } from "../../types/types";

defineOptions({
  name: "HeaderHamburgerMenuItems",
});

withDefaults(
  defineProps<{
    items: MenuItem[];
    depth?: number;
  }>(),
  {
    depth: 0,
  }
);

const linkClass =
  "font-display tracking-widest transition-colors hover:underline text-sub-menu hover:text-sub-menu-hover";
</script>

<template>
  <ul class="flex flex-col gap-4" :class="{ 'pl-6 pt-4': depth > 0 }">
    <li v-for="item in items" :key="`${item.label}-${item.location}`">
      <NavigationMenuLink
        v-if="!item.submenu || item.submenu.length === 0"
        :item="item"
        :link-class="`${linkClass} ${
          depth === 0 ? 'text-heading-4xl' : 'text-heading-2xl'
        }`"
      />
      <template v-else>
        <span
          class="font-display tracking-widest text-sub-menu"
          :class="depth === 0 ? 'text-heading-4xl' : 'text-heading-2xl'"
        >
          {{ item.label }}
        </span>
        <HeaderHamburgerMenuItems :items="item.submenu" :depth="depth + 1" />
      </template>
    </li>
  </ul>
</template>

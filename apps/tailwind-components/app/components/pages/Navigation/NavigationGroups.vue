<script setup lang="ts">
import NavigationCards from "./NavigationCards.vue";
import type { INavigationGroups } from "../../../../types/cms";

defineProps<INavigationGroups>();
</script>

<template>
  <nav aria-label="Go to page">
    <ul
      :id="id"
      class="w-full m-0 list-none flex justify-center items-center flex-col md:flex-row gap-5"
    >
      <slot name="links"></slot>
    </ul>
    <slot></slot>
  </nav>
</template>

<script setup lang="ts">
import NavigationCards from "./NavigationCards.vue";
import type { INavigationGroups } from "../../../../types/cms";

const props = defineProps<INavigationGroups>();
</script>

<template>
  <ul
    class="list-none flex justify-stretch items-center flex-col md:flex-row md:flex-wrap gap-5"
    aria-label="subnavigation"
  >
    <template v-for="link in links" :key="link.url">
      <NavigationCards
        :title="link.title"
        :description="link.description"
        :url="link.url"
        :url-is-external="link.urlIsExternal"
        :url-label="link.urlLabel"
        class="grow w-full md:w-auto"
      />
    </template>
  </ul>
</template>

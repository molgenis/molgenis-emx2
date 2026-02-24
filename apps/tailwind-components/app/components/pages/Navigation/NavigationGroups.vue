<script setup lang="ts">
import NavigationCards from "./NavigationCards.vue";
import type { INavigationGroups } from "../../../../types/cms";

const props = defineProps<INavigationGroups>();
</script>

<template>
  <ul
    :id="id"
    class="w-full m-0 list-none flex justify-center items-center flex-col md:flex-row gap-5"
    aria-label="subnavigation"
  >
    <template v-for="link in links" :key="link.id">
      <NavigationCards
        :id="link.id"
        :title="link.title"
        :description="link.description"
        :url="link.url"
        :url-is-external="link.urlIsExternal"
        :url-label="link.urlLabel"
        class="w-full md:w-80"
      />
    </template>
  </ul>
</template>

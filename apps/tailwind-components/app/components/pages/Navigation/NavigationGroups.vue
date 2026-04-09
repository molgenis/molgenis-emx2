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
      <li v-for="card in links" :key="card.id">
        <NavigationCards
          :id="card.id"
          :title="card.title"
          :description="card.description"
          :url="card.url"
          :url-is-external="card.urlIsExternal"
          :url-label="card.urlLabel"
          class="w-full md:w-80"
        />
      </li>
    </ul>
    <slot></slot>
  </nav>
</template>

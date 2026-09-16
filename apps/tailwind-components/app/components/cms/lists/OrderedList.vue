<script setup lang="ts">
import ListItem from "./ListItem.vue";
import type { IOrderedLists } from "../../../../types/cms.ts";
defineProps<IOrderedLists>();
</script>

<template>
  <ol :id="id" class="list-decimal ml-7.5 mb-2.5 md:ml-10 w-full">
    <ListItem v-for="item in orderedItems" :text="item" />
  </ol>
</template>

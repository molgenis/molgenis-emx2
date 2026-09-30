<script setup lang="ts">
import ListItem from "./ListItem.vue";
import type { IUnorderedLists } from "../../../../types/cms.ts";
defineProps<IUnorderedLists>();
</script>

<template>
  <ul :id="id" class="list-disc ml-7.5 my-2.5 md:ml-10 w-full">
    <ListItem v-for="item in unorderedItems" :text="item" />
  </ul>
</template>

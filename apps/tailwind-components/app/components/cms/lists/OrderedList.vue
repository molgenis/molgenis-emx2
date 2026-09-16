<script setup lang="ts">
import ListItem from "./ListItem.vue";
import type { IOrderedLists } from "../../../../types/cms.ts";
defineProps<IOrderedLists>();
</script>

<template>
  <ol :id="id" class="list-disc ml-7.5 md:ml-10 w-full">
    <ListItem v-for="item in items" :text="item" />
  </ol>
</template>

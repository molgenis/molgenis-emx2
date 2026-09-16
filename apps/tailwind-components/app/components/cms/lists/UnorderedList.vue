<script setup lang="ts">
import ListItem from "./ListItem.vue";
import type { IUnorderedLists } from "../../../../types/cms.ts";

const props = withDefaults(
  defineProps<IUnorderedLists & { isEditable?: boolean }>(),
  {
    isEditable: false,
  }
);
</script>

<template>
  <ul :id="id" class="list-disc ml-7.5 md:ml-10 w-full">
    <ListItem v-for="item in items" :text="item" />
  </ul>
</template>

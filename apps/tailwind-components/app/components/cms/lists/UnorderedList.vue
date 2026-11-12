<script setup lang="ts">
import ListItem from "./ListItem.vue";
import type { IUnorderedLists } from "../../../../types/cms.ts";
const props = defineProps<IUnorderedLists & { isEditable?: boolean }>();
const emit = defineEmits(["updateUnorderedItems"]);
</script>

<template>
  <ul :id="id" class="list-disc ml-7.5 my-2.5 md:ml-10 w-auto">
    <ListItem
      v-for="(item, key) in unorderedItems"
      :text="item"
      :key="key"
      :contenteditable="props.isEditable"
      @input="
        props.isEditable &&
          emit('updateUnorderedItems', {
            text: ($event.target as HTMLElement)?.innerHTML ?? '',
            key,
          })
      "
    />
  </ul>
</template>

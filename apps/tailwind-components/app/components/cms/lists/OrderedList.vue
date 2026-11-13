<script setup lang="ts">
import ListItem from "./ListItem.vue";
import type { IOrderedLists } from "../../../../types/cms.ts";
const props = defineProps<IOrderedLists & { isEditable?: boolean }>();
const emit = defineEmits(["updateOrderedItems", "inlineSave"]);
</script>

<template>
  <ol :id="id" class="list-decimal ml-7.5 my-2.5 md:ml-10 w-auto">
    <ListItem
      v-for="(item, key) in orderedItems"
      :text="item"
      :key="key"
      :contenteditable="props.isEditable"
      @input="
        props.isEditable &&
          emit('updateOrderedItems', {
            text: ($event.target as HTMLElement)?.innerHTML ?? '',
            key,
          })
      "
      @blur="props.isEditable && emit('inlineSave')"
    />
  </ol>
</template>

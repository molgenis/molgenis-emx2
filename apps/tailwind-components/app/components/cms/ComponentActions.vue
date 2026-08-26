<script setup lang="ts">
import { ref } from "vue";
import Button from "../Button.vue";
import BaseIcon from "../BaseIcon.vue";

const props = withDefaults(defineProps<{ name?: string }>(), {});
const emit = defineEmits(["edit", "delete", "move"]);
</script>
<template>
  <div
    class="z-40 absolute left-0 top-0 inline-flex flex-row items-center gap-2 p-1 divide-x group border border-theme rounded-theme [background-image:var(--background-color-bulk-gradient)] shadow-lg"
  >
    <div class="flex flex-row items-center gap-2">
      <div
        class="text-title-contrast whitespace-nowrap cursor-grab flex items-center"
        draggable="true"
        @dragstart="$emit('move', 'grab')"
        @dragend="$emit('move', 'release')"
      >
        {{ name }}
        <BaseIcon name="drag" :width="16" :height="16" />
      </div>
      <div class="border-l border-dashed h-5"></div>
      <Button
        icon-only
        size="tiny"
        icon="arrow-up"
        type="inline"
        label="move up"
        @click="$emit('move', 'up')"
      />
      <Button
        icon-only
        size="tiny"
        icon="arrow-down"
        type="inline"
        label="move down"
        @click="$emit('move', 'down')"
      />
      <Button
        icon-only
        size="tiny"
        icon="edit"
        type="inline"
        label="edit"
        @click="$emit('edit')"
      />
      <Button
        icon-only
        size="tiny"
        icon="trash"
        type="inline"
        label="delete"
        @click="$emit('delete')"
      />
    </div>
  </div>
</template>

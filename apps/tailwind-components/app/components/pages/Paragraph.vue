<script setup lang="ts">
import type { IParagraphs } from "../../../types/cms";
import EditButton from "./EditButton.vue";

const props = withDefaults(
  defineProps<IParagraphs & { isEditable?: boolean }>(),
  {
    paragraphIsCentered: false,
    isEditable: false,
  }
);

const emit = defineEmits<{
  (e: "edit"): void;
}>();
</script>

<template>
  <p class="text-title-contrast text-left">
    <EditButton v-if="isEditable" @click="emit('edit')" class="text-left">
      <span class="sr-only">edit paragraph: </span>
      <span class="group-hover:underline group-focus:underline">
        {{ text }}
      </span>
    </EditButton>
    <span v-else>
      {{ text }}
    </span>
  </p>
</template>

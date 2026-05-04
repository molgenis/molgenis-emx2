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
  <p
    :id="id"
    class="text-title-contrast"
    :class="{
      'text-center': paragraphIsCentered,
      'text-left': !paragraphIsCentered,
    }"
  >
    <EditButton
      v-if="isEditable"
      @click="emit('edit')"
      :class="{
        'text-center': paragraphIsCentered,
        'text-left': !paragraphIsCentered,
      }"
    >
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

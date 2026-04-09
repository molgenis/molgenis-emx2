<script setup lang="ts">
import type { IHeadings } from "../../../types/cms";
import EditButton from "./EditButton.vue";

withDefaults(defineProps<IHeadings & { isEditable?: boolean }>(), {
  level: 2,
  headingIsCentered: false,
  isEditable: false,
});

const emit = defineEmits<{
  (e: "edit", value: boolean): void;
}>();
</script>

<template>
  <component
    :is="`h${level}`"
    :id="id"
    class="text-title"
    :class="{
      'text-heading-6xl': level === 1,
      'text-heading-5xl': level === 2,
      'text-heading-4xl': level === 3,
      'text-heading-3xl': level === 4,
      'text-heading-2xl': level === 5,
      'text-heading-xl': level === 6,
      'text-center': headingIsCentered,
      group: isEditable,
    }"
  >
    <EditButton v-if="isEditable" @click="emit('edit', true)">
      <span class="sr-only">edit heading: </span>
      <span class="group-hover:underline group-focus:underline">
        {{ text }}
      </span>
    </EditButton>
    <span v-else>
      {{ text }}
    </span>
  </component>
</template>

<script setup lang="ts">
import { ref } from "vue";
import type { IHeadings } from "../../../types/cms";
import ComponentActions from "./ComponentActions.vue";

const emit = defineEmits(["edit", "delete", "move"]);
const showMenu = ref<boolean>(false);

withDefaults(defineProps<IHeadings & { isEditable?: boolean }>(), {
  level: 2,
  headingIsCentered: false,
  isEditable: false,
});
</script>

<template>
  <VMenu
    v-if="isEditable"
    v-model:shown="showMenu"
    showGroup="component-menu"
    :triggers="['hover', 'focus']"
    :popperTriggers="['hover', 'focus']"
    :delay="{ show: 100, hide: 200 }"
    :placement="headingIsCentered ? 'bottom-auto' : 'bottom-start'"
    noAutoFocus
  >
    <template #popper>
      <ComponentActions
        name="Heading"
        :id="`${id}-toolbar`"
        :aria-controls="id"
        @edit="$emit('edit')"
        @delete="$emit('delete')"
        @move="$emit('move', $event)"
      />
    </template>
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
        'w-full flex justify-center text-center': headingIsCentered,
        group: isEditable,
        underline: showMenu,
      }"
    >
      {{ text }}
    </component>
  </VMenu>

  <component
    v-else
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
      'w-full flex justify-center text-center': headingIsCentered,
      group: isEditable,
      underline: showMenu,
    }"
  >
    {{ text }}
  </component>
</template>

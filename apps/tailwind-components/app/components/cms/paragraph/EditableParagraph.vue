<script setup lang="ts">
import { ref } from "vue";
import Paragraph from "./Paragraph.vue";
import ComponentActions from "../ComponentActions.vue";
import type { IParagraphs } from "../../../../types/cms";

const props = withDefaults(
  defineProps<IParagraphs & { isEditable?: boolean }>(),
  {
    paragraphIsCentered: false,
    isEditable: false,
  }
);
const emit = defineEmits(["edit", "delete", "move"]);
const showMenu = ref<boolean>(false);
</script>

<template>
  <VMenu
    v-if="isEditable"
    v-model:shown="showMenu"
    showGroup="component-menu"
    :triggers="['hover', 'focus']"
    :popperTriggers="['hover', 'focus']"
    :delay="{ show: 100, hide: 50 }"
    :placement="paragraphIsCentered ? 'bottom-auto' : 'bottom-start'"
    noAutoFocus
  >
    <template #popper>
      <ComponentActions
        name="Paragraph"
        :id="`${id}-toolbar`"
        :aria-controls="id"
        @edit="$emit('edit')"
        @delete="$emit('delete')"
        @move="$emit('move', $event)"
      />
    </template>
    <Paragraph v-bind="props" />
  </VMenu>
  <Paragraph v-else v-bind="props" />
</template>

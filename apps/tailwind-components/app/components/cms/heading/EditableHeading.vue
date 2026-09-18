<script setup lang="ts">
import { ref } from "vue";
import Heading from "./Heading.vue";
import ComponentActions from "../ComponentActions.vue";
import type { IHeadings } from "../../../../types/cms";

const props = withDefaults(
  defineProps<IHeadings & { isEditable?: boolean }>(),
  {
    level: 2,
    headingIsCentered: false,
    headingIsHidden: false,
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
    :placement="headingIsCentered ? 'bottom' : 'bottom-start'"
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
    <Heading
      v-bind="props"
      :class="{
        group: isEditable,
        underline: showMenu,
        'sr-only': headingIsHidden,
      }"
    />
  </VMenu>
  <Heading v-else v-bind="props" />
</template>

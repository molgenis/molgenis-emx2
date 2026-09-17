<script setup lang="ts">
import { ref } from "vue";
import Section from "./Section.vue";
import ComponentActions from "../ComponentActions.vue";
import type { ISections } from "../../../../types/cms";

const props = withDefaults(
  defineProps<ISections & { isEditable?: boolean }>(),
  {
    columns: 1,
    enableFullScreenWidth: false,
    applyShadedBackground: false,
    isEditable: false,
  }
);

const emit = defineEmits(["edit", "delete", "move"]);
const showMenu = ref<boolean>(false);
</script>

<template>
  <div :id="id" class="w-full flex relative">
    <VMenu
      v-if="isEditable"
      v-model:shown="showMenu"
      showGroup="component-menu"
      :triggers="['hover', 'focus']"
      :popperTriggers="['hover', 'focus']"
      :delay="{ show: 100, hide: 200 }"
      placement="auto"
      noAutoFocus
      class="h-auto"
    >
      <template #popper>
        <ComponentActions
          name="Section"
          :id="`${id}-toolbar`"
          :aria-controls="id"
          @edit="$emit('edit')"
          @delete="$emit('delete')"
          @move="$emit('move', $event)"
        />
      </template>
      <div
        class="flex items-center justify-center py-4 pr-8 w-0 relative h-full"
      >
        <div class="border border-dashed w-0 absolute top-3 bottom-3"></div>
        <span
          class="text-button-disabled rotate-90 inline-block pb-6 whitespace-nowrap"
        >
          Section<span v-if="columns > 1"> - {{ columns }} columns</span>
        </span>
      </div>
    </VMenu>
    <Section v-bind="props"><slot></slot></Section>
  </div>
</template>

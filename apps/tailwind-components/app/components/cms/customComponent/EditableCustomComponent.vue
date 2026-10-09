<script setup lang="ts">
import { ref } from "vue";
import CustomComponent from "./CustomComponent.vue";
import ComponentActions from "../ComponentActions.vue";
import type { ICustomComponents } from "../../../../types/cms.ts";

const props = withDefaults(
  defineProps<ICustomComponents & { isEditable?: boolean }>(),
  {
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
    placement="bottom-start"
    noAutoFocus
  >
    <template #popper>
      <ComponentActions
        name="Custom component"
        :id="`${id}-toolbar`"
        :aria-controls="id"
        @edit="$emit('edit')"
        @delete="$emit('delete')"
        @move="$emit('move', $event)"
      />
    </template>
    <div>
      <CustomComponent v-bind="props" />
    </div>
  </VMenu>
  <CustomComponent v-else v-bind="props" />
</template>

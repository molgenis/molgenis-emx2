<script setup lang="ts">
import { ref } from "vue";
import Button from "./Button.vue";
import ComponentActions from "../ComponentActions.vue";
import type { IButtons } from "../../../../types/cms.ts";

const props = withDefaults(defineProps<IButtons & { isEditable?: boolean }>(), {
  buttonIsCentered: false,
  isEditable: false,
});
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
    :placement="buttonIsCentered ? 'bottom' : 'bottom-start'"
    noAutoFocus
  >
    <template #popper>
      <ComponentActions
        name="Button"
        :id="`${id}-toolbar`"
        :aria-controls="id"
        @edit="$emit('edit')"
        @delete="$emit('delete')"
        @move="$emit('move', $event)"
      />
    </template>
    <Button v-bind="props" />
  </VMenu>
  <Button v-else v-bind="props" />
</template>

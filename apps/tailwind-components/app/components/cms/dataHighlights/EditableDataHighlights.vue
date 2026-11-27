<script setup lang="ts">
import { ref } from "vue";

import DataHighlights from "./DataHighlights.vue";
import ComponentActions from "../ComponentActions.vue";
import {
  actionsToolbarShowDelay,
  actionsToolbarHideDelay,
} from "../../../utils/cms/constants.ts";

import type { IDataHighlights } from "../../../../types/cms";

withDefaults(defineProps<IDataHighlights & { isEditable?: boolean }>(), {
  isEditable: false,
});

const emit = defineEmits(["edit", "delete", "move", "updateText"]);
const showMenu = ref<boolean>(false);
</script>

<template>
  <VMenu
    v-if="isEditable"
    v-model:shown="showMenu"
    showGroup="component-menu"
    :triggers="['hover', 'focus']"
    :popperTriggers="['hover', 'focus']"
    :delay="{ show: actionsToolbarShowDelay, hide: actionsToolbarHideDelay }"
    placement="bottom-start"
    noAutoFocus
  >
    <template #popper>
      <ComponentActions
        name="DataHighlight"
        :id="`${id}-toolbar`"
        :aria-controls="id"
        @edit="$emit('edit')"
        @delete="$emit('delete')"
        @move="$emit('move', $event)"
      />
    </template>
    <DataHighlights v-bind="$props" />
  </VMenu>
  <DataHighlights v-else v-bind="$props" />
</template>

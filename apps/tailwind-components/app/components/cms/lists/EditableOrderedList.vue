<script setup lang="ts">
import { ref } from "vue";
import OrderedList from "./OrderedList.vue";
import type { IOrderedLists } from "../../../../types/cms.ts";
import ComponentActions from "../ComponentActions.vue";
import {
  actionsToolbarShowDelay,
  actionsToolbarHideDelay,
} from "../../../utils/cms";

const props = withDefaults(
  defineProps<IOrderedLists & { isEditable?: boolean }>(),
  {
    isEditable: false,
  }
);

const emit = defineEmits([
  "edit",
  "delete",
  "move",
  "updateOrderedItems",
  "inlineSave",
]);
const showMenu = ref<boolean>(false);
</script>

<template>
  <VMenu
    v-if="isEditable"
    v-model:show="showMenu"
    :popperTriggers="['hover', 'focus']"
    :delay="{ show: actionsToolbarShowDelay, hide: actionsToolbarHideDelay }"
    placement="bottom-start"
    noAutoFocus
  >
    <template #popper>
      <ComponentActions
        name="OrderedLists"
        :id="`${id}-toolbar`"
        :aria-controls="id"
        @edit="$emit('edit')"
        @delete="$emit('delete')"
        @move="$emit('move', $event)"
      />
    </template>
    <OrderedList
      :id="id"
      :orderedItems="orderedItems"
      :isEditable="isEditable"
      @updateOrderedItems="
        $emit('updateOrderedItems', { key: $event.key, text: $event.text })
      "
      @inlineSave="$emit('inlineSave')"
    />
  </VMenu>
  <OrderedList v-else :id="id" :orderedItems="orderedItems" />
</template>

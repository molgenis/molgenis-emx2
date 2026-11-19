<script setup lang="ts">
import { ref } from "vue";
import UnorderedList from "./UnorderedList.vue";
import type { IUnorderedLists } from "../../../../types/cms.ts";
import ComponentActions from "../ComponentActions.vue";
import {
  actionsToolbarShowDelay,
  actionsToolbarHideDelay,
} from "../../../utils/cms/constants.ts";

const props = withDefaults(
  defineProps<IUnorderedLists & { isEditable?: boolean }>(),
  {
    isEditable: false,
  }
);

const emit = defineEmits([
  "edit",
  "delete",
  "move",
  "updateUnorderedItems",
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
        name="UnorderedLists"
        :id="`${id}-toolbar`"
        :aria-controls="id"
        @edit="$emit('edit')"
        @delete="$emit('delete')"
        @move="$emit('move', $event)"
      />
    </template>
    <UnorderedList
      :id="id"
      :unorderedItems="unorderedItems"
      :isEditable="isEditable"
      @updateUnorderedItems="
        $emit('updateUnorderedItems', { key: $event.key, text: $event.text })
      "
      @inlineSave="$emit('inlineSave')"
    />
  </VMenu>
  <UnorderedList v-else :id="id" :unorderedItems="unorderedItems" />
</template>

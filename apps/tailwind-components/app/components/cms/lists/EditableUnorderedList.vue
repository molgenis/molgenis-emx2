<script setup lang="ts">
import { ref } from "vue";
import UnorderedList from "./UnorderedList.vue";
import type { IUnorderedLists } from "../../../../types/cms.ts";
import ComponentActions from "../ComponentActions.vue";

const props = withDefaults(
  defineProps<IUnorderedLists & { isEditable?: boolean }>(),
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
    v-model:show="showMenu"
    :popperTriggers="['hover', 'focus']"
    :delay="{ show: 100, hide: 200 }"
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
    <UnorderedList :id="id" :unorderedItems="unorderedItems" />
  </VMenu>
  <UnorderedList v-else :id="id" :unorderedItems="unorderedItems" />
</template>

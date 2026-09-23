<script setup lang="ts">
import { ref } from "vue";
import Header from "./Header.vue";
import ComponentActions from "./../ComponentActions.vue";
import type { IHeaders, IFile } from "../../../../types/cms";

const props = withDefaults(
  defineProps<IHeaders & { image?: IFile; isEditable?: boolean }>(),
  {
    enableFullScreenWidth: false,
    isEditable: false,
  }
);
const emit = defineEmits(["edit", "delete", "move"]);
const showMenu = ref<boolean>(true);
</script>

<template>
  <div
    class="w-full relative"
    @mouseenter="showMenu = true"
    @mouseleave="showMenu = false"
  >
    <ComponentActions
      v-if="isEditable && showMenu"
      name="Header"
      :id="`${id}-toolbar`"
      :aria-controls="id"
      @edit="$emit('edit')"
      @delete="$emit('delete')"
      @move="$emit('move', $event)"
      class="right-2 top-2 !left-auto"
    />
    <Header v-bind="props" />
  </div>
</template>

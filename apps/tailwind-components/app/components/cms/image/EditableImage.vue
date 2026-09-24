<script setup lang="ts">
import { ref } from "vue";
import Image from "./Image.vue";
import type { IImages } from "../../../../types/cms";

import ComponentActions from "../ComponentActions.vue";
import BaseIcon from "../../BaseIcon.vue";

const props = withDefaults(defineProps<IImages & { isEditable?: boolean }>(), {
  isEditable: false,
  imageIsCentered: false,
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
    :placement="imageIsCentered ? 'bottom' : 'bottom-start'"
    noAutoFocus
  >
    <template #popper>
      <ComponentActions
        name="Image"
        :id="`${id}-toolbar`"
        :aria-controls="id"
        @edit="$emit('edit')"
        @delete="$emit('delete')"
        @move="$emit('move', $event)"
      />
    </template>
    <div>
      <div
        v-if="!props.image?.url"
        class="w-full flex items-center justify-center text-center gap-2 text-title-contrast py-5 border border-button-tertiary rounded-base mb-2.5 hover:border-button-tertiary-hover"
      >
        <BaseIcon name="Image" :width="21" />
        <span>Click the edit button to upload an image</span>
      </div>
      <Image v-else v-bind="props" />
    </div>
  </VMenu>
  <Image v-else v-bind="props" />
</template>

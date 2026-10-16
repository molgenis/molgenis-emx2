<script setup lang="ts">
import { ref } from "vue";
import Image from "./Image.vue";
import type { IImages } from "../../../../types/cms";
import ComponentActions from "../ComponentActions.vue";
import UploadNeeded from "../UploadNeeded.vue";

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
      <UploadNeeded v-if="!props.image?.url"
        >Click the edit button to upload an image</UploadNeeded
      >
      <Image v-else v-bind="props" />
    </div>
  </VMenu>
  <Image v-else v-bind="props" />
</template>

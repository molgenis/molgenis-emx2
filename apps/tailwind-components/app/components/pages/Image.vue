<script setup lang="ts">
import { ref } from "vue";
import type { IImages } from "../../../types/cms";

import ComponentActions from "./ComponentActions.vue";
import BaseIcon from "../BaseIcon.vue";
import BaseIcon from "../BaseIcon.vue";

const props = withDefaults(defineProps<IImages & { isEditable?: boolean }>(), {
  isEditable: false,
  imageIsCentered: false,
});

const emit = defineEmits(["edit", "delete"]);

const showMenu = ref<boolean>(true);

const src = ref<string>();
if (props.image?.url) {
  src.value = props.image.url.replace("Components", "Images");
}

let style: string = "";
if (src.value && props.width) {
  style = style + `width: ${props.width};`;
}

if (src.value && props.height) {
  style = style + `height: ${props.height};`;
}
</script>

<template>
  <div
    class="w-full"
    @mouseenter="showMenu = true"
    @mouseleave="showMenu = false"
  >
    <div
      class="relative"
      :class="{
        'm-auto': imageIsCentered,
      }"
      :style="style"
    >
      <div
        v-if="!src"
        class="w-full flex items-center justify-center text-center gap-2 text-title-contrast py-5 border border-button-tertiary rounded-base mb-2.5 hover:border-button-tertiary-hover"
      >
        <BaseIcon name="Image" :width="21" />
        <span>Click the edit button to upload an image</span>
      </div>
      <img v-if="src" :id="id" :src="src" :alt="alt" />
      <ComponentActions
        v-if="isEditable && showMenu"
        name="Image"
        :id="`${id}-toolbar`"
        :aria-controls="id"
        @edit="$emit('edit')"
        @delete="$emit('delete')"
        class="right-2 top-2 !left-auto"
      />
    </div>
  </div>
</template>

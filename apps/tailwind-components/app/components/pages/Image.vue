<script setup lang="ts">
import { ref } from "vue";
import type { IImages } from "../../../types/cms";

import EditButton from "./EditButton.vue";

const props = withDefaults(defineProps<IImages & { isEditable?: boolean }>(), {
  isEditable: false,
  imageIsCentered: false,
});

const emit = defineEmits<{
  (e: "edit"): void;
}>();

const src = ref<string>();
if (props.image?.url) {
  src.value = props.image.url.replace("Components", "Images");
}

let style = "";
if (src.value && props.width) {
  style = style + `width: ${props.width};`;
}

if (src.value && props.height) {
  style = style + `height: ${props.height};`;
}
</script>

<template>
  <div>
    <EditButton
      v-if="isEditable"
      class="border-2 border-transparent bg-button-secondary hover:bg-button-secondary-hover focus:bg-button-secondary-hover"
      :class="{
        'm-auto flex justify-center items-center': imageIsCentered,
        'w-full': !src,
      }"
      @click="emit('edit')"
      :fix-icon-position="true"
    >
      <span class="sr-only">Edit image: </span>
      <img v-if="src" :id="id" :src="src" :alt="alt" :style="style" />
      <div
        v-else
        :id="`${id}-edit-add-image-message`"
        class="w-full bg-neutral py-5"
      >
        <span class="text-neutral">Click edit to add your own image</span>
      </div>
    </EditButton>
    <img
      v-else-if="src"
      :id="id"
      :src="src"
      :class="{
        'm-auto': imageIsCentered,
      }"
      :alt="alt"
      :style="style"
    />
  </div>
</template>

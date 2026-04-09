<script setup lang="ts">
import { ref } from "vue";
import type { IImages } from "../../../types/cms";

import Button from "../Button.vue";
import EditButton from "./EditButton.vue";

const props = withDefaults(defineProps<IImages & { isEditable?: boolean }>(), {
  isEditable: false,
  imageIsCentered: false,
});

const emit = defineEmits<{
  (e: "edit", value: boolean): void;
}>();

const src = ref<string>();
if (props.image?.url) {
  src.value = props.image.url.replace("Components", "Images");
}

let style = "";
if (props.width) {
  style = style + `width: ${props.width};`;
}

if (props.height) {
  style = style + `height: ${props.height};`;
}
</script>

<template>
  <div>
    <EditButton
      v-if="isEditable"
      class="border-2 border-transparent hover:bg-button-primary-hover focus:bg-button-primary-hover"
      :class="{ 'm-auto flex justify-center items-center': imageIsCentered }"
      @click="emit('edit', true)"
      :fix-icon-position="true"
    >
      <span class="sr-only">Edit image: </span>
      <img :id="id" :src="src" :alt="alt" :style="style" />
    </EditButton>
    <img
      v-else
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

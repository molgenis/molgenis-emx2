<script setup lang="ts">
import { ref } from "vue";
import type { IImages } from "../../../types/cms";

const props = withDefaults(defineProps<IImages>(), {
  imageIsCentered: false,
});

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
  <img
    :src="src"
    :class="{
      'm-auto': imageIsCentered,
    }"
    :alt="alt"
    :style="style"
  />
</template>

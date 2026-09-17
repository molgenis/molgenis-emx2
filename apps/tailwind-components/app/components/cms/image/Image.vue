<script setup lang="ts">
import { ref } from "vue";
import type { IImages } from "../../../../types/cms";

const props = withDefaults(defineProps<IImages & { isEditable?: boolean }>(), {
  isEditable: false,
  imageIsCentered: false,
});

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
  <img
    :id="id"
    :src="src"
    :alt="alt"
    :style="style"
    :class="{ 'm-auto': imageIsCentered }"
  />
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { IImages } from "../../../../types/cms";

const props = withDefaults(defineProps<IImages & { isEditable?: boolean }>(), {
  isEditable: false,
  imageIsCentered: false,
});

// required: remove Components table ID from page and point to Images table
const src = computed<string | undefined>(() => {
  if (!props.image?.url) {
    return undefined;
  }
  return props.image.url.replace("Components", "Images");
});

const style = computed<string | undefined>(() => {
  const css = [];
  if (src.value && props.width) {
    css.push(`width: ${props.width};`);
  }

  if (src.value && props.height) {
    css.push(`height: ${props.height};`);
  }
  if (css) {
    return css.join(" ");
  } else {
    return undefined;
  }
});
</script>

<template>
  <img
    :id="id"
    :src="src"
    :alt="alt"
    class="my-5"
    :class="{ 'm-auto': imageIsCentered }"
    :style="style"
  />
</template>

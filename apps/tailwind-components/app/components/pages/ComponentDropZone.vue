<script setup lang="ts">
import { useMouse } from "@vueuse/core";
import { useElementBounding } from "@vueuse/core";
import { computed, useTemplateRef } from "vue";
import { useWindowScroll } from "@vueuse/core";

const scroll = useWindowScroll();
const dropzone = useTemplateRef("dropzone");

const mouse = useMouse();
const { y } = useElementBounding(dropzone);

const props = withDefaults(
  defineProps<{
    componentType: string;
    draggingInfo: {
      dragging: boolean;
      componentName: string;
      componentType: string;
    };
  }>(),
  {}
);

const maxDistance = 200;
const maxDropzoneSize = 50;
const distance = computed(() => {
  if (!props.draggingInfo.dragging) return 0;
  if (props.draggingInfo.componentType !== props.componentType) return 0;
  return Math.max(
    maxDistance -
      Math.max(
        Math.abs(
          mouse.y.value - scroll.y.value - (y.value + maxDropzoneSize / 2)
        ),
        50
      ),
    0
  );
});
</script>

<template>
  <div
    ref="dropzone"
    class="overflow-hidden flex items-center justify-center bg-dashboard-dropzone border border-button-primary border-dashed rounded-lg p-1 text-center"
    :style="{ height: distance * 0.25 + 'px', opacity: distance / maxDistance }"
  >
    <p class="text-title-contrast">Add new component here</p>
  </div>
</template>

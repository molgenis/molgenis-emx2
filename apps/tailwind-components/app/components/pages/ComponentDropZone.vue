<script setup lang="ts">
import { useMouse } from "@vueuse/core";
import { useElementBounding } from "@vueuse/core";
import { computed, ref, useTemplateRef } from "vue";
import { useWindowScroll } from "@vueuse/core";
import { useRafFn } from "@vueuse/core";

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

const maxDistance = 50;
const distance = ref<number>(Infinity);
const canPlace = ref<boolean>(false);
useRafFn(() => {
  canPlace.value =
    props.draggingInfo.dragging &&
    props.draggingInfo.componentType === props.componentType;
  distance.value = Math.max(
    maxDistance -
      Math.max(
        Math.abs(mouse.y.value - scroll.y.value - (y.value + maxDistance / 2)),
        10
      ),
    0
  );
});
</script>

<template>
  <div v-if="canPlace" class="relative">
    <div class="absolute left-0 right-0 top-0 bottom-0 z-40">
      <div class="border-t border-button-primary border-dashed"></div>
      <div class="-translate-y-1/2">
        <div
          ref="dropzone"
          :class="{ border: distance > 0 }"
          class="border-button-primary border-dashed rounded-lg overflow-hidden flex items-center justify-center bg-dashboard-dropzone text-center"
          :style="{ height: distance + 'px' }"
        >
          <p class="text-title-contrast">
            Add new {{ props.draggingInfo.componentName }} here
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
